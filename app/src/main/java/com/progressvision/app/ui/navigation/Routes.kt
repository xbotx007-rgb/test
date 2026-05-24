package com.progressvision.app.ui.navigation

object Routes {
    const val SPORT_LIST = "sport"
    const val SPORT_WORKOUT_DETAIL = "sport/workout/{workoutId}"
    fun sportWorkoutDetail(id: Long) = "sport/workout/$id"
    const val SPORT_EXERCISE_DETAIL = "sport/exercise/{exerciseId}"
    fun sportExerciseDetail(id: Long) = "sport/exercise/$id"

    const val TASK_LIST = "task"
    const val TASK_DETAIL = "task/{taskId}"
    fun taskDetail(id: Long) = "task/$id"

    const val DAY = "day"
}
