package com.example.myscreenshot.navigation

object Routes {
    const val HOME = "home"
    const val REVIEW = "review"
    const val SETTINGS = "settings"
    const val EMPTY = "empty"
    const val DETAIL = "detail"
    const val REVIEW_SAMPLE = "review_sample"

    fun detail(id: String) = "$DETAIL/$id"
}
