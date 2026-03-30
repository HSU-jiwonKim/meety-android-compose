package com.bugzero.meety.ui.feed

import com.bugzero.meety.ui.team.Team
import com.google.firebase.auth.FirebaseAuth
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
 */
class FeedRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    // =====================
    // 팀 목록 조회
    // =====================

    /**
     * teams 컬렉션에서 활성(active) 팀 목록을 1회 가져온다.
     *
     * - status == "active" 팀만 조회
     * - 내가 속한 팀 제외
     * - 이미 좋아요/패스한 팀 제외 (userPreferences 기반)
     * - 최신순(createdAt 내림차순) 정렬
     */
    suspend fun fetchActiveTeams(): Result<List<Team>> {
        return try {
            val currentUserId = auth.currentUser?.uid

            val snapshot = db.collection("teams")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            // 이미 액션을 취한 팀 ID 목록 (좋아요 + 패스)
            val actionedIds = currentUserId?.let { fetchActionedTeamIds(it) } ?: emptySet()

            val teams = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)
            }.filter { team ->
                val notMyTeam = currentUserId == null || !team.memberIds.contains(currentUserId)
                val notActioned = !actionedIds.contains(team.teamId)
                notMyTeam && notActioned
            }

            Result.success(teams)
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
     * (추후 실시간 피드 업데이트가 필요할 때 사용)
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
     * 저장 위치: "likes/{likeId}"
     * → team 패키지 담당자가 이 컬렉션을 읽어 매칭탭 "보낸 관심"을 구현한다.
     *
     * 부수 효과:
     * - userPreferences에 해당 팀의 태그/MBTI 점수를 +로 누적
     * - likedTeamIds에 팀 ID 추가 (다음 피드 로딩 시 해당 팀 제외)
     */
    suspend fun saveLike(team: Team): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            // 내 팀 ID 조회 (없으면 빈 문자열)
            val myTeamId = fetchMyTeamId(userId)

            // 1) likes 컬렉션에 저장
            val likeRef = db.collection("likes").document()
            val like = Like(
                likeId = likeRef.id,
                fromUserId = userId,
                fromTeamId = myTeamId,
                toTeamId = team.teamId,
                toTeamName = team.teamName,
                toTeamTags = team.tags,
                toTeamMbtiTags = team.mbtiTags,
                createdAt = System.currentTimeMillis()
            )
            likeRef.set(like).await()

            // 2) userPreferences 업데이트 (선호도 점수 + / likedTeamIds 추가)
            updatePreferenceScores(
                userId = userId,
                team = team,
                isLike = true
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 패스를 Firebase에 저장한다.
     *
     * likes 컬렉션에는 저장하지 않고,
     * userPreferences에만 비선호 신호(-점수, passedTeamIds)를 기록한다.
     */
    suspend fun savePass(team: Team): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            updatePreferenceScores(
                userId = userId,
                team = team,
                isLike = false
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =====================
    // 사용자 선호도 (AI 매칭 데이터)
    // =====================

    /**
     * Firebase에서 사용자 선호도를 불러온다.
     *
     * 앱을 재시작해도 이전에 학습된 취향이 유지된다.
     */
    suspend fun loadUserPreference(): Result<UserPreference> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            val doc = db.collection("userPreferences").document(userId).get().await()

            if (!doc.exists()) {
                // 처음 사용하는 경우 빈 선호도 반환
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
     * 좋아요/패스한 팀의 태그·MBTI 점수를 userPreferences에 누적한다.
     *
     * 좋아요  → 태그/MBTI 점수 +, likedTeamIds 추가
     * 패스    → 태그/MBTI 점수 -, passedTeamIds 추가
     */
    private suspend fun updatePreferenceScores(
        userId: String,
        team: Team,
        isLike: Boolean
    ) {
        val tagWeight = if (isLike) TAG_LIKE_WEIGHT else TAG_PASS_WEIGHT
        val mbtiWeight = if (isLike) MBTI_LIKE_WEIGHT else MBTI_PASS_WEIGHT

        // 기존 점수 맵을 가져와서 증감 후 덮어쓰기
        val doc = db.collection("userPreferences").document(userId).get().await()

        @Suppress("UNCHECKED_CAST")
        val currentTagScores = (doc.get("tagScores") as? Map<String, Long>)
            ?.mapValues { it.value.toInt() }?.toMutableMap() ?: mutableMapOf()

        @Suppress("UNCHECKED_CAST")
        val currentMbtiScores = (doc.get("mbtiScores") as? Map<String, Long>)
            ?.mapValues { it.value.toInt() }?.toMutableMap() ?: mutableMapOf()

        team.tags.forEach { tag ->
            currentTagScores[tag] = (currentTagScores[tag] ?: 0) + tagWeight
        }
        team.mbtiTags.forEach { mbti ->
            currentMbtiScores[mbti] = (currentMbtiScores[mbti] ?: 0) + mbtiWeight
        }

        val teamIdField = if (isLike) "likedTeamIds" else "passedTeamIds"

        db.collection("userPreferences").document(userId).set(
            mapOf(
                "userId" to userId,
                "tagScores" to currentTagScores,
                "mbtiScores" to currentMbtiScores,
                teamIdField to FieldValue.arrayUnion(team.teamId),
                "updatedAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()  // 기존 데이터 덮어쓰지 않고 병합
        ).await()
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
        private const val TAG_LIKE_WEIGHT  = 1   // 좋아요한 팀의 태그 점수
        private const val TAG_PASS_WEIGHT  = -1  // 패스한 팀의 태그 점수
        private const val MBTI_LIKE_WEIGHT = 2   // 좋아요한 팀의 MBTI 점수 (가중치 높음)
        private const val MBTI_PASS_WEIGHT = -2  // 패스한 팀의 MBTI 점수
    }
}
