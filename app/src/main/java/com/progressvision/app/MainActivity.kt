package com.progressvision.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.progressvision.app.ui.day.DayScreen
import com.progressvision.app.ui.navigation.Routes
import com.progressvision.app.ui.sport.SportDetailScreen
import com.progressvision.app.ui.sport.SportListScreen
import com.progressvision.app.ui.sport.SportWorkoutDetailScreen
import com.progressvision.app.ui.task.TaskDetailScreen
import com.progressvision.app.ui.task.TaskListScreen
import com.progressvision.app.ui.theme.ProgressVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProgressVisionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    AppRoot()
                }
            }
        }
    }
}

private data class TopTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        TopTab(Routes.SPORT_LIST, "Спорт", Icons.Filled.FitnessCenter),
        TopTab(Routes.TASK_LIST, "Задача", Icons.Filled.CheckCircle),
        TopTab(Routes.DAY, "День", Icons.Filled.Today)
    )

    Scaffold(
        bottomBar = {
            val showBar = currentRoute == null || tabs.any { it.route == currentRoute }
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        AppNavHost(nav = nav, padding = padding)
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, padding: PaddingValues) {
    NavHost(
        navController = nav,
        startDestination = Routes.SPORT_LIST,
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        composable(Routes.SPORT_LIST) {
            SportListScreen(
                onOpen = { id -> nav.navigate(Routes.sportWorkoutDetail(id)) }
            )
        }
        composable(
            Routes.SPORT_WORKOUT_DETAIL,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("workoutId") ?: 0L
            SportWorkoutDetailScreen(
                workoutId = id,
                onBack = { nav.popBackStack() },
                onOpenExercise = { exId -> nav.navigate(Routes.sportExerciseDetail(exId)) }
            )
        }
        composable(
            Routes.SPORT_EXERCISE_DETAIL,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("exerciseId") ?: 0L
            SportDetailScreen(exerciseId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.TASK_LIST) {
            TaskListScreen(onOpen = { id -> nav.navigate(Routes.taskDetail(id)) })
        }
        composable(
            Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("taskId") ?: 0L
            TaskDetailScreen(bigTaskId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.DAY) {
            DayScreen()
        }
    }
}
