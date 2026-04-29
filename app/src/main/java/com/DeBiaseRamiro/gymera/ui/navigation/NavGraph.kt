package com.DeBiaseRamiro.gymera.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.DeBiaseRamiro.gymera.ui.screens.splash.SplashScreen


// Definimos todas las rutas de la app como constantes
object Routes {
    const val SPLASH         = "splash"
    const val LOGIN          = "login"
    const val FORM_IA        = "form_ia"
    const val LOADING_IA     = "loading_ia"
    const val ROUTINE        = "routine"
    const val DAY_DETAIL     = "day_detail/{dayId}"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val SEARCH         = "search"
}


@Composable
fun NavGraph(isUserLoggedIn: Boolean) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        // Splash
        composable(Routes.SPLASH) {
            SplashScreen(
                isUserLoggedIn = isUserLoggedIn,
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.ROUTINE) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Las demás pantallas las vamos agregando en los próximos pasos
        // composable(Routes.LOGIN) { ... }
        // composable(Routes.ROUTINE) { ... }
    }
}