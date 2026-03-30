package com.bugzero.meety.ui.feed

import com.bugzero.meety.ui.team.Team
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * 피드 화면 전용 Firebase Repository
 *
 * - teams 컬렉션에서 팀 목록을 가져옴
 * - 내가 속한 팀은 피드에서 제외
 * - 실시간 업데이트(Flow) 또는 1회성 조회 모두 지원
 */
class FeedRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * teams 컬렉션에서 활성(active) 팀 목록을 1회 가져온다.
     *
     * - status가 "active"인 팀만 조회
     * - 내가 이미 속한 팀은 제외
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

            val teams = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)
            }.filter { team ->
                // 내가 속한 팀은 피드에 보여주지 않음
                currentUserId == null || !team.memberIds.contains(currentUserId)
            }

            Result.success(teams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * teams 컬렉션의 실시간 변경사항을 Flow로 받는다.
     *
     * 팀이 새로 생기거나 삭제되면 자동으로 최신 목록이 내려온다.
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

        // Flow가 취소(화면 이탈 등)되면 리스너 자동 해제 → 메모리 누수 방지
        awaitClose { listener.remove() }
    }

    // =====================
    // 프로필 관련
    // =====================

    /**
     * 현재 로그인한 유저의 프로필 정보를 가져온다.
     */
    suspend fun fetchMyProfile(): Result<Map<String, Any>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인된 사용자가 없습니다."))

            val doc = db.collection("users").document(userId).get().await()
            val data = doc.data ?: emptyMap()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 현재 유저의 프로필 정보를 업데이트한다.
     *
     * @param fields 업데이트할 필드 맵 (예: mapOf("name" to "김민지", "mbti" to "ENFP"))
     */
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
}
