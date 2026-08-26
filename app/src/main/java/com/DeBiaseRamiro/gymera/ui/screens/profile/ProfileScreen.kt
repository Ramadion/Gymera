package com.DeBiaseRamiro.gymera.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.firebase.auth.FirebaseAuth
import com.DeBiaseRamiro.gymera.data.repository.calculateAge
import com.DeBiaseRamiro.gymera.ui.components.GymeraDatePickerDialog
import com.DeBiaseRamiro.gymera.ui.components.WeightHeightFields
import com.DeBiaseRamiro.gymera.ui.components.formatDateShort
import com.DeBiaseRamiro.gymera.ui.theme.*
import com.DeBiaseRamiro.gymera.ui.shared.SharedRoutineViewModel
import com.DeBiaseRamiro.gymera.domain.model.Routine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user            = FirebaseAuth.getInstance().currentUser
    val physicalProfile by viewModel.physicalProfile.collectAsState()
    val saveState       by viewModel.saveState.collectAsState()
    val sharedRoutineViewModel: SharedRoutineViewModel = hiltViewModel()
    val currentRoutine by sharedRoutineViewModel.currentRoutine.collectAsState()

    var isEditing         by rememberSaveable { mutableStateOf(false) }
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker    by rememberSaveable { mutableStateOf(false) }

    // Campos del modo edición
    var selectedDateText   by rememberSaveable { mutableStateOf("") }
    var selectedDateMillis by rememberSaveable { mutableStateOf(0L) }
    var weightInput        by rememberSaveable { mutableStateOf("") }
    var heightInput        by rememberSaveable { mutableStateOf("") }
    var genderSelected     by rememberSaveable { mutableStateOf("No especificado") }

    // DatePickerState no es Saveable — se recrea al rotar inicializado con la
    // última fecha confirmada (o la de Room si aún no se editó).
    // Rango: entre 100 y 10 años atrás
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis.takeIf { it > 0L }
            ?: physicalProfile?.birthDateMillis?.takeIf { it > 0L },
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val now    = System.currentTimeMillis()
                val minAge = now - (100L * 365 * 24 * 60 * 60 * 1000)
                val maxAge = now - (10L  * 365 * 24 * 60 * 60 * 1000)
                return utcTimeMillis in minAge..maxAge
            }
        }
    )

    // Inicializamos los campos cuando llegan los datos de Room
    LaunchedEffect(physicalProfile) {
        physicalProfile?.let { profile ->
            if (!isEditing) {
                weightInput    = if (profile.weightKg > 0) profile.weightKg.toInt().toString() else ""
                heightInput    = if (profile.heightCm > 0) profile.heightCm.toString() else ""
                genderSelected = profile.gender.ifBlank { "No especificado" }
                if (profile.birthDateMillis > 0L) {
                    selectedDateText   = formatDateShort(profile.birthDateMillis)
                    selectedDateMillis = profile.birthDateMillis
                }
            }
        }
    }

    // Cuando se guarda correctamente, salimos del modo edición
    LaunchedEffect(saveState) {
        if (saveState is SaveState.Saved) {
            isEditing = false
            viewModel.resetSaveState()
        }
    }

    // ── Diálogo de confirmación de cierre de sesión ───────────────────────
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Cerrar sesión", color = OnBackground, fontWeight = FontWeight.Bold) },
            text  = { Text("¿Estás seguro que querés cerrar sesión?", color = MutedGray) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    viewModel.signOut()
                    onSignOut()
                }) { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── DatePickerDialog ──────────────────────────────────────────────────
    if (showDatePicker) {
        GymeraDatePickerDialog(
            state     = datePickerState,
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                selectedDateText   = formatDateShort(millis)
                selectedDateMillis = millis
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Perfil", fontWeight = FontWeight.Bold, color = OnBackground)
                },
                actions = {
                    if (isEditing) {
                        // Botón guardar — muestra spinner mientras guarda
                        IconButton(
                            onClick = {
                                viewModel.savePhysicalProfile(
                                    birthDateMillis = selectedDateMillis,
                                    weightKg        = weightInput.replace(",", ".").toFloatOrNull() ?: 0f,
                                    heightCm        = heightInput.toIntOrNull() ?: 0,
                                    gender          = genderSelected
                                )
                            },
                            enabled = saveState !is SaveState.Saving
                        ) {
                            if (saveState is SaveState.Saving) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(20.dp),
                                    color       = PurplePrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector        = Icons.Default.Check,
                                    contentDescription = "Guardar",
                                    tint               = PurplePrimary
                                )
                            }
                        }
                    } else {
                        // Botón editar
                        IconButton(onClick = { isEditing = true }) {
                            Icon(
                                imageVector        = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint               = PurplePrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // ── Avatar ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val photoUrl = user?.photoUrl?.toString()
                if (photoUrl != null) {
                    GlideImage(
                        model              = photoUrl,
                        contentDescription = "Foto de perfil",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier           = Modifier.size(40.dp),
                        tint               = MutedGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text       = user?.displayName ?: "Usuario",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = OnBackground
            )
            Text(
                text     = user?.email ?: "",
                fontSize = 14.sp,
                color    = MutedGray
            )


            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = SurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            // ── Header de datos físicos ───────────────────────────────────
            Row(
                modifier                  = Modifier.fillMaxWidth(),
                horizontalArrangement     = Arrangement.SpaceBetween,
                verticalAlignment         = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Estado físico",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = OnBackground
                )
                if (!isEditing) {
                    val hasData = physicalProfile?.let {
                        it.age > 0 || it.weightKg > 0 || it.heightCm > 0
                    } ?: false
                    if (!hasData) {
                        Text(
                            text     = "Completá tu perfil para mejores sugerencias",
                            fontSize = 11.sp,
                            color    = PurplePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {

                // ── MODO EDICIÓN ──────────────────────────────────────────

                // Fecha de nacimiento
                Text(
                    text     = "Fecha de nacimiento",
                    fontSize = 13.sp,
                    color    = MutedGray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedDateText.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(containerColor = SurfaceVariant),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = "📅  $selectedDateText",
                                color    = OnBackground,
                                fontSize = 15.sp
                            )
                            if (selectedDateMillis > 0L) {
                                Text(
                                    text       = "${calculateAge(selectedDateMillis)} años",
                                    color      = PurplePrimary,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick  = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = PurplePrimary),
                    border   = BorderStroke(1.dp, SurfaceVariant)
                ) {
                    Text(
                        text     = if (selectedDateText.isEmpty()) "Seleccionar fecha de nacimiento"
                        else "Cambiar fecha",
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Peso y altura (componente compartido con FormScreen)
                WeightHeightFields(
                    weightInput    = weightInput,
                    heightInput    = heightInput,
                    onWeightChange = { weightInput = it },
                    onHeightChange = { heightInput = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Género
                Text(
                    text     = "Género",
                    fontSize = 13.sp,
                    color    = MutedGray,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Masculino", "Femenino", "No especificado").forEach { option ->
                        FilterChip(
                            selected = genderSelected == option,
                            onClick  = { genderSelected = option },
                            label    = { Text(option, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurplePrimary.copy(alpha = 0.2f),
                                selectedLabelColor     = PurplePrimary
                            )
                        )
                    }
                }

                // Botón cancelar edición
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick  = { isEditing = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancelar", color = MutedGray, fontSize = 14.sp)
                }

            } else {

                // ── MODO LECTURA ──────────────────────────────────────────

                val imc = calcularIMC(
                    physicalProfile?.weightKg ?: 0f,
                    physicalProfile?.heightCm ?: 0
                )

                ProfileStateSummaryCard(
                    imc = imc,
                    age = physicalProfile?.age ?: 0,
                    weightKg = physicalProfile?.weightKg ?: 0f,
                    heightCm = physicalProfile?.heightCm ?: 0,
                    goal = currentRoutine?.goal.orEmpty(),
                    level = currentRoutine?.level.orEmpty(),
                    daysPerWeek = currentRoutine?.daysPerWeek ?: 0
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label    = "Edad",
                        value    = if ((physicalProfile?.age ?: 0) > 0)
                            "${physicalProfile?.age} años" else "—"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label    = "Peso",
                        value    = if ((physicalProfile?.weightKg ?: 0f) > 0)
                            "${physicalProfile?.weightKg?.toInt()} kg" else "—"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label    = "Altura",
                        value    = if ((physicalProfile?.heightCm ?: 0) > 0)
                            "${physicalProfile?.heightCm} cm" else "—"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val focusLabel = currentRoutine?.goal?.takeIf { it.isNotBlank() } ?: "Rutina personalizada"
                val recommendationBase = buildLoadRecommendationBase(
                    weightKg = physicalProfile?.weightKg ?: 0f,
                    heightCm = physicalProfile?.heightCm ?: 0,
                    goal = currentRoutine?.goal.orEmpty(),
                    level = currentRoutine?.level.orEmpty()
                )

                RecommendationOverviewCard(
                    focusLabel = focusLabel,
                    currentRoutine = currentRoutine,
                    recommendationBase = recommendationBase
                )

                Spacer(modifier = Modifier.height(12.dp))

                LoadRecommendationSection(
                    recommendations = recommendationBase
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Fecha de nacimiento — solo si está cargada
                if (selectedDateText.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Nacimiento", fontSize = 13.sp, color = MutedGray)
                            Text(
                                text       = selectedDateText,
                                fontSize   = 13.sp,
                                color      = OnBackground,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Género — solo si está cargado y no es "No especificado"
                if (physicalProfile?.gender?.isNotBlank() == true &&
                    physicalProfile?.gender != "No especificado") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Género", fontSize = 13.sp, color = MutedGray)
                            Text(
                                text       = physicalProfile?.gender ?: "",
                                fontSize   = 13.sp,
                                color      = OnBackground,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = SurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            // ── Cerrar sesión ─────────────────────────────────────────────
            Button(
                onClick  = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
private fun ProfileTagRow(tags: List<String>) {
    if (tags.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.take(3).forEach { tag ->
            Surface(
                shape = RoundedCornerShape(50),
                color = SurfaceDark
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    color = OnBackground,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProfileOverviewCard(
    age: Int,
    weightKg: Float,
    heightCm: Int,
    gender: String,
    focus: String,
    level: String,
    daysPerWeek: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape    = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resumen rápido",
                        fontSize = 13.sp,
                        color = MutedGray
                    )
                    Text(
                        text = if (focus.isNotBlank()) focus else "Sin rutina activa",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                    Text(
                        text = buildString {
                            append(if (level.isNotBlank()) level else "Nivel no definido")
                            if (daysPerWeek > 0) append(" • $daysPerWeek días/semana")
                        },
                        fontSize = 12.sp,
                        color = MutedGray
                    )
                }

                Surface(
                    color = PurplePrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (gender.isNotBlank() && gender != "No especificado") gender else "Perfil base",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        color = PurplePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Edad",
                    value = if (age > 0) "$age años" else "—"
                )
                ProfileMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Peso",
                    value = if (weightKg > 0f) "${weightKg.toInt()} kg" else "—"
                )
                ProfileMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Altura",
                    value = if (heightCm > 0) "$heightCm cm" else "—"
                )
            }
        }
    }
}

@Composable
private fun ProfileMetricPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        color = SurfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MutedGray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (value == "—") MutedGray else OnBackground
            )
        }
    }
}

@Composable
private fun ProfileStateSummaryCard(
    imc: Float?,
    age: Int,
    weightKg: Float,
    heightCm: Int,
    goal: String,
    level: String,
    daysPerWeek: Int
) {
    val statusLabel = imc?.let { categoriaIMC(it) } ?: "Sin IMC"
    val summaryText = when {
        imc == null && weightKg <= 0f && heightCm <= 0 -> "Completá peso y altura para obtener una lectura útil de tu estado físico."
        imc == null -> "Tu perfil ya tiene datos suficientes para armar sugerencias, pero el IMC todavía no puede calcularse."
        imc < 18.5f -> "Estás por debajo del rango normal. Conviene priorizar progresión suave y constancia."
        imc < 25f -> "Estás en un punto estable. Es una base buena para progresar sin apurar la carga."
        imc < 30f -> "Tenés margen para priorizar técnica y progresión gradual en lugar de subir peso de golpe."
        else -> "La prioridad acá es ajustar volumen, técnica y progresión de manera conservadora."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Estado físico",
                        fontSize = 13.sp,
                        color = MutedGray
                    )
                    Text(
                        text = statusLabel,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (imc != null) "%.1f".format(imc) else "—",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                    Text(
                        text = "IMC",
                        fontSize = 11.sp,
                        color = MutedGray
                    )
                }
            }

            Text(
                text = summaryText,
                fontSize = 13.sp,
                color = OnBackground
            )



            if (level.isNotBlank()) {
                Surface(
                    color = PurplePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Nivel actual: $level",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        color = PurplePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMiniInfo(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier,
        color = SurfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MutedGray
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                maxLines = 2
            )
        }
    }
}

private data class LoadRecommendation(
    val group: String,
    val suggestedRangeKg: String,
    val repRange: String,
    val note: String
)

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = MutedGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = value,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = if (value == "—") MutedGray else OnBackground
            )
        }
    }
}

// ── Funciones de IMC ──────────────────────────────────────────────────────────

private fun calcularIMC(weightKg: Float, heightCm: Int): Float? {
    if (weightKg <= 0 || heightCm <= 0) return null
    val heightM = heightCm / 100f
    return weightKg / (heightM * heightM)
}

private fun categoriaIMC(imc: Float): String = when {
    imc < 18.5f -> "Bajo peso"
    imc < 25f   -> "Peso normal"
    imc < 30f   -> "Sobrepeso"
    else        -> "Obesidad"
}

@Composable
private fun colorIMC(imc: Float) = when {
    imc < 18.5f -> androidx.compose.ui.graphics.Color(0xFF64B5F6)
    imc < 25f   -> androidx.compose.ui.graphics.Color(0xFF81C784)
    imc < 30f   -> androidx.compose.ui.graphics.Color(0xFFFFB74D)
    else        -> RedError
}

private fun buildLoadRecommendationBase(
    weightKg: Float,
    heightCm: Int,
    goal: String,
    level: String
): List<LoadRecommendation> {
    if (weightKg <= 0f || heightCm <= 0) {
        return emptyList()
    }

    val heightFactor = when {
        heightCm < 160 -> 0.92f
        heightCm < 175 -> 1.0f
        heightCm < 190 -> 1.08f
        else -> 1.15f
    }

    val goalFactor = when {
        goal.contains("fuerza", ignoreCase = true) -> 1.15f
        goal.contains("power", ignoreCase = true) -> 1.15f
        goal.contains("hipertrof", ignoreCase = true) -> 1.0f
        goal.contains("musculo", ignoreCase = true) -> 1.0f
        goal.contains("resistencia", ignoreCase = true) -> 0.85f
        goal.contains("defin", ignoreCase = true) -> 0.9f
        else -> 1.0f
    }

    val levelFactor = when {
        level.contains("princip", ignoreCase = true) -> 0.78f
        level.contains("beginner", ignoreCase = true) -> 0.78f
        level.contains("inter", ignoreCase = true) -> 1.0f
        level.contains("avanz", ignoreCase = true) -> 1.12f
        level.contains("advanced", ignoreCase = true) -> 1.12f
        else -> 1.0f
    }

    val multiplier = heightFactor * goalFactor * levelFactor

    fun suggest(
        group: String,
        percent: Float,
        repRange: String,
        note: String,
        useBodyWeight: Boolean = false
    ): LoadRecommendation {
        val baseKg = weightKg * percent * multiplier
        val suggestedRange = if (useBodyWeight) {
            "Peso corporal"
        } else {
            val minKg = (baseKg * 0.85f).roundToInt().coerceAtLeast(1)
            val maxKg = (baseKg * 1.15f).roundToInt().coerceAtLeast(minKg)
            if (minKg == maxKg) "~$minKg kg" else "~$minKg-$maxKg kg"
        }
        return LoadRecommendation(
            group = group,
            suggestedRangeKg = suggestedRange,
            repRange = repRange,
            note = note
        )
    }

    return listOf(
        suggest("Piernas", 0.50f, "8-12 reps", "Arranque prudente; sube de a poco si mantenés técnica."),
        suggest("Gluteos", 0.45f, "10-15 reps", "Suele tolerar más volumen, no apures la carga."),
        suggest("Espalda", 0.35f, "8-12 reps", "Priorizá control y recorrido completo."),
        suggest("Pecho", 0.28f, "8-12 reps", "Buscá un peso que no rompa la línea del movimiento."),
        suggest("Hombros", 0.16f, "10-15 reps", "Carga moderada, técnica estricta."),
        suggest("Biceps", 0.12f, "10-15 reps", "Mejor quedarse corto que balancear."),
        suggest("Triceps", 0.12f, "10-15 reps", "Suele responder bien a progresiones chicas."),
        suggest("Core", 0.00f, "12-20 reps", "En core priorizá peso corporal o carga mínima.", useBodyWeight = true)
    )
}

@Composable
private fun RecommendationOverviewCard(
    focusLabel: String,
    currentRoutine: Routine?,
    recommendationBase: List<LoadRecommendation>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Tu enfoque actual",
                fontSize = 13.sp,
                color = MutedGray
            )
            Text(
                text = focusLabel,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )

            val routineSummary = buildString {
                append(currentRoutine?.level?.takeIf { it.isNotBlank() } ?: "Rutina no activa")
                currentRoutine?.daysPerWeek?.takeIf { it > 0 }?.let {
                    append(" • $it días/semana")
                }
            }
            Text(
                text = routineSummary,
                fontSize = 13.sp,
                color = MutedGray
            )

            if (recommendationBase.isNotEmpty()) {
                Text(
                    text = "Las cargas de abajo son un punto de partida. Ajustalas para terminar cada serie con buena técnica y 1-3 reps en reserva.",
                    fontSize = 12.sp,
                    color = PurplePrimary
                )
            }
        }
    }
}

@Composable
private fun LoadRecommendationSection(
    recommendations: List<LoadRecommendation>
) {
    if (recommendations.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Cargas sugeridas por grupo muscular",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground
        )

        recommendations.forEach { rec ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = rec.group,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                        Text(
                            text = rec.note,
                            fontSize = 12.sp,
                            color = MutedGray,
                            maxLines = 2
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = rec.suggestedRangeKg,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                        Text(
                            text = rec.repRange,
                            fontSize = 11.sp,
                            color = MutedGray
                        )
                    }
                }
            }
        }
    }
}
