package com.bugzero.meety.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzero.meety.data.model.InAppNotification
import com.bugzero.meety.data.repository.InAppNotificationRepository
import com.bugzero.meety.ui.team.FirebaseTeamRepository
import com.bugzero.meety.ui.team.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 상단 알림 버튼/알림 목록 화면 공용 ViewModel.
 *
 * "읽음 = 삭제" 모델이라 unreadCount = 현재 알림 개수와 동일하다.
 * 좋아요 수락/거절은 TeamRepository에 위임한다.
 */
class NotificationViewModel(
    private val repository: InAppNotificationRepository = InAppNotificationRepository(),
    private val teamRepository: TeamRepository = FirebaseTeamRepository()
) : ViewModel() {

    val notifications: StateFlow<List<InAppNotification>> =
        repository.observeMyNotifications()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> =
        notifications
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** UI에 일회성 메시지 전달용 (수락/거절 결과 토스트 등). 소비 후 null로 비움. */
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun consumeActionMessage() { _actionMessage.value = null }

    /** 알림 한 개 삭제 (= 그 한 건만 읽음 처리) */
    fun deleteOne(notificationId: String) {
        viewModelScope.launch {
            repository.deleteOne(notificationId)
        }
    }

    /** 알림 전체 삭제 (= 화면을 떠나면서 모두 읽음 처리) */
    fun clearAll() {
        viewModelScope.launch {
            repository.deleteAllMine()
        }
    }

    /**
     * 좋아요 수락. 성공 시 해당 좋아요 알림도 삭제한다.
     *
     * @param likeId 알림의 relatedId에 들어있던 likeId
     * @param notificationId 알림 문서 ID — 처리 후 삭제용
     */
    fun acceptLike(likeId: String, notificationId: String) {
        if (likeId.isBlank()) return
        teamRepository.acceptReceivedLike(
            likeId = likeId,
            onSuccess = {
                _actionMessage.value = "좋아요를 수락했어요"
                deleteOne(notificationId)
            },
            onFailure = { msg ->
                _actionMessage.value = "수락 실패: $msg"
            }
        )
    }

    /**
     * 좋아요 거절. 성공 시 해당 좋아요 알림도 삭제한다.
     */
    fun rejectLike(likeId: String, notificationId: String) {
        if (likeId.isBlank()) return
        teamRepository.rejectReceivedLike(
            likeId = likeId,
            onSuccess = {
                _actionMessage.value = "좋아요를 거절했어요"
                deleteOne(notificationId)
            },
            onFailure = { msg ->
                _actionMessage.value = "거절 실패: $msg"
            }
        )
    }
}
