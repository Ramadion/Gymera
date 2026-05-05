package com.DeBiaseRamiro.gymera.ui.screens.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.ui.theme.*

@Composable
fun FormScreen(
    onFormCompleted: (UserProfile) -> Unit,
    viewModel: FormViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()


    LaunchedEffect(currentStep) {
        if (currentStep >= viewModel.totalSteps) {
            onFormCompleted(userProfile)       // ← lo pasamos completo
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Barra de progreso arriba
            ProgressBar(currentStep = currentStep, totalSteps = viewModel.totalSteps)

            Spacer(modifier = Modifier.height(32.dp))

            // Contenido de cada paso
            when (currentStep) {
                0 -> StepGoal(onAnswer = { viewModel.setGoal(it) })
                1 -> StepDays(onAnswer = { viewModel.setDaysPerWeek(it) })
                2 -> StepDuration(onAnswer = { viewModel.setSessionDuration(it) })
                3 -> StepLevel(onAnswer = { viewModel.setLevel(it) })
                4 -> StepLimitations(onAnswer = { viewModel.setLimitations(it) })
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón de volver (excepto en el primer paso)
            if (currentStep > 0) {
                TextButton(
                    onClick = { viewModel.previousStep() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "← Volver", color = MutedGray)
                }
            }
        }
    }
}

// --- Barra de progreso ---

@Composable
fun ProgressBar(currentStep: Int, totalSteps: Int) {
    Column {
        Text(
            text = "Paso ${currentStep + 1} de $totalSteps",
            color = MutedGray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = PurplePrimary,
            trackColor = SurfaceVariant
        )
    }
}

// --- Componente reutilizable para cada paso ---

@Composable
fun StepContainer(
    question: String,
    options: List<Pair<String, () -> Unit>>  // Texto del botón + acción
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = question,
            color = OnBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        options.forEach { (label, onClick) ->
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
            ) {
                Text(text = label, color = OnBackground, fontSize = 16.sp)
            }
        }
    }
}

// --- Pasos individuales ---

@Composable
fun StepGoal(onAnswer: (String) -> Unit) {
    StepContainer(
        question = "¿Cuál es tu objetivo?",
        options = listOf(
            "Pérdida de peso"    to { onAnswer("WEIGHT_LOSS") },
            "Ganancia muscular"  to { onAnswer("MUSCLE_GAIN") },
            "Resistencia"        to { onAnswer("ENDURANCE") },
            "Tonificación"       to { onAnswer("TONING") }
        )
    )
}

@Composable
fun StepDays(onAnswer: (Int) -> Unit) {
    StepContainer(
        question = "¿Cuántos días por semana podés entrenar?",
        options = listOf(
            "3 días" to { onAnswer(3) },
            "4 días" to { onAnswer(4) },
            "5 días" to { onAnswer(5) },
            "6 días" to { onAnswer(6) }
        )
    )
}

@Composable
fun StepDuration(onAnswer: (Int) -> Unit) {
    StepContainer(
        question = "¿Cuánto tiempo tenés por sesión?",
        options = listOf(
            "30 minutos" to { onAnswer(30) },
            "45 minutos" to { onAnswer(45) },
            "60 minutos" to { onAnswer(60) },
            "90 minutos" to { onAnswer(90) }
        )
    )
}

@Composable
fun StepLevel(onAnswer: (String) -> Unit) {
    StepContainer(
        question = "¿Cuál es tu nivel de experiencia?",
        options = listOf(
            "Principiante" to { onAnswer("BEGINNER") },
            "Intermedio"   to { onAnswer("INTERMEDIATE") },
            "Avanzado"     to { onAnswer("ADVANCED") }
        )
    )
}

@Composable
fun StepLimitations(onAnswer: (String) -> Unit) {
    // Este paso es texto libre, no opciones
    var text by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "¿Tenés alguna lesión o limitación física?",
            color = OnBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
        Text(
            text = "Si no tenés ninguna, dejá el campo vacío.",
            color = MutedGray,
            fontSize = 14.sp
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Ej: dolor de rodilla, hernia de disco...", color = MutedGray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurplePrimary,
                unfocusedBorderColor = SurfaceVariant,
                focusedTextColor = OnBackground,
                unfocusedTextColor = OnBackground
            )
        )
        Button(
            onClick = { onAnswer(text) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
        ) {
            Text(text = "Crear mi rutina con IA 💪", color = OnBackground, fontSize = 16.sp)
        }
    }
}