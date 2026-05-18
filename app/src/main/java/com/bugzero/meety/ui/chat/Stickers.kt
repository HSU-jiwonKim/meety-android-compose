package com.bugzero.meety.ui.chat

import androidx.annotation.DrawableRes
import com.bugzero.meety.R

/**
 * 이모티콘(스티커) 카탈로그.
 *
 * Firestore 의 messages 컬렉션에는 type="sticker" 로 저장되고,
 * content 필드에 sticker id (예: "sticker_03") 가 들어갑니다.
 *
 * 새 스티커 추가 절차:
 *  1) PNG 를 app/src/main/res/drawable/ 에 sticker_NN.png 로 저장 (NN 은 두 자리 0패딩)
 *  2) 아래 STICKER_IDS 리스트에 "sticker_NN" 추가
 *  3) drawableFor() when 분기에도 한 줄 추가
 *
 * 잘못된 id 가 들어왔거나 리소스가 없을 땐 fallback 으로 sticker_01 을 보여줍니다.
 */
object Stickers {

    /** 현재 사용 가능한 스티커 id 목록 (피커에 표시되는 순서) */
    val STICKER_IDS: List<String> = listOf(
        "sticker_01",
        "sticker_02",
        "sticker_03",
        "sticker_05",
        "sticker_06",
        "sticker_07",
        "sticker_09",
        "sticker_10",
        "sticker_11",
        "sticker_12",
        "sticker_13",
        "sticker_14",
        "sticker_15",
        "sticker_16",
        "sticker_17",
        "sticker_18",
        "sticker_19",
        "sticker_20",
        "sticker_21",
        "sticker_22",
        "sticker_23"
    )

    /** id → drawable resource. 못 찾으면 fallback */
    @DrawableRes
    fun drawableFor(stickerId: String): Int = when (stickerId) {
        "sticker_01" -> R.drawable.sticker_01
        "sticker_02" -> R.drawable.sticker_02
        "sticker_03" -> R.drawable.sticker_03
        "sticker_05" -> R.drawable.sticker_05
        "sticker_06" -> R.drawable.sticker_06
        "sticker_07" -> R.drawable.sticker_07
        "sticker_09" -> R.drawable.sticker_09
        "sticker_10" -> R.drawable.sticker_10
        "sticker_11" -> R.drawable.sticker_11
        "sticker_12" -> R.drawable.sticker_12
        "sticker_13" -> R.drawable.sticker_13
        "sticker_14" -> R.drawable.sticker_14
        "sticker_15" -> R.drawable.sticker_15
        "sticker_16" -> R.drawable.sticker_16
        "sticker_17" -> R.drawable.sticker_17
        "sticker_18" -> R.drawable.sticker_18
        "sticker_19" -> R.drawable.sticker_19
        "sticker_20" -> R.drawable.sticker_20
        "sticker_21" -> R.drawable.sticker_21
        "sticker_22" -> R.drawable.sticker_22
        "sticker_23" -> R.drawable.sticker_23
        else -> R.drawable.sticker_01
    }

    /** content 값이 스티커 id 형식인지 확인 */
    fun isStickerId(content: String): Boolean =
        content.startsWith("sticker_") && content.length <= 16
}
