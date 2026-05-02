package com.DeBiaseRamiro.gymera.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.DeBiaseRamiro.gymera.ui.screens.splash.SplashScreen
import com.DeBiaseRamiro.gymera.ui.screens.auth.LoginScreen
import com.DeBiaseRamiro.gymera.ui.screens.form.FormScreen
import com.DeBiaseRamiro.gymera.ui.screens.loading.LoadingScreen
import com.DeBiaseRamiro.gymera.domain.model.UserProfile


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
                    //por ahora aca voy poniendo las pantallas que voy haciendo para probar
                    //despues pongo la que va que es la home (que seria ROUTINE)
                    navController.navigate(Routes.LOADING_IA) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToForm = {
                    navController.navigate(Routes.FORM_IA) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRoutine = {
                    navController.navigate(Routes.ROUTINE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.FORM_IA) {
            FormScreen(
                onFormCompleted = {
                    navController.navigate(Routes.LOADING_IA) {
                        popUpTo(Routes.FORM_IA) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOADING_IA) {
            // Recuperamos el UserProfile que guardó el FormViewModel
            // Por ahora lo pasamos via SavedStateHandle - lo configuramos abajo
            LoadingScreen(
                userProfile = UserProfile(), // temporal - Nota importante: pasar el UserProfile entre pantallas lo vamos a resolver bien en el siguiente paso usando un SharedViewModel. Por ahora la llamada a Gemini no va a funcionar todavía hasta que conectemos el formulario correctamente.


                onRoutineGenerated = { routine ->
                    navController.navigate(Routes.ROUTINE) {
                        popUpTo(Routes.FORM_IA) { inclusive = true }
                    }
                },
                onError = {
                    navController.popBackStack()
                }
            )
        }

        // Las demás pantallas las vamos agregando en los próximos pasos
        // composable(Routes.LOGIN) { ... }
        // composable(Routes.ROUTINE) { ... }
    }
}