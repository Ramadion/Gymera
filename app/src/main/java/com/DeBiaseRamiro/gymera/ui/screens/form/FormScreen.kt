package com.DeBiaseRamiro.gymera.ui.screens.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.DeBiaseRamiro.gymera.domain.model.UserProfile
import com.DeBiaseRamiro.gymera.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    onFormCompleted: (UserProfile) -> Unit,
    viewModel: FormViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val userProfile  by viewModel.userProfile.collectAsState()
    val birthDateError by viewModel.birthDateError.collectAsState()
    val metricsError   by viewModel.metricsError.collectAsState()

    LaunchedEffect(currentStep) {
        if (currentStep >= viewModel.totalSteps) {
            onFormCompleted(userProfile)
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
            ProgressBar(currentStep = currentStep, totalSteps = viewModel.totalSteps)

            Spacer(modifier = Modifier.height(32.dp))

            // Pasos 0-2: datos físicos nuevos
            // Pasos 3-7: datos de rutina existentes (renumerados)
            when (currentStep) {
                0 -> StepGender(
                    onAnswer = { viewModel.setGender(it) }
                )
                1 -> StepBirthDate(
                    onAnswer      = { viewModel.setBirthDate(it) },
                    errorMessage  = birthDateError
                )
                2 -> StepBodyMetrics(
                    onAnswer     = { weight, height -> viewModel.setBodyMetrics(weight, height) },
                    errorMessage = metricsError
                )
                3 -> StepGoal(onAnswer        = { viewModel.setGoal(it) })
                4 -> StepDays(onAnswer        = { viewModel.setDaysPerWeek(it) })
                5 -> StepDuration(onAnswer    = { viewModel.setSessionDuration(it) })
                6 -> StepLevel(onAnswer       = { viewModel.setLevel(it) })
                7 -> StepLimitations(onAnswer = { viewModel.setLimitations(it) })
            }

            Spacer(modifier = Modifier.weight(1f))

            if (currentStep > 0) {
                TextButton(
                    onClick = { viewModel.previousStep() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("← Volver", color = MutedGray)
                }
            }
        }
    }
}

// ── Barra de progreso ─────────────────────────────────────────────────────────

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
            modifier  = Modifier.fillMaxWidth(),
            color      = PurplePrimary,
            trackColor = SurfaceVariant
        )
    }
}

// ── Componente reutilizable de opciones ───────────────────────────────────────

@Composable
fun StepContainer(
    question: String,
    options: List<Pair<String, () -> Unit>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text       = question,
            color      = OnBackground,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        options.forEach { (label, onClick) ->
            Button(
                onClick  = onClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
            ) {
                Text(text = label, color = OnBackground, fontSize = 16.sp)
            }
        }
    }
}

// ── PASO 0 — Género ───────────────────────────────────────────────────────────

@Composable
fun StepGender(onAnswer: (String) -> Unit) {
    StepContainer(
        question = "¿Con qué género te identificás?",
        options  = listOf(
            "Masculino"       to { onAnswer("Masculino") },
            "Femenino"        to { onAnswer("Femenino") },
            "No especificado" to { onAnswer("No especificado") }
        )
    )
}

// ── PASO 1 — Fecha de nacimiento ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepBirthDate(
    onAnswer: (Long) -> Unit,
    errorMessage: String?
) {
    var showPicker        by remember { mutableStateOf(false) }
    var selectedDateText  by remember { mutableStateOf("") }
    var selectedMillis    by remember { mutableStateOf<Long?>(null) }

    // Rango seleccionable: entre 100 años atrás y 10 años atrás
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val now    = System.currentTimeMillis()
                val minAge = now - (100L * 365 * 24 * 60 * 60 * 1000)
                val maxAge = now - (10L  * 365 * 24 * 60 * 60 * 1000)
                return utcTimeMillis in minAge..maxAge
            }
        }
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text       = "¿Cuándo naciste?",
            color      = OnBackground,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
        Text(
            text     = "La usamos para calcular tu edad automáticamente cada año.",
            color    = MutedGray,
            fontSize = 14.sp
        )

        // Muestra la fecha seleccionada si ya eligió
        if (selectedDateText.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text     = "📅  $selectedDateText",
                    color    = OnBackground,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Botón para abrir el DatePicker
        OutlinedButton(
            onClick  = { showPicker = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = PurplePrimary),
            border   = androidx.compose.foundation.BorderStroke(1.dp, PurplePrimary)
        ) {
            Text(
                text     = if (selectedDateText.isEmpty()) "Seleccionar fecha" else "Cambiar fecha",
                fontSize = 16.sp
            )
        }

        // Error de validación
        if (errorMessage != null) {
            Text(
                text     = errorMessage,
                color    = androidx.compose.ui.graphics.Color(0xFFE57373),
                fontSize = 13.sp
            )
        }

        // Botón continuar — solo visible si eligió una fecha válida
        if (selectedMillis != null && errorMessage == null) {
            Button(
                onClick  = { selectedMillis?.let { onAnswer(it) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Text("Continuar →", color = OnBackground, fontSize = 16.sp)
            }
        }
    }

    // DatePickerDialog
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            selectedDateText = fmt.format(Date(millis))
                            selectedMillis   = millis
                        }
                        showPicker = false
                    }
                ) { Text("Confirmar", color = PurplePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = SurfaceDark)
        ) {
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor   = PurplePrimary,
                    todayDateBorderColor         = PurplePrimary,
                    containerColor               = SurfaceDark
                )
            )
        }
    }
}

// ── PASO 2 — Peso y Altura ────────────────────────────────────────────────────

@Composable
fun StepBodyMetrics(
    onAnswer: (Float, Int) -> Boolean,
    errorMessage: String?
) {
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text       = "¿Cuánto pesás y medís?",
            color      = OnBackground,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
        Text(
            text     = "Estos datos nos permiten personalizar mejor tu rutina.",
            color    = MutedGray,
            fontSize = 14.sp
        )

        // Campo peso
        OutlinedTextField(
            value         = weightInput,
            onValueChange = { if (it.length <= 5) weightInput = it },
            label         = { Text("Peso", color = MutedGray) },
            suffix        = { Text("kg", color = MutedGray) },
            placeholder   = { Text("Ej: 75", color = MutedGray) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape  = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = PurplePrimary,
                unfocusedBorderColor = SurfaceVariant,
                focusedTextColor     = OnBackground,
                unfocusedTextColor   = OnBackground,
                cursorColor          = PurplePrimary,
                focusedContainerColor   = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            )
        )

        // Campo altura
        OutlinedTextField(
            value         = heightInput,
            onValueChange = { if (it.length <= 3) heightInput = it },
            label         = { Text("Altura", color = MutedGray) },
            suffix        = { Text("cm", color = MutedGray) },
            placeholder   = { Text("Ej: 175", color = MutedGray) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape  = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = PurplePrimary,
                unfocusedBorderColor = SurfaceVariant,
                focusedTextColor     = OnBackground,
                unfocusedTextColor   = OnBackground,
                cursorColor          = PurplePrimary,
                focusedContainerColor   = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            )
        )

        // Mensaje de error con validación
        if (errorMessage != null) {
            errorMessage.split("\n").forEach { line ->
                Text(
                    text     = "⚠ $line",
                    color    = androidx.compose.ui.graphics.Color(0xFFE57373),
                    fontSize = 13.sp
                )
            }
        }

        // Botón continuar
        Button(
            onClick = {
                onAnswer(
                    weightInput.replace(",", ".").toFloatOrNull() ?: 0f,
                    heightInput.toIntOrNull() ?: 0
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            enabled  = weightInput.isNotBlank() && heightInput.isNotBlank()
        ) {
            Text("Continuar →", color = OnBackground, fontSize = 16.sp)
        }
    }
}

// ── Pasos existentes (sin cambios) ───────────────────────────────────────────

@Composable
fun StepGoal(onAnswer: (String) -> Unit) {
    StepContainer(
        question = "¿Cuál es tu objetivo?",
        options  = listOf(
            "Pérdida de peso"   to { onAnswer("WEIGHT_LOSS") },
            "Ganancia muscular" to { onAnswer("MUSCLE_GAIN") },
            "Resistencia"       to { onAnswer("ENDURANCE") },
            "Tonificación"      to { onAnswer("TONING") }
        )
    )
}

@Composable
fun StepDays(onAnswer: (Int) -> Unit) {
    StepContainer(
        question = "¿Cuántos días por semana podés entrenar?",
        options  = listOf(
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
        options  = listOf(
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
        options  = listOf(
            "Principiante" to { onAnswer("BEGINNER") },
            "Intermedio"   to { onAnswer("INTERMEDIATE") },
            "Avanzado"     to { onAnswer("ADVANCED") }
        )
    )
}

@Composable
fun StepLimitations(onAnswer: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text       = "¿Tenés alguna lesión o limitación física?",
            color      = OnBackground,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        )
        Text(
            text     = "Si no tenés ninguna, dejá el campo vacío.",
            color    = MutedGray,
            fontSize = 14.sp
        )
        OutlinedTextField(
            value         = text,
            onValueChange = { text = it },
            placeholder   = { Text("Ej: dolor de rodilla, hernia de disco...", color = MutedGray) },
            modifier      = Modifier.fillMaxWidth(),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = PurplePrimary,
                unfocusedBorderColor = SurfaceVariant,
                focusedTextColor     = OnBackground,
                unfocusedTextColor   = OnBackground
            )
        )
        Button(
            onClick  = { onAnswer(text) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
        ) {
            Text("Crear mi rutina con IA 💪", color = OnBackground, fontSize = 16.sp)
        }
    }
}