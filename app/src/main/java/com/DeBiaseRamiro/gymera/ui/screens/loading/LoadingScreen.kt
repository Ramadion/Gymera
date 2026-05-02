package com.DeBiaseRamiro.gymera.ui.screens.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.domain.model.Routine
import com.DeBiaseRamiro.gymera.ui.theme.*

@Composable
fun LoadingScreen(
    userProfile: UserProfile,
    onRoutineGenerated: (Routine) -> Unit,
    onError: () -> Unit,
    viewModel: LoadingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Arrancamos la generación al entrar a la pantalla
    LaunchedEffect(Unit) {
        viewModel.generateRoutine(userProfile)
    }

    // Reaccionamos a los cambios de estado
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoadingUiState.Success -> onRoutineGenerated(state.routine)
            is LoadingUiState.Error   -> onError()
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is LoadingUiState.Loading -> LoadingContent()
            is LoadingUiState.Error   -> ErrorContent(
                message = state.message,
                onRetry = { viewModel.generateRoutine(userProfile) }
            )
            else -> {}
        }
    }
}

@Composable
fun LoadingContent() {
    // Animación de rotación para el ícono
    val rotation by rememberInfiniteTransition(label = "rotation")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing)
            ),
            label = "rotate"
        )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "💪",
            fontSize = 64.sp,
            modifier = Modifier.rotate(rotation)
        )
        Text(
            text = "Creando tu rutina personalizada...",
            color = OnBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "La IA está analizando tu perfil y\ngenerando el plan ideal para vos.",
            color = MutedGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = PurplePrimary,
            trackColor = SurfaceVariant
        )
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(text = "❌", fontSize = 48.sp)
        Text(
            text = "Algo salió mal",
            color = OnBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            color = RedError,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
        ) {
            Text(text = "Reintentar", color = OnBackground)
        }
    }
}