package com.tehuberz.weather.lite.ui.state

import com.tehuberz.weather.lite.util.UiText

sealed interface BookmarkState {
    val message: UiText

    data class OnSuccess(
        override val message: UiText,
    ) : BookmarkState

    data class OnError(
        override val message: UiText,
    ) : BookmarkState

    data class OnDelete(
        override val message: UiText,
    ) : BookmarkState
}
