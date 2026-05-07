package com.DeBiaseRamiro.gymera.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.DeBiaseRamiro.gymera.R
import com.DeBiaseRamiro.gymera.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    // Ya no recibe isUserLoggedIn — el ViewModel lo determina consultando Room
    onNavigateToLogin: () -> Unit,
    onNavigateToForm: () -> Unit,
    onNavigateToRoutine: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splash_alpha"
    )

    val destination by viewModel.destination.collectAsState()

    LaunchedEffect(Unit) { visible = true }

    // Observamos el destino — cuando el ViewModel termine de verificar Room,
    // esperamos al menos 2 segundos de splash y navegamos
    LaunchedEffect(destination) {
        if (destination !is SplashDestination.Loading) {
            delay(2000) // mínimo 2s de splash para que se vea la animación
            when (destination) {
                is SplashDestination.Login   -> onNavigateToLogin()
                is SplashDestination.Form    -> onNavigateToForm()
                is SplashDestination.Routine -> onNavigateToRoutine()
                else -> {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_gymera_logo),
                contentDescription = "Logo Gymera",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Gymera",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
                modifier = Modifier.alpha(alpha)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tu entrenador con IA",
                fontSize = 16.sp,
                color = MutedGray,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}