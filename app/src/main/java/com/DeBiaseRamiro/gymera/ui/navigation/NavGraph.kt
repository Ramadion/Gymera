package com.DeBiaseRamiro.gymera.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.ui.components.BottomNavBar
import com.DeBiaseRamiro.gymera.ui.screens.auth.LoginScreen
import com.DeBiaseRamiro.gymera.ui.screens.daydetail.DayDetailScreen
import com.DeBiaseRamiro.gymera.ui.screens.form.FormScreen
import com.DeBiaseRamiro.gymera.ui.screens.loading.LoadingScreen
import com.DeBiaseRamiro.gymera.ui.screens.routine.RoutineScreen
import com.DeBiaseRamiro.gymera.ui.screens.splash.SplashScreen
import com.DeBiaseRamiro.gymera.ui.shared.SharedRoutineViewModel
import androidx.navigation.navArgument
import com.DeBiaseRamiro.gymera.ui.screens.exercisedetail.ExerciseDetailScreen

object Routes {
    const val SPLASH          = "splash"
    const val LOGIN           = "login"
    const val FORM_IA         = "form_ia"
    const val LOADING_IA      = "loading_ia"
    const val ROUTINE         = "routine"
    const val DAY_DETAIL      = "day_detail/{dayId}"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val SEARCH          = "search"

    fun dayDetail(dayId: String)           = "day_detail/$dayId"
    fun exerciseDetail(exerciseId: String) = "exercise_detail/$exerciseId"
}

/**
 * Rutas donde se muestra la BottomNavBar.
 * Todas las demás pantallas (splash, login, form, loading, detalle) la ocultan.
 */
private val bottomNavRoutes = setOf(
    Routes.ROUTINE,
    Routes.SEARCH
)

@Composable
fun NavGraph(isUserLoggedIn: Boolean) {

    val navController = rememberNavController()

    val sharedRoutineViewModel: SharedRoutineViewModel = hiltViewModel()
    val currentRoutine by sharedRoutineViewModel.currentRoutine.collectAsState()

    // Observamos la ruta actual para saber qué ítem resaltar
    // y si debemos mostrar la BottomNavBar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // La BottomNavBar solo se muestra en las pantallas principales
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            // Solo renderizamos la barra si corresponde a la ruta actual
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            // Evitamos apilar la misma pantalla si ya estamos en ella
                            launchSingleTop = true
                            // Al cambiar de tab, volvemos al inicio de ese tab
                            // sin acumular el back stack
                            restoreState = true
                            popUpTo(Routes.ROUTINE) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            // El padding del Scaffold (espacio que ocupa la BottomNavBar)
            // se lo pasamos al NavHost para que el contenido no quede tapado
            modifier = Modifier.padding(innerPadding)
        ) {

            // ── Splash ────────────────────────────────────────────────────
            composable(Routes.SPLASH) {
                SplashScreen(
                    // Ya no pasa isUserLoggedIn — el SplashViewModel lo maneja solo
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToForm = {
                        navController.navigate(Routes.FORM_IA) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToRoutine = {
                        navController.navigate(Routes.ROUTINE) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            // ── Login ─────────────────────────────────────────────────────
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

            // ── Formulario ────────────────────────────────────────────────
            composable(Routes.FORM_IA) {
                FormScreen(
                    onFormCompleted = { userProfile ->
                        sharedRoutineViewModel.setUserProfile(userProfile)
                        navController.navigate(Routes.LOADING_IA) {
                            popUpTo(Routes.FORM_IA) { inclusive = true }
                        }
                    }
                )
            }

            // ── Loading IA ────────────────────────────────────────────────
            composable(Routes.LOADING_IA) {
                val userProfile by sharedRoutineViewModel.pendingUserProfile.collectAsState()

                LoadingScreen(
                    userProfile = userProfile ?: UserProfile(),
                    onRoutineGenerated = { _ ->
                        // Ya NO llamamos setRoutine() — la rutina ya está en Room
                        // y el Flow de SharedRoutineViewModel la emite automáticamente.
                        // Solo limpiamos el perfil pendiente y navegamos.
                        sharedRoutineViewModel.clearUserProfile()
                        navController.navigate(Routes.ROUTINE) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onError = { navController.popBackStack() }
                )
            }

            // ── Rutina Semanal ────────────────────────────────────────────
            composable(Routes.ROUTINE) {
                val routine = currentRoutine
                if (routine == null) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.FORM_IA) {
                            popUpTo(Routes.ROUTINE) { inclusive = true }
                        }
                    }
                } else {
                    RoutineScreen(
                        routine = routine,
                        onDaySelected = { dayId ->
                            navController.navigate(Routes.dayDetail(dayId))
                        },
                        onGenerateNew = {
                            sharedRoutineViewModel.clearRoutine()
                            navController.navigate(Routes.FORM_IA) {
                                popUpTo(Routes.ROUTINE) { inclusive = true }
                            }
                        }
                    )
                }
            }

            // ── Day Detail ────────────────────────────────────────────────
            composable(
                route = Routes.DAY_DETAIL,
                arguments = listOf(navArgument("dayId") { type = NavType.StringType })
            ) { backStackEntry ->
                val dayId = backStackEntry.arguments?.getString("dayId") ?: ""
                val workoutDay = currentRoutine?.workoutDays?.find { it.id == dayId }

                if (workoutDay == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    DayDetailScreen(
                        workoutDay = workoutDay,
                        onBack = { navController.popBackStack() },
                        onExerciseClick = { route ->
                            // route ya viene completa con todos los parámetros encodeados
                            navController.navigate(route)
                        }
                    )
                }
            }

            // ── Exercise Detail (feature/exercise-detail) ─────────────────
            composable(
                // Usamos query params para pasar múltiples valores de tipos simples
                // La ruta queda: exercise_detail?nameEn=...&nameEs=...&sets=...&reps=...&rest=...&notes=...
                route = "exercise_detail" +
                        "?nameEn={nameEn}" +
                        "&nameEs={nameEs}" +
                        "&sets={sets}" +
                        "&reps={reps}" +
                        "&restSeconds={restSeconds}" +
                        "&notes={notes}",
                arguments = listOf(
                    navArgument("nameEn")      { type = NavType.StringType; defaultValue = "" },
                    navArgument("nameEs")      { type = NavType.StringType; defaultValue = "" },
                    navArgument("sets")        { type = NavType.IntType;    defaultValue = 0  },
                    navArgument("reps")        { type = NavType.StringType; defaultValue = "" },
                    navArgument("restSeconds") { type = NavType.IntType;    defaultValue = 60 },
                    navArgument("notes")       { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments!!
                ExerciseDetailScreen(
                    nameEn      = args.getString("nameEn")      ?: "",
                    nameEs      = args.getString("nameEs")      ?: "",
                    sets        = args.getInt("sets"),
                    reps        = args.getString("reps")        ?: "",
                    restSeconds = args.getInt("restSeconds"),
                    notes       = args.getString("notes")       ?: "",
                    onBack      = { navController.popBackStack() }
                )
            }


            // ── Search (feature/search) ───────────────────────────────────
            composable(Routes.SEARCH) {
                // TODO: SearchScreen()
            }
        }
    }
}