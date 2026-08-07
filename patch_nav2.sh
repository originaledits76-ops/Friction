sed -i 's/navController.navigate(ROUTE_HOME) {/navController.navigate(ROUTE_PERMISSIONS) {/g' app/src/main/java/com/example/core/navigation/FrictionNavigation.kt
sed -i 's/popUpTo(ROUTE_ONBOARDING) { inclusive = true }/popUpTo(ROUTE_ONBOARDING) { inclusive = true }\n                    }/g' app/src/main/java/com/example/core/navigation/FrictionNavigation.kt
cat << 'INNER_EOF' >> app/src/main/java/com/example/core/navigation/FrictionNavigation.kt

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
INNER_EOF
