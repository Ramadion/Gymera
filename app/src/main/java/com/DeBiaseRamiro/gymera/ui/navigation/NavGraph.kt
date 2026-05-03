package com.DeBiaseRamiro.gymera.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.ui.screens.auth.LoginScreen
import com.DeBiaseRamiro.gymera.ui.screens.form.FormScreen
import com.DeBiaseRamiro.gymera.ui.screens.loading.LoadingScreen
import com.DeBiaseRamiro.gymera.ui.screens.routine.RoutineScreen
import com.DeBiaseRamiro.gymera.ui.screens.splash.SplashScreen
import com.DeBiaseRamiro.gymera.ui.shared.SharedRoutineViewModel

// Todas las rutas de la app como constantes
object Routes {
    const val SPLASH          = "splash"
    const val LOGIN           = "login"
    const val FORM_IA         = "form_ia"
    const val LOADING_IA      = "loading_ia"
    const val ROUTINE         = "routine"
    const val DAY_DETAIL      = "day_detail/{dayId}"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val SEARCH          = "search"

    // Helpers para construir URLs con argumentos
    fun dayDetail(dayId: String) = "day_detail/$dayId"
    fun exerciseDetail(exerciseId: String) = "exercise_detail/$exerciseId"
}

/**
 * NavGraph — Grafo de navegación principal de Gymera.
 *
 * Recibe isUserLoggedIn desde MainActivity (que lo obtiene de Firebase)
 * para que SplashScreen pueda decidir a dónde navegar sin hacer
 * llamadas asíncronas.
 *
 * El SharedRoutineViewModel se crea acá (una sola vez, en el scope
 * del NavGraph) para que LoadingScreen y RoutineScreen compartan
 * la misma instancia y puedan pasarse la Routine entre sí.
 */
@Composable
fun NavGraph(isUserLoggedIn: Boolean) {

    val navController = rememberNavController()

    // SharedRoutineViewModel: vive en el scope de este Composable (NavGraph),
    // que es el Composable raíz. Al ser creado con hiltViewModel() aquí,
    // la misma instancia es accesible en cualquier composable hijo que lo pida
    // pasándola como parámetro (que es exactamente lo que hacemos abajo).
    val sharedRoutineViewModel: SharedRoutineViewModel = hiltViewModel()

    // Observamos la rutina actual para pasársela a RoutineScreen
    val currentRoutine by sharedRoutineViewModel.currentRoutine.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                isUserLoggedIn = isUserLoggedIn,
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    // TODO (feature/room): cuando tengamos Room, acá verificamos
                    // si el usuario tiene rutina guardada y navegamos a ROUTINE.
                    // Por ahora, si está logueado va al formulario.
                    navController.navigate(Routes.FORM_IA) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Login ─────────────────────────────────────────────────────────────
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

        // ── Formulario ────────────────────────────────────────────────────────
        composable(Routes.FORM_IA) {
            FormScreen(
                onFormCompleted = {
                    // FormScreen ya le pasa el UserProfile a LoadingScreen
                    // via SharedRoutineViewModel (ver abajo cómo lo resolvemos)
                    navController.navigate(Routes.LOADING_IA) {
                        popUpTo(Routes.FORM_IA) { inclusive = true }
                    }
                }
            )
        }

        // ── Loading IA ────────────────────────────────────────────────────────
        composable(Routes.LOADING_IA) {
            LoadingScreen(
                // Por ahora UserProfile() vacío — en feature/room conectamos
                // el FormViewModel correctamente usando el SharedRoutineViewModel
                // para guardar el perfil antes de navegar.
                userProfile = UserProfile(),
                onRoutineGenerated = { routine ->
                    // ✅ Guardamos la rutina generada en el SharedViewModel
                    // para que RoutineScreen pueda leerla
                    sharedRoutineViewModel.setRoutine(routine)
                    navController.navigate(Routes.ROUTINE) {
                        // Limpiamos hasta SPLASH para que el back button
                        // no vuelva al Loading ni al Form
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onError = {
                    navController.popBackStack()
                }
            )
        }

        // ── Rutina Semanal ────────────────────────────────────────────────────
        composable(Routes.ROUTINE) {
            // Si por alguna razón la rutina es null (ej: el proceso fue
            // interrumpido), volvemos al formulario como fallback seguro.
            val routine = currentRoutine
            if (routine == null) {
                navController.navigate(Routes.FORM_IA) {
                    popUpTo(Routes.ROUTINE) { inclusive = true }
                }
            } else {
                RoutineScreen(
                    routine = routine,
                    onDaySelected = { dayId ->
                        // feature/day-detail — próximo paso
                        navController.navigate(Routes.dayDetail(dayId))
                    },
                    onGenerateNew = {
                        // Borramos la rutina del SharedViewModel y volvemos
                        // al formulario para que el usuario genere una nueva
                        sharedRoutineViewModel.clearRoutine()
                        navController.navigate(Routes.FORM_IA) {
                            popUpTo(Routes.ROUTINE) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ── Day Detail (feature/day-detail) ───────────────────────────────────
        composable(
            route = Routes.DAY_DETAIL,
            arguments = listOf(navArgument("dayId") { type = NavType.StringType })
        ) { backStackEntry ->
            val dayId = backStackEntry.arguments?.getString("dayId") ?: ""
            // TODO: DayDetailScreen(dayId = dayId)
        }

        // ── Exercise Detail (feature/exercise-detail) ─────────────────────────
        composable(
            route = Routes.EXERCISE_DETAIL,
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            // TODO: ExerciseDetailScreen(exerciseId = exerciseId)
        }

        // ── Search (feature/search) ───────────────────────────────────────────
        composable(Routes.SEARCH) {
            // TODO: SearchScreen()
        }
    }
}