package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.feed.Like
import com.bugzero.meety.ui.feed.MemberProfile
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
 *  - fetchMyTeamIds: teamId(String) → teamIds(List<String>) 변경
 */
class FeedRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // 페이지네이션 커서 — RECOMMEND 탭 (필터링된 목록)
    private var lastDocument: DocumentSnapshot? = null

    // 페이지네이션 커서 — LIST 탭 (전체 목록)
    private var lastAllDocument: DocumentSnapshot? = null

    // =====================
    // 팀 목록 조회
    // =====================

    suspend fun fetchActiveTeams(loadMore: Boolean = false): Result<Pair<List<Team>, Boolean>> {
        return try {
            val currentUserId = auth.currentUser?.uid

            if (!loadMore) lastDocument = null

            var query = db.collection("teams")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE.toLong())

            if (loadMore && lastDocument != null) {
                query = query.startAfter(lastDocument!!)
            }

            val snapshot = query.get().await()

            if (snapshot.documents.isNotEmpty()) {
                lastDocument = snapshot.documents.last()
            }

            val actionedIds = currentUserId?.let { fetchActionedTeamIds(it) } ?: emptySet()

            // 변경: 내가 속한 팀들(teamIds) 전부 필터링
            val myTeamIds = currentUserId?.let { fetchMyTeamIds(it) } ?: emptyList()

            val teams = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)
            }.filter { team ->
                val notMyTeam = currentUserId == null || !team.memberIds.contains(currentUserId)
                val notActioned = !actionedIds.contains(team.teamId)
                val notMyOwnTeam = !myTeamIds.contains(team.teamId)
                notMyTeam && notActioned && notMyOwnTeam
            }

            val hasMore = snapshot.documents.size >= PAGE_SIZE

            Result.success(Pair(teams, hasMore))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
     * 전체보기 탭 전용: 좋아요·패스·내 팀 여부와 무관하게 활성 팀 전체를 페이지네이션으로 가져온다.
     *
     * @param loadMore true이면 이전 페이지 이후부터 조회
     * @return Pair<팀 목록, 서버에 더 있는지 여부>
     */
    suspend fun fetchAllActiveTeams(loadMore: Boolean = false): Result<Pair<List<Team>, Boolean>> {
        return try {
            if (!loadMore) lastAllDocument = null

            var query = db.collection("teams")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE.toLong())

            if (loadMore && lastAllDocument != null) {
                query = query.startAfter(lastAllDocument!!)
            }

            val snapshot = query.get().await()

            if (snapshot.documents.isNotEmpty()) {
                lastAllDocument = snapshot.documents.last()
            }

            // 필터링 없이 전체 반환
            val teams = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)
            }

            val hasMore = snapshot.documents.size >= PAGE_SIZE

            Result.success(Pair(teams, hasMore))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 현재 로그인된 유저가 소속된 팀 ID를 반환한다.
     * 소속 팀이 없거나 오류 시 빈 문자열을 반환한다.
     */
    suspend fun fetchMyTeamId(): String {
        val userId = auth.currentUser?.uid ?: return ""
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.getString("teamId") ?: ""
        } catch (e: Exception) {
            ""
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

    suspend fun saveLike(team: Team, likeId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            val myTeamId = fetchMyTeamIdForUser(userId)

            val like = Like(
                likeId = likeId,
                fromUserId = userId,
                fromTeamId = fromTeamId,
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

    suspend fun cancelLike(likeId: String): Result<Unit> {
        return try {
            db.collection("likes").document(likeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    private suspend fun updatePreferenceScores(
        userId: String,
        team: Team,
        isLike: Boolean
    ) {
        val tagWeight  = if (isLike) TAG_LIKE_WEIGHT  else TAG_PASS_WEIGHT
        val mbtiWeight = if (isLike) MBTI_LIKE_WEIGHT else MBTI_PASS_WEIGHT
        val teamIdField = if (isLike) "likedTeamIds" else "passedTeamIds"

        db.collection("userPreferences").document(userId)
            .set(mapOf("userId" to userId), SetOptions.mergeFields("userId"))
            .await()

        val updates = mutableMapOf<String, Any>()
        team.tags.forEach     { tag  -> updates["tagScores.$tag"]   = FieldValue.increment(tagWeight.toLong()) }
        team.mbtiTags.forEach { mbti -> updates["mbtiScores.$mbti"] = FieldValue.increment(mbtiWeight.toLong()) }
        updates[teamIdField] = FieldValue.arrayUnion(team.teamId)
        updates["updatedAt"] = System.currentTimeMillis()

        db.collection("userPreferences").document(userId).update(updates).await()
    }

    // =====================
    // 팀원 프로필
    // =====================

    /**
     * memberIds 목록에 해당하는 유저 프로필을 Firestore에서 일괄 조회한다.
     * 개별 실패는 무시하고 성공한 것만 반환한다.
     */
    suspend fun fetchMemberProfiles(memberIds: List<String>): Result<List<MemberProfile>> {
        return try {
            if (memberIds.isEmpty()) return Result.success(emptyList())
            val profiles = memberIds.mapNotNull { userId ->
                try {
                    val doc = db.collection("users").document(userId).get().await()
                    if (!doc.exists()) return@mapNotNull null
                    MemberProfile(
                        userId        = userId,
                        name          = doc.getString("name") ?: "",
                        age           = doc.getLong("age")?.toInt() ?: 0,
                        department    = doc.getString("department") ?: "",
                        mbti          = doc.getString("mbti") ?: "",
                        bio           = doc.getString("bio") ?: "",
                        height        = doc.getLong("height")?.toInt() ?: 0,
                        location      = doc.getString("location") ?: "",
                        profileImages = (doc.get("profileImages") as? List<*>)
                                            ?.filterIsInstance<String>() ?: emptyList(),
                        interests     = (doc.get("interests") as? List<*>)
                                            ?.filterIsInstance<String>() ?: emptyList(),
                        foodLikes     = (doc.get("foodLikes") as? List<*>)
                                            ?.filterIsInstance<String>() ?: emptyList()
                    )
                } catch (e: Exception) { null }
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * toTeamId로 likes 컬렉션을 조회해 해당 좋아요 문서를 삭제한다.
     * (상세화면에서 좋아요 취소 시 사용 — likeId를 모를 때)
     */
    /**
     * toTeamId로 likes 컬렉션을 조회해 해당 좋아요 문서를 삭제한다.
     *
     * Composite Index 이슈를 피하기 위해 fromUserId 단일 쿼리 후
     * 클라이언트에서 toTeamId를 필터링한다.
     * 한 유저가 보낸 좋아요 수는 제한적이므로 성능 문제 없음.
     */
    suspend fun cancelLikeByTeamId(toTeamId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            val snapshot = db.collection("likes")
                .whereEqualTo("fromUserId", userId)
                .get().await()

            snapshot.documents
                .filter { it.getString("toTeamId") == toTeamId }
                .forEach { it.reference.delete().await() }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 패스했던 팀에 좋아요를 보낸다.
     *
     * 1) 패스 기록 역산 (reversePreferenceScores)
     * 2) 신규 좋아요 저장 (saveLike)
     */
    suspend fun convertPassToLike(team: Team, likeId: String): Result<Unit> {
        val reverseResult = reversePreferenceScores(team, wasLike = false)
        if (reverseResult.isFailure) return reverseResult
        return saveLike(team, likeId)
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

    private suspend fun fetchMyTeamIdForUser(userId: String): String {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            @Suppress("UNCHECKED_CAST")
            (doc.get("teamIds") as? List<String>) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
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

        const val PAGE_SIZE = 20
    }
}