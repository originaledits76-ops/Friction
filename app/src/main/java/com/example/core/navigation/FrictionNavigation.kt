package com.example.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.AuthStatus
import com.example.features.auth.LoginScreen
import com.example.features.auth.LoginViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.FrictionRepository
import com.example.features.home.HomeScreen
import com.example.features.home.HomeViewModel
import com.example.features.splash.SplashScreen
import com.example.features.onboarding.OnboardingScreen
import com.example.features.permission.PermissionsFlowScreen

const val ROUTE_SPLASH = "splash"
const val ROUTE_LOGIN = "login"
const val ROUTE_HOME = "home"
const val ROUTE_ONBOARDING = "onboarding"
const val ROUTE_PERMISSIONS = "permissions"

@Composable
fun FrictionNavigationHost(
    loginViewModel: LoginViewModel,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val authStatus by loginViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = ROUTE_SPLASH,
        modifier = modifier.fillMaxSize()
    ) {
        
        // 1. Splash Screen Node
        composable(
            route = ROUTE_SPLASH,
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }
        ) {
            SplashScreen(
                onSplashComplete = {
                    if (authStatus is AuthStatus.Authenticated) {
                        val user = (authStatus as AuthStatus.Authenticated).user
                        if (user.goal.isEmpty()) {
                            navController.navigate(ROUTE_ONBOARDING) {
                                popUpTo(ROUTE_SPLASH) { inclusive = true }
                            }
                        } else {
                            navController.navigate(ROUTE_PERMISSIONS) {
                                popUpTo(ROUTE_SPLASH) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate(ROUTE_LOGIN) {
                            popUpTo(ROUTE_SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 2. Login Screen Node
        composable(
            route = ROUTE_LOGIN,
            enterTransition = { 
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up, 
                    animationSpec = tween(500)
                ) + fadeIn(animationSpec = tween(500)) 
            },
            exitTransition = { 
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down, 
                    animationSpec = tween(500)
                ) + fadeOut(animationSpec = tween(500)) 
            }
        ) {
            LaunchedEffect(authStatus) {
                if (authStatus is AuthStatus.Authenticated) {
                    val user = (authStatus as AuthStatus.Authenticated).user
                    if (user.goal.isEmpty()) {
                        navController.navigate(ROUTE_ONBOARDING) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    } else {
                        navController.navigate(ROUTE_PERMISSIONS) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    }
                }
            }
            LoginScreen(viewModel = loginViewModel)
        }

        // Onboarding Screen Node
        composable(
            route = ROUTE_ONBOARDING,
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            OnboardingScreen(
                onComplete = { name, age, goal, customGoal, motivation ->
                    loginViewModel.updateOnboardingData(name, age, goal, customGoal, motivation)
                    navController.navigate(ROUTE_PERMISSIONS) {
                        popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // 3. Home Screen Node
        composable(
            route = ROUTE_HOME,
            enterTransition = { fadeIn(animationSpec = tween(600)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            LaunchedEffect(authStatus) {
                if (authStatus is AuthStatus.Unauthenticated) {
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(ROUTE_HOME) { inclusive = true }
                    }
                }
            }

            if (authStatus is AuthStatus.Authenticated) {
                val user = (authStatus as AuthStatus.Authenticated).user
                LaunchedEffect(user.uid) {
                    homeViewModel.setUserContext(user.uid)
                }

                val currentLevel by homeViewModel.userLevel.collectAsState()
                val currentXp by homeViewModel.userXp.collectAsState()
                val currentStreak by homeViewModel.userStreak.collectAsState()

                val dynamicUser = user.copy(
                    level = currentLevel,
                    xp = currentXp,
                    currentStreak = currentStreak
                )

                HomeScreen(
                    user = dynamicUser,
                    loginViewModel = loginViewModel,
                    homeViewModel = homeViewModel
                )
            }
        }

        // 4. Permissions Flow Node
        composable(
            route = ROUTE_PERMISSIONS,
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            PermissionsFlowScreen(
                homeViewModel = homeViewModel,
                onComplete = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }
    }
}
