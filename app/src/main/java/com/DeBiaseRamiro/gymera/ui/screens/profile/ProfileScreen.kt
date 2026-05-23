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
import com.DeBiaseRamiro.gymera.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user            = FirebaseAuth.getInstance().currentUser
    val physicalProfile by viewModel.physicalProfile.collectAsState()
    val saveState       by viewModel.saveState.collectAsState()

    var isEditing         by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDatePicker    by remember { mutableStateOf(false) }

    // Campos del modo edición
    var selectedDateText   by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(0L) }
    var weightInput        by remember { mutableStateOf("") }
    var heightInput        by remember { mutableStateOf("") }
    var genderSelected     by remember { mutableStateOf("No especificado") }

    // DatePickerState — rango: entre 100 y 10 años atrás
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = physicalProfile?.birthDateMillis?.takeIf { it > 0L },
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
                    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    selectedDateText   = fmt.format(Date(profile.birthDateMillis))
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
                    FirebaseAuth.getInstance().signOut()
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
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            selectedDateText   = fmt.format(Date(millis))
                            selectedDateMillis = millis
                        }
                        showDatePicker = false
                    }
                ) { Text("Confirmar", color = PurplePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = MutedGray)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = SurfaceDark)
        ) {
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = PurplePrimary,
                    todayDateBorderColor      = PurplePrimary,
                    containerColor            = SurfaceDark
                )
            )
        }
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

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = SurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            // ── Header de datos físicos ───────────────────────────────────
            Row(
                modifier                  = Modifier.fillMaxWidth(),
                horizontalArrangement     = Arrangement.SpaceBetween,
                verticalAlignment         = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Datos físicos",
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
                            text     = "Completá tu perfil para mejores rutinas",
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
                                    text       = "${viewModel.calculateAge(selectedDateMillis)} años",
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

                // Peso
                OutlinedTextField(
                    value         = weightInput,
                    onValueChange = { if (it.length <= 5) weightInput = it },
                    label         = { Text("Peso", color = MutedGray) },
                    suffix        = { Text("kg", color = MutedGray) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape  = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = PurplePrimary,
                        unfocusedBorderColor    = SurfaceVariant,
                        focusedTextColor        = OnBackground,
                        unfocusedTextColor      = OnBackground,
                        cursorColor             = PurplePrimary,
                        focusedContainerColor   = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Altura
                OutlinedTextField(
                    value         = heightInput,
                    onValueChange = { if (it.length <= 3) heightInput = it },
                    label         = { Text("Altura", color = MutedGray) },
                    suffix        = { Text("cm", color = MutedGray) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape  = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = PurplePrimary,
                        unfocusedBorderColor    = SurfaceVariant,
                        focusedTextColor        = OnBackground,
                        unfocusedTextColor      = OnBackground,
                        cursorColor             = PurplePrimary,
                        focusedContainerColor   = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
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

                // IMC — solo si tenemos peso y altura
                val imc = calcularIMC(
                    physicalProfile?.weightKg ?: 0f,
                    physicalProfile?.heightCm ?: 0
                )
                if (imc != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("IMC", fontSize = 12.sp, color = MutedGray)
                                Text(
                                    text       = "%.1f".format(imc),
                                    fontSize   = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = OnBackground
                                )
                            }
                            Text(
                                text       = categoriaIMC(imc),
                                fontSize   = 13.sp,
                                color      = colorIMC(imc),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

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
    else        -> androidx.compose.ui.graphics.Color(0xFFE57373)
}