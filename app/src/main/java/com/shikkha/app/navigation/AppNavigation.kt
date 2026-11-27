package com.shikkha.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shikkha.app.R
import com.shikkha.app.auth.data.RootState
import com.shikkha.app.auth.ui.LoadingBox
import com.shikkha.app.auth.ui.LoginScreen
import com.shikkha.app.auth.ui.PendingApprovalScreen
import com.shikkha.app.auth.ui.RegisterScreen
import com.shikkha.app.auth.ui.RootViewModel
import com.shikkha.app.cq.ui.CqScreen
import com.shikkha.app.curriculum.ui.ChaptersScreen
import com.shikkha.app.curriculum.ui.SubjectsScreen
import com.shikkha.app.dashboard.DashboardDestination
import com.shikkha.app.dashboard.DashboardScreen
import com.shikkha.app.dashboard.PlannedFeatureScreen
import com.shikkha.app.mcq.ui.PracticeResultScreen
import com.shikkha.app.mcq.ui.PracticeScreen
import com.shikkha.app.mistakes.data.MISTAKES_MODE_ID
import com.shikkha.app.mistakes.ui.MistakesScreen
import com.shikkha.app.profile.ProfileScreen
import com.shikkha.app.settings.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PENDING = "pending"
    const val DASHBOARD = "dashboard"
    const val SUBJECTS = "subjects"
    const val CHAPTERS = "chapters/{subjectId}"
    const val PRACTICE = "practice/{chapterId}"
    const val PRACTICE_RESULT = "practice_result/{attemptId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val MISTAKES = "mistakes"
    const val CQ = "cq/{chapterId}"
    const val PLANNED = "planned/{titleRes}"

    fun chapters(subjectId: String) = "chapters/$subjectId"
    fun practice(chapterId: String) = "practice/$chapterId"
    fun cq(chapterId: String) = "cq/$chapterId"
    fun result(attemptId: String) = "practice_result/$attemptId"
    fun planned(titleRes: Int) = "planned/$titleRes"
}

private fun destinationFor(state: RootState): String? = when (state) {
    RootState.Loading -> null
    RootState.LoggedOut -> Routes.LOGIN
    is RootState.Pending, is RootState.Rejected, is RootState.Blocked -> Routes.PENDING
    is RootState.Ready -> Routes.DASHBOARD
}

/**
 * Single NavHost; the root session state decides the entry point, and every
 * auth-state transition (login, approval, logout) resets the back stack (Spec 3.2).
 */
@Composable
fun AppNavigation(
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val navController: NavHostController = rememberNavController()
    val rootState = rootViewModel.state

    if (rootState is RootState.Loading) {
        LoadingBox()
        return
    }

    var startDestination by remember { mutableStateOf<String?>(null) }
    if (startDestination == null) {
        startDestination = destinationFor(rootState) ?: Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination!!) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginResult = { state ->
                    if (state !is RootState.LoggedOut) rootViewModel.updateState(state)
                },
                onNavigateRegister = { navController.navigate(Routes.REGISTER) },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { rootViewModel.refreshStatus() },
                onEmailConfirmationNotice = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateLogin = { navController.popBackStack() },
            )
        }

        composable(Routes.PENDING) {
            val rejected = rootState !is RootState.Pending
            PendingApprovalScreen(
                rejected = rejected,
                checking = rootState is RootState.Loading,
                onCheckStatus = { rootViewModel.refreshStatus() },
                onSignOut = { rootViewModel.signOut() },
            )
        }

        composable(Routes.DASHBOARD) {
            val profile = (rootState as? RootState.Ready)?.profile
            if (profile == null) {
                // Defensive: graph should never offer this route without a profile.
                LaunchedEffect(Unit) { rootViewModel.refreshStatus() }
                LoadingBox()
            } else {
                DashboardScreen(
                    profile = profile,
                    onTileClick = { tile ->
                        when (tile.destination) {
                            DashboardDestination.Subjects ->
                                navController.navigate(Routes.SUBJECTS)
                            DashboardDestination.Practice ->
                                navController.navigate(Routes.SUBJECTS)
                            DashboardDestination.Mistakes ->
                                navController.navigate(Routes.MISTAKES)
                            DashboardDestination.Profile ->
                                navController.navigate(Routes.PROFILE)
                            DashboardDestination.Settings ->
                                navController.navigate(Routes.SETTINGS)
                            DashboardDestination.PlannedFeature ->
                                navController.navigate(Routes.planned(tile.titleRes))
                        }
                    },
                    onSignOut = { rootViewModel.signOut() },
                )
            }
        }

        composable(Routes.SUBJECTS) {
            val profile = (rootState as? RootState.Ready)?.profile
            if (profile == null) {
                LoadingBox()
            } else {
                SubjectsScreen(
                    classLevel = profile.classLevel,
                    medium = profile.medium,
                    onBack = { navController.popBackStack() },
                    onSubjectClick = { subjectId ->
                        navController.navigate(Routes.chapters(subjectId))
                    },
                )
            }
        }

        composable(
            route = Routes.CHAPTERS,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType }),
        ) { entry ->
            ChaptersScreen(
                subjectId = entry.arguments?.getString("subjectId").orEmpty(),
                onBack = { navController.popBackStack() },
                onStartPractice = { chapterId ->
                    navController.navigate(Routes.practice(chapterId))
                },
                onOpenCreativeQuestions = { chapterId ->
                    navController.navigate(Routes.cq(chapterId))
                },
            )
        }

        composable(
            route = Routes.CQ,
            arguments = listOf(navArgument("chapterId") { type = NavType.StringType }),
        ) {
            CqScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MISTAKES) {
            MistakesScreen(
                onBack = { navController.popBackStack() },
                onPracticeMistakes = {
                    navController.navigate(Routes.practice(MISTAKES_MODE_ID))
                },
            )
        }

        composable(
            route = Routes.PRACTICE,
            arguments = listOf(navArgument("chapterId") { type = NavType.StringType }),
        ) {
            PracticeScreen(
                onFinish = { attemptId ->
                    navController.navigate(Routes.result(attemptId)) {
                        popUpTo(Routes.DASHBOARD)
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PRACTICE_RESULT,
            arguments = listOf(navArgument("attemptId") { type = NavType.StringType }),
        ) {
            PracticeResultScreen(
                onBackToChapters = {
                    navController.navigate(Routes.SUBJECTS) {
                        popUpTo(Routes.DASHBOARD)
                    }
                },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onProfileUpdated = { rootViewModel.updateState(RootState.Ready(it)) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.PLANNED,
            arguments = listOf(navArgument("titleRes") { type = NavType.IntType }),
        ) { entry ->
            PlannedFeatureScreen(
                titleRes = entry.arguments?.getInt("titleRes") ?: R.string.planned_title,
                onBack = { navController.popBackStack() },
            )
        }
    }

    // Auth-state transitions: login, approval flip, logout — reset the stack.
    val target = destinationFor(rootState)
    LaunchedEffect(rootState) {
        if (target != null && navController.currentDestination != null &&
            navController.currentDestination?.route != target
        ) {
            // Only reset on transitions between auth phases, not on first entry.
            val currentRoute = navController.currentDestination?.route
            val authRoute = setOf(Routes.LOGIN, Routes.REGISTER, Routes.PENDING, Routes.DASHBOARD)
            if (currentRoute in authRoute) {
                navController.navigate(target) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
}
