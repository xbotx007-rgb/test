package com.progressvision.app.ui.navigation

object Routes {
    const val SPORT_LIST = "sport"
    const val SPORT_DETAIL = "sport/{trackerId}"
    fun sportDetail(id: Long) = "sport/$id"

    const val TASK_LIST = "task"
    const val TASK_DETAIL = "task/{taskId}"
    fun taskDetail(id: Long) = "task/$id"

    const val DAY = "day"
}
