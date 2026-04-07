package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.feed.Like
import com.bugzero.meety.ui.feed.UserPreference
import com.bugzero.meety.ui.team.Team
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * 피드 화면 전용 Firebase Repository
 *
 * 담당 컬렉션:
 *   - teams          : 피드에 표시할 팀 목록
 *   - likes          : 좋아요 기록 (team 패키지 담당자가 매칭탭에서 읽을 컬렉션)
 *   - userPreferences: 사용자 태그/MBTI 선호도 (AI 매칭 데이터)
 *   - users          : 프로필 조회/수정
 *
 * [주요 변경 내역]
 *  - updatePreferenceScores: read-modify-write → FieldValue.increment() 원자적 업데이트
 *    (동시 스와이프 시 race condition 원천 차단, 네트워크 왕복 1회 절감)
 *  - saveLike: 클라이언트에서 미리 생성한 likeId를 받아 저장 → undo 즉시 가능
 *  - cancelLike: 좋아요 문서 삭제 (undo 지원)
 *  - reversePreferenceScores: 선호도 역산 (undo 지원)
 *  - fetchActiveTeams: 페이지네이션 지원 (PAGE_SIZE 단위 로딩)
 */
class FeedRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // 페이지네이션 커서
    private var lastDocument: DocumentSnapshot? = null

    // =====================
    // 팀 목록 조회
    // =====================

    /**
     * teams 컬렉션에서 활성 팀 목록을 PAGE_SIZE 단위로 가져온다.
     *
     * @param loadMore true이면 이전 페이지 이후부터 조회 (페이지네이션)
     * @return Pair<팀 목록, 서버에 더 있는지 여부>
     */
    suspend fun fetchActiveTeams(loadMore: Boolean = false): Result<Pair<List<Team>, Boolean>> {
        return try {
            val currentUserId = auth.currentUser?.uid

            // 새 조회 시 커서 초기화
            if (!loadMore) lastDocument = null

            var query = db.collection("teams")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE.toLong())

            // 페이지네이션: 이전 페이지 마지막 문서 이후부터
            if (loadMore && lastDocument != null) {
                query = query.startAfter(lastDocument!!)
            }

            val snapshot = query.get().await()

            // 다음 페이지 커서 갱신
            if (snapshot.documents.isNotEmpty()) {
                lastDocument = snapshot.documents.last()
            }

            // 이미 액션을 취한 팀 ID 목록 (좋아요 + 패스)
            val actionedIds = currentUserId?.let { fetchActionedTeamIds(it) } ?: emptySet()

            val teams = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)
            }.filter { team ->
                val notMyTeam = currentUserId == null || !team.memberIds.contains(currentUserId)
                val notActioned = !actionedIds.contains(team.teamId)
                notMyTeam && notActioned
            }

            // 서버에서 PAGE_SIZE만큼 왔으면 다음 페이지 존재 가능성이 높음
            val hasMore = snapshot.documents.size >= PAGE_SIZE

            Result.success(Pair(teams, hasMore))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 현재 유저가 이미 좋아요/패스한 팀 ID 집합을 반환한다. */
    private suspend fun fetchActionedTeamIds(userId: String): Set<String> {
        return try {
            val doc = db.collection("userPreferences").document(userId).get().await()
            val liked = (doc.get("likedTeamIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val passed = (doc.get("passedTeamIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            (liked + passed).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * teams 컬렉션의 실시간 변경사항을 Flow로 받는다.
     */
    fun observeActiveTeams(): Flow<Result<List<Team>>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid

        val listener = db.collection("teams")
            .whereEqualTo("status", "active")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val teams = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Team::class.java)
                }?.filter { team ->
                    currentUserId == null || !team.memberIds.contains(currentUserId)
                } ?: emptyList()

                trySend(Result.success(teams))
            }

        awaitClose { listener.remove() }
    }

    // =====================
    // 좋아요 / 패스
    // =====================

    /**
     * 좋아요를 Firebase에 저장한다.
     *
     * [변경] likeId를 클라이언트(ViewModel)에서 미리 생성해 전달받는다.
     * → undo 시 Firebase 응답을 기다리지 않고 즉시 cancelLike 호출 가능.
     *
     * @param team   좋아요를 누른 팀
     * @param likeId ViewModel에서 UUID로 미리 생성한 Firestore 문서 ID
     */
    suspend fun saveLike(team: Team, likeId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            val myTeamId = fetchMyTeamId(userId)

            val like = Like(
                likeId = likeId,
                fromUserId = userId,
                fromTeamId = myTeamId,
                toTeamId = team.teamId,
                toTeamName = team.teamName,
                toTeamTags = team.tags,
                toTeamMbtiTags = team.mbtiTags,
                createdAt = System.currentTimeMillis()
            )
            db.collection("likes").document(likeId).set(like).await()

            updatePreferenceScores(userId = userId, team = team, isLike = true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 패스를 Firebase에 저장한다.
     */
    suspend fun savePass(team: Team): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            updatePreferenceScores(userId = userId, team = team, isLike = false)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 좋아요를 취소한다 (undo 지원).
     */
    suspend fun cancelLike(likeId: String): Result<Unit> {
        return try {
            db.collection("likes").document(likeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 스와이프 선호도를 역산한다 (undo 지원).
     *
     * FieldValue.increment()를 사용하므로 원자적으로 처리된다.
     */
    suspend fun reversePreferenceScores(team: Team, wasLike: Boolean): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            val tagWeight  = if (wasLike) TAG_PASS_WEIGHT  else TAG_LIKE_WEIGHT
            val mbtiWeight = if (wasLike) MBTI_PASS_WEIGHT else MBTI_LIKE_WEIGHT
            val teamIdField = if (wasLike) "likedTeamIds" else "passedTeamIds"

            val updates = mutableMapOf<String, Any>()
            team.tags.forEach     { tag  -> updates["tagScores.$tag"]   = FieldValue.increment(tagWeight.toLong()) }
            team.mbtiTags.forEach { mbti -> updates["mbtiScores.$mbti"] = FieldValue.increment(mbtiWeight.toLong()) }
            updates[teamIdField] = FieldValue.arrayRemove(team.teamId)
            updates["updatedAt"] = System.currentTimeMillis()

            db.collection("userPreferences").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =====================
    // 사용자 선호도 (AI 매칭 데이터)
    // =====================

    suspend fun loadUserPreference(): Result<UserPreference> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            val doc = db.collection("userPreferences").document(userId).get().await()

            if (!doc.exists()) {
                return Result.success(UserPreference(userId = userId))
            }

            @Suppress("UNCHECKED_CAST")
            val tagScores = (doc.get("tagScores") as? Map<String, Long>)
                ?.mapValues { it.value.toInt() } ?: emptyMap()

            @Suppress("UNCHECKED_CAST")
            val mbtiScores = (doc.get("mbtiScores") as? Map<String, Long>)
                ?.mapValues { it.value.toInt() } ?: emptyMap()

            val likedTeamIds = (doc.get("likedTeamIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()

            val passedTeamIds = (doc.get("passedTeamIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()

            Result.success(
                UserPreference(
                    userId = userId,
                    tagScores = tagScores,
                    mbtiScores = mbtiScores,
                    likedTeamIds = likedTeamIds,
                    passedTeamIds = passedTeamIds,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 좋아요/패스한 팀의 태그·MBTI 점수를 userPreferences에 원자적으로 누적한다.
     *
     * [개선] 기존 read-modify-write → FieldValue.increment() 원자적 업데이트
     *   - race condition 완전 차단
     *   - 네트워크 왕복 1회 감소
     */
    private suspend fun updatePreferenceScores(
        userId: String,
        team: Team,
        isLike: Boolean
    ) {
        val tagWeight  = if (isLike) TAG_LIKE_WEIGHT  else TAG_PASS_WEIGHT
        val mbtiWeight = if (isLike) MBTI_LIKE_WEIGHT else MBTI_PASS_WEIGHT
        val teamIdField = if (isLike) "likedTeamIds" else "passedTeamIds"

        // 1) 문서 존재 보장 (기존 데이터는 건드리지 않음)
        db.collection("userPreferences").document(userId)
            .set(mapOf("userId" to userId), SetOptions.mergeFields("userId"))
            .await()

        // 2) 원자적 증감 (dot notation으로 중첩 맵 필드 직접 업데이트)
        val updates = mutableMapOf<String, Any>()
        team.tags.forEach     { tag  -> updates["tagScores.$tag"]   = FieldValue.increment(tagWeight.toLong()) }
        team.mbtiTags.forEach { mbti -> updates["mbtiScores.$mbti"] = FieldValue.increment(mbtiWeight.toLong()) }
        updates[teamIdField] = FieldValue.arrayUnion(team.teamId)
        updates["updatedAt"] = System.currentTimeMillis()

        db.collection("userPreferences").document(userId).update(updates).await()
    }

    // =====================
    // 프로필
    // =====================

    suspend fun fetchMyProfile(): Result<Map<String, Any>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))
            val doc = db.collection("users").document(userId).get().await()
            Result.success(doc.data ?: emptyMap())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMyProfile(fields: Map<String, Any>): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))
            db.collection("users").document(userId).update(fields).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchMyTeamId(userId: String): String {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.getString("teamId") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // =====================
    // 상수
    // =====================

    companion object {
        private const val TAG_LIKE_WEIGHT  = 1
        private const val TAG_PASS_WEIGHT  = -1
        private const val MBTI_LIKE_WEIGHT = 2
        private const val MBTI_PASS_WEIGHT = -2

        /** 한 번에 불러올 팀 수 */
        const val PAGE_SIZE = 20
    }
}
