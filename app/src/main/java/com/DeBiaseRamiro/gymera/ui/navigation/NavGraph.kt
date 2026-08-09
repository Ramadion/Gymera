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
import com.DeBiaseRamiro.gymera.ui.screens.exercisedetail.ExerciseDetailScreen
import com.DeBiaseRamiro.gymera.ui.screens.profile.ProfileScreen
import com.DeBiaseRamiro.gymera.ui.screens.search.SearchScreen
import com.DeBiaseRamiro.gymera.ui.screens.daydetail.encodeForNav
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator

object Routes {
    const val SPLASH          = "splash"
    const val LOGIN           = "login"
    const val FORM_IA         = "form_ia"
    const val LOADING_IA      = "loading_ia"
    const val ROUTINE         = "routine"
    const val DAY_DETAIL      = "day_detail/{dayId}"
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}"
    const val SEARCH          = "search"
    const val PROFILE         = "profile"

    fun dayDetail(dayId: String)           = "day_detail/$dayId"
    fun exerciseDetail(exerciseId: String) = "exercise_detail/$exerciseId"
}

private val bottomNavRoutes = setOf(
    Routes.ROUTINE,
    Routes.SEARCH,
    Routes.PROFILE
)

@Composable
fun NavGraph(isUserLoggedIn: Boolean) {

    val navController = rememberNavController()
    val sharedRoutineViewModel: SharedRoutineViewModel = hiltViewModel()
    val currentRoutine by sharedRoutineViewModel.currentRoutine.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
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
            modifier = Modifier.padding(innerPadding)
        ) {

            // ── Splash ────────────────────────────────────────────────────
            composable(Routes.SPLASH) {
                SplashScreen(
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
                    onRoutineGenerated = { routine ->

                        sharedRoutineViewModel.setRoutine(routine)
                        sharedRoutineViewModel.clearUserProfile()

                        navController.navigate(Routes.ROUTINE) {
                            popUpTo(Routes.LOADING_IA) { inclusive = true }
                        }
                    },
                    onError = { navController.popBackStack() }
                )
            }

            // ── Rutina Semanal ────────────────────────────────────────────
            composable(Routes.ROUTINE) {
                val routine = currentRoutine

                // shouldRedirectToForm actúa como fallback de seguridad:
                // si después de 600ms currentRoutine SIGUE siendo null
                // (el usuario llegó aquí sin rutina), redirigimos al Form.
                //
                // Con el fix de setRoutine(), este fallback casi nunca se
                // activa al llegar desde LoadingScreen, porque routine ya
                // tiene valor en la primera composición.
                var shouldRedirectToForm by remember { mutableStateOf(false) }

                LaunchedEffect(routine) {
                    if (routine == null) {
                        kotlinx.coroutines.delay(600L)
                        // Re-chequeamos después del delay: si sigue null, redirigimos
                        shouldRedirectToForm = true
                    } else {
                        // Llegó la rutina — cancelamos cualquier redirect pendiente
                        shouldRedirectToForm = false
                    }
                }

                when {
                    routine != null -> {
                        RoutineScreen(
                            routine       = routine,
                            onDaySelected = { dayId ->
                                navController.navigate(Routes.dayDetail(dayId))
                            },
                            onGenerateNew = {
                                // clearRoutine() ahora limpia _currentRoutine de forma
                                // inmediata (no espera a Room), por lo que la UI
                                // responde al instante.
                                sharedRoutineViewModel.clearRoutine()
                                navController.navigate(Routes.FORM_IA) {
                                    popUpTo(Routes.ROUTINE) { inclusive = true }
                                }
                            }
                        )
                    }

                    shouldRedirectToForm -> {
                        // Fallback: si después de 600ms no hay rutina, vamos al Form
                        LaunchedEffect(Unit) {
                            navController.navigate(Routes.FORM_IA) {
                                popUpTo(Routes.ROUTINE) { inclusive = true }
                            }
                        }
                    }

                    else -> {
                        // Spinner mientras esperamos la emisión de Room
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(com.DeBiaseRamiro.gymera.ui.theme.BackgroundDark),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = com.DeBiaseRamiro.gymera.ui.theme.PurplePrimary
                            )
                        }
                    }
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
                            navController.navigate(route)
                        }
                    )
                }
            }

            // ── Exercise Detail ───────────────────────────────────────────
            composable(
                route = "exercise_detail" +
                        "?dayId={dayId}" +
                        "&exerciseId={exerciseId}" +
                        "&nameEn={nameEn}" +
                        "&nameEs={nameEs}" +
                        "&sets={sets}" +
                        "&reps={reps}" +
                        "&restSeconds={restSeconds}" +
                        "&notes={notes}",
                arguments = listOf(
                    navArgument("dayId")       { type = NavType.StringType; defaultValue = "" },
                    navArgument("exerciseId")  { type = NavType.StringType; defaultValue = "" },
                    navArgument("nameEn")      { type = NavType.StringType; defaultValue = "" },
                    navArgument("nameEs")      { type = NavType.StringType; defaultValue = "" },
                    navArgument("sets")        { type = NavType.IntType;    defaultValue = 0  },
                    navArgument("reps")        { type = NavType.StringType; defaultValue = "" },
                    navArgument("restSeconds") { type = NavType.IntType;    defaultValue = 60 },
                    navArgument("notes")       { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments!!
                val dayId = args.getString("dayId") ?: ""
                val exerciseId = args.getString("exerciseId") ?: ""

                val currentDay = currentRoutine?.workoutDays?.find { it.id == dayId }
                val currentIndex = currentDay?.exercises?.indexOfFirst { it.id == exerciseId } ?: -1
                val exerciseCount = currentDay?.exercises?.size ?: 0
                val hasNext = currentIndex >= 0 && currentIndex < exerciseCount - 1

                val onNextAction: (() -> Unit)? = if (currentIndex >= 0 && exerciseCount > 0) {
                    if (hasNext) {
                        val next = currentDay!!.exercises[currentIndex + 1]
                        {
                            val route = "exercise_detail" +
                                "?dayId=${dayId.encodeForNav()}" +
                                "&exerciseId=${next.id.encodeForNav()}" +
                                "&nameEn=${next.nameEn.encodeForNav()}" +
                                "&nameEs=${next.name.encodeForNav()}" +
                                "&sets=${next.sets}" +
                                "&reps=${next.reps.encodeForNav()}" +
                                "&restSeconds=${next.restSeconds}" +
                                "&notes=${next.notes.encodeForNav()}"
                            navController.navigate(route) {
                                popUpTo("exercise_detail") { inclusive = true }
                            }
                        }
                    } else {
                        { navController.popBackStack() }
                    }
                } else null

                val isLastExercise = currentIndex >= 0 && currentIndex == exerciseCount - 1

                ExerciseDetailScreen(
                    dayId        = dayId,
                    exerciseId   = exerciseId,
                    nameEn       = args.getString("nameEn")      ?: "",
                    nameEs       = args.getString("nameEs")      ?: "",
                    sets         = args.getInt("sets"),
                    reps         = args.getString("reps")        ?: "",
                    restSeconds  = args.getInt("restSeconds"),
                    notes        = args.getString("notes")       ?: "",
                    onBack       = { navController.popBackStack() },
                    onNextAction = onNextAction,
                    isLastExercise = isLastExercise
                )
            }

            // ── Search ────────────────────────────────────────────────────
            composable(Routes.SEARCH) {
                SearchScreen(
                    onExerciseClick = { route ->
                        navController.navigate(route)
                    }
                )
            }

            // ── Profile ───────────────────────────────────────────────────
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onSignOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}