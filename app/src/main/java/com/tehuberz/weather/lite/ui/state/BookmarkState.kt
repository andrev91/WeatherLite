package com.tehuberz.weather.lite.ui.state

import com.tehuberz.weather.lite.util.UiText

sealed interface BookmarkState {
    val message: UiText
    data class onSuccess(override val message: UiText) : BookmarkState
    data class onError(override val message: UiText) : BookmarkState
    data class onDelete(override val message: UiText) : BookmarkState
}