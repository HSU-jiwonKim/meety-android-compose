package com.bugzero.meety.data.repository

import com.bugzero.meety.ui.feed.FeedConstants
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
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
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
 *  - fetchAllActiveTeams: LIST 탭 전용 (필터링 없이 전체 조회)
 *  - fetchMemberProfiles: 팀원 프로필 일괄 조회
 *  - cancelLikeByTeamId: likeId 없이 toTeamId로 좋아요 취소
 *  - convertPassToLike: 패스 → 좋아요 전환
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

    /**
     * teams 컬렉션에서 활성 팀 목록을 PAGE_SIZE 단위로 가져온다.
     * 내가 속한 팀(teamIds 배열), 이미 액션한 팀은 필터링한다.
     *
     * @param loadMore true이면 이전 페이지 이후부터 조회 (페이지네이션)
     * @return Pair<팀 목록, 서버에 더 있는지 여부>
     */
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

            // 병렬 fetch: actionedIds + myTeamIds 를 동시에 조회해 지연을 절반으로 줄임
            val (actionedIds, myTeamIds) = coroutineScope {
                val actionedDeferred = async { currentUserId?.let { fetchActionedTeamIds(it) } ?: emptySet() }
                val myTeamDeferred   = async { currentUserId?.let { fetchMyTeamIds(it) } ?: emptyList() }
                Pair(actionedDeferred.await(), myTeamDeferred.await())
            }

            val teams = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)?.takeIf { it.teamId.isNotBlank() }
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

            val teams = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)?.takeIf { it.teamId.isNotBlank() }
            }

            val hasMore = snapshot.documents.size >= PAGE_SIZE

            Result.success(Pair(teams, hasMore))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 어드민이 데이터를 초기화했을 때 실시간으로 감지한다.
     *
     * AdminRepository가 resetSignals/{userId} 문서에 타임스탬프를 쓰면
     * 이 Flow가 신호를 방출 → FeedViewModel이 피드를 초기 상태로 리로드한다.
     */
    fun observeResetSignal(): Flow<Long> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            close()
            return@callbackFlow
        }

        val listener = db.collection("resetSignals").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val resetAt = snapshot.getLong("resetAt") ?: return@addSnapshotListener
                trySend(resetAt)
            }

        awaitClose { listener.remove() }
    }

    /**
     * users/{userId}의 teamIds 필드를 실시간으로 감시한다.
     * 좋아요 승인(autoAccept)으로 팀에 합류하면 즉시 방출 → FeedViewModel이 myTeamIds를 갱신한다.
     */
    fun observeMyTeamIds(): Flow<List<String>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                @Suppress("UNCHECKED_CAST")
                val teamIds = (snapshot.get("teamIds") as? List<String>) ?: emptyList()
                trySend(teamIds)
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
     * [변경] teamIds 배열에서 첫 번째 팀 ID를 fromTeamId로 사용.
     *
     * @param team   좋아요를 누른 팀
     * @param likeId ViewModel에서 UUID로 미리 생성한 Firestore 문서 ID
     */
    suspend fun saveLike(team: Team, likeId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            // 변경: 여러 팀 중 첫 번째 팀 ID를 fromTeamId로 사용
            // (좋아요를 보내는 팀 선택 로직이 필요하면 여기서 확장)
            val myTeamIds = fetchMyTeamIds(userId)
            val fromTeamId = myTeamIds.firstOrNull() ?: ""

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
     * 스와이프 선호도를 역산한다 (undo 지원).
     *
     * FieldValue.increment()를 사용하므로 원자적으로 처리된다.
     */
    suspend fun reversePreferenceScores(team: Team, wasLike: Boolean): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            val tagWeight  = if (wasLike) FeedConstants.TAG_PASS_WEIGHT  else FeedConstants.TAG_LIKE_WEIGHT
            val mbtiWeight = if (wasLike) FeedConstants.MBTI_PASS_WEIGHT else FeedConstants.MBTI_LIKE_WEIGHT
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
        val tagWeight   = if (isLike) FeedConstants.TAG_LIKE_WEIGHT  else FeedConstants.TAG_PASS_WEIGHT
        val mbtiWeight  = if (isLike) FeedConstants.MBTI_LIKE_WEIGHT else FeedConstants.MBTI_PASS_WEIGHT
        val teamIdField = if (isLike) "likedTeamIds" else "passedTeamIds"

        // set(merge=true) 한 번으로: 문서 없으면 생성, 있으면 병합
        // → 기존 2단계(set userId + update scores) 에서 Firestore 왕복 1회 절감
        val updates = mutableMapOf<String, Any>("userId" to userId)
        team.tags.forEach     { tag  -> updates["tagScores.$tag"]   = FieldValue.increment(tagWeight.toLong()) }
        team.mbtiTags.forEach { mbti -> updates["mbtiScores.$mbti"] = FieldValue.increment(mbtiWeight.toLong()) }
        updates[teamIdField] = FieldValue.arrayUnion(team.teamId)
        updates["updatedAt"] = System.currentTimeMillis()

        db.collection("userPreferences").document(userId)
            .set(updates, SetOptions.merge())
            .await()
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

    /** 현재 로그인 유저의 팀 ID 목록 반환 (ViewModel 등 외부 호출용) */
    suspend fun fetchMyTeamIds(): List<String> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return fetchMyTeamIds(userId)
    }

    // 변경: String 반환 → List<String> 반환
    private suspend fun fetchMyTeamIds(userId: String): List<String> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            @Suppress("UNCHECKED_CAST")
            (doc.get("teamIds") as? List<String>) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // =====================
    // 신규 팀 조회 (자동 갱신용)
    // =====================

    /**
     * 이미 큐에 있거나 액션한 팀을 제외하고 최신 활성 팀을 가져온다.
     *
     * FeedViewModel이 주기적으로 호출해 새로 생성된 팀을 추천 큐에 추가한다.
     * 페이지네이션 커서를 쓰지 않고 항상 최신순(top-N)으로 가져온다.
     *
     * @param excludeIds 이미 큐에 있거나 액션한 팀 ID 집합
     */
    suspend fun fetchNewTeams(excludeIds: Set<String>): Result<List<Team>> {
        return try {
            val userId = auth.currentUser?.uid

            val snapshot = db.collection("teams")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE.toLong())
                .get()
                .await()

            val teams = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)?.takeIf { it.teamId.isNotBlank() }
            }.filter { team ->
                !excludeIds.contains(team.teamId) &&
                (userId == null || !team.memberIds.contains(userId))
            }

            Result.success(teams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =====================
    // 상수
    // =====================

    companion object {
        /** 한 번에 불러올 팀 수 */
        const val PAGE_SIZE = 20
    }
}