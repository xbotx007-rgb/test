package com.progressvision.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.progressvision.app.ui.screens.day.DayScreen
import com.progressvision.app.ui.screens.sport.SportDetailScreen
import com.progressvision.app.ui.screens.sport.SportListScreen
import com.progressvision.app.ui.screens.task.TaskDetailScreen
import com.progressvision.app.ui.screens.task.TaskListScreen

sealed class TopDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Sport : TopDestination("sport", "Спорт", Icons.Filled.FitnessCenter)
    data object Task : TopDestination("task", "Задача", Icons.Filled.TaskAlt)
    data object Day : TopDestination("day", "День", Icons.Filled.CalendarToday)
}

private val topDestinations = listOf(TopDestination.Sport, TopDestination.Task, TopDestination.Day)

@Composable
fun ProgressVisionRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = topDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                if (currentRoute != dest.route) {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.Sport.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(TopDestination.Sport.route) {
                SportListScreen(onTrackerClick = { id ->
                    navController.navigate("sport/$id")
                })
            }
            composable(
                route = "sport/{trackerId}",
                arguments = listOf(navArgument("trackerId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("trackerId") ?: 0L
                SportDetailScreen(trackerId = id, onBack = { navController.popBackStack() })
            }
            composable(TopDestination.Task.route) {
                TaskListScreen(onTaskClick = { id ->
                    navController.navigate("task/$id")
                })
            }
            composable(
                route = "task/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("taskId") ?: 0L
                TaskDetailScreen(taskId = id, onBack = { navController.popBackStack() })
            }
            composable(TopDestination.Day.route) { DayScreen() }
        }
    }
}
