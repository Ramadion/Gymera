package com.DeBiaseRamiro.gymera.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.DeBiaseRamiro.gymera.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    isUserLoggedIn: Boolean
) {
    // Controla si el logo está visible o no (para la animación)
    var visible by remember { mutableStateOf(false) }

    // Anima la opacidad del logo de 0 a 1
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splash_alpha"
    )

    // Al entrar a la pantalla: activa la animación y navega después de 2 segundos
    LaunchedEffect(Unit) {
        visible = true
        delay(2000)
        if (isUserLoggedIn) {
            onNavigateToHome()
        } else {
            onNavigateToLogin()
        }
    }

    // UI
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
            Text(
                text = "💪",
                fontSize = 72.sp,
                modifier = Modifier.alpha(alpha)
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