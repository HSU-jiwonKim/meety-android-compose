const functions = require("firebase-functions/v1");
const admin     = require("firebase-admin");

admin.initializeApp();

const db        = admin.firestore();
const messaging = admin.messaging();

/**
 * 새 채팅 메시지 → 수신자에게 FCM 알림
 * Firestore 경로: chats/{chatId}/messages/{messageId}
 */
exports.sendChatNotification = functions.firestore
  .document("chats/{chatId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const messageData = snap.data();
    if (!messageData) return null;

    const { senderId, content, type } = messageData;
    const chatId = context.params.chatId;

    if (type && type !== "text") return null;

    // 채팅방 참여자 조회
    const chatDoc = await db.collection("chats").doc(chatId).get();
    if (!chatDoc.exists) return null;

    const chatData   = chatDoc.data();
    const members    = chatData.members ?? chatData.participants ?? [];
    const roomName   = chatData.name ?? chatData.roomName ?? "채팅";
    const recipients = members.filter((uid) => uid !== senderId);
    if (recipients.length === 0) return null;

    // 발신자 이름 조회
    let senderName = "새 메시지";
    try {
      const senderDoc = await db.collection("users").doc(senderId).get();
      if (senderDoc.exists) {
        const d = senderDoc.data();
        senderName = d.name ?? d.displayName ?? "새 메시지";
      }
    } catch (_) {}

    // FCM 토큰 수집
    const userDocs = await Promise.all(
      recipients.map((uid) => db.collection("users").doc(uid).get())
    );
    const tokens = userDocs.map((d) => d.data()?.fcmToken).filter(Boolean);
    if (tokens.length === 0) return null;

    const body = content?.length > 50 ? content.substring(0, 50) + "…" : content ?? "(사진)";

    const response = await messaging.sendEachForMulticast({
      notification: {
        title: `${roomName} · ${senderName}`,
        body,
      },
      data: { chatId, type: "chat" },
      android: {
        priority: "high",
        notification: { channelId: "meety_chat", sound: "default" },
      },
      tokens,
    });

    console.log(`[chat:${chatId}] 성공 ${response.successCount} / 실패 ${response.failureCount}`);

    // 만료된 토큰 정리
    response.responses.forEach(async (res, idx) => {
      if (!res.success) {
        const code = res.error?.code;
        if (
          code === "messaging/invalid-registration-token" ||
          code === "messaging/registration-token-not-registered"
        ) {
          const uid = recipients[idx];
          if (uid) await db.collection("users").doc(uid).update({ fcmToken: null });
        }
      }
    });

    return null;
  });

/**
 * 수신 전화 알림
 * Firestore 경로: calls/{chatId}  (status = "calling" 일 때만 전송)
 */
exports.sendCallNotification = functions.firestore
  .document("calls/{chatId}")
  .onWrite(async (change, context) => {
    const after = change.after.data();
    if (!after || after.status !== "calling") return null;

    const { callerId, callType } = after;
    const chatId = context.params.chatId;

    // 채팅방 참여자 조회
    const chatDoc = await db.collection("chats").doc(chatId).get();
    if (!chatDoc.exists) return null;

    const chatData   = chatDoc.data();
    const members    = chatData.members ?? chatData.participants ?? [];
    const recipients = members.filter((uid) => uid !== callerId);
    if (recipients.length === 0) return null;

    // 채팅방 이름 (알림/네비게이션에 사용)
    const roomName = chatData.teamName ?? chatData.name ?? chatData.roomName ?? "채팅";
    const isGroup  = (members.length > 2) || chatData.type === "group" || chatData.type === "team";

    // 발신자 이름 조회
    let callerName = "Meety";
    try {
      const callerDoc = await db.collection("users").doc(callerId).get();
      if (callerDoc.exists) {
        const d = callerDoc.data();
        callerName = d.name ?? d.displayName ?? "Meety";
      }
    } catch (_) {}

    // FCM 토큰 수집
    const userDocs = await Promise.all(
      recipients.map((uid) => db.collection("users").doc(uid).get())
    );
    const tokens = userDocs.map((d) => d.data()?.fcmToken).filter(Boolean);
    if (tokens.length === 0) return null;

    const callLabel = callType === "video" ? "영상 통화" : "음성 통화";
    // 그룹 통화면 발신자 이름에 방 이름을 덧붙여 구분
    const displayCallerName = isGroup ? `${roomName} · ${callerName}` : callerName;

    // ⚠️ notification 필드 제거 → data-only 메시지
    // notification 필드가 있으면 앱이 꺼졌을 때 Firebase가 기본 알림을 직접 띄워서
    // onMessageReceived가 호출되지 않아 수락/거절 버튼이 안 붙음.
    // data-only면 항상 onMessageReceived가 호출되어 커스텀 알림(버튼 포함)을 만들 수 있음.
    const response = await messaging.sendEachForMulticast({
      data: {
        chatId,
        callerId,
        callType:      callType ?? "voice",
        type:          "incoming_call",
        callerName:    displayCallerName,
        callLabel,
        roomName,
      },
      android: {
        priority: "high",   // data-only일 때 반드시 high 설정해야 즉시 전달됨
      },
      tokens,
    });

    console.log(`[call:${chatId}] 성공 ${response.successCount} / 실패 ${response.failureCount}`);
    return null;
  });
