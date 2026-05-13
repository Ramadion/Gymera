package com.DeBiaseRamiro.gymera.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit
) {
    // Leemos el usuario directamente de Firebase — no necesita ViewModel
    // porque es solo lectura y no tiene lógica de negocio
    val user = FirebaseAuth.getInstance().currentUser

    // Controla el diálogo de confirmación de cierre de sesión
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Diálogo de confirmación — igual al de "Generar nueva rutina"
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Cerrar sesión",
                    color = com.DeBiaseRamiro.gymera.ui.theme.OnBackground,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro que querés cerrar sesión?",
                    color = com.DeBiaseRamiro.gymera.ui.theme.MutedGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        FirebaseAuth.getInstance().signOut()
                        onSignOut()
                    }
                ) {
                    Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(
                        "Cancelar",
                        color = com.DeBiaseRamiro.gymera.ui.theme.MutedGray
                    )
                }
            },
            containerColor = com.DeBiaseRamiro.gymera.ui.theme.SurfaceDark
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Perfil",
                        fontWeight = FontWeight.Bold,
                        color = com.DeBiaseRamiro.gymera.ui.theme.OnBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.DeBiaseRamiro.gymera.ui.theme.BackgroundDark
                )
            )
        },
        containerColor = com.DeBiaseRamiro.gymera.ui.theme.BackgroundDark
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            // ── Avatar ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(com.DeBiaseRamiro.gymera.ui.theme.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val photoUrl = user?.photoUrl?.toString()
                if (photoUrl != null) {
                    GlideImage(
                        model = photoUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback si no hay foto de Google
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = com.DeBiaseRamiro.gymera.ui.theme.MutedGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Nombre ────────────────────────────────────────────────────
            Text(
                text = user?.displayName ?: "Usuario",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = com.DeBiaseRamiro.gymera.ui.theme.OnBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Email ─────────────────────────────────────────────────────
            Text(
                text = user?.email ?: "",
                fontSize = 14.sp,
                color = com.DeBiaseRamiro.gymera.ui.theme.MutedGray
            )

            Spacer(modifier = Modifier.height(40.dp))

            HorizontalDivider(
                color = com.DeBiaseRamiro.gymera.ui.theme.SurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Botón cerrar sesión ───────────────────────────────────────
            Button(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cerrar sesión",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}