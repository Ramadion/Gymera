package com.DeBiaseRamiro.gymera.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import com.DeBiaseRamiro.gymera.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.res.painterResource
import com.DeBiaseRamiro.gymera.R

@Composable
fun LoginScreen(
    onNavigateToForm: () -> Unit,
    onNavigateToRoutine: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Cuando el login es exitoso, navegamos fuera de esta pantalla
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            // Por ahora siempre vamos al formulario (el usuario acaba de registrarse)
            // Más adelante verificaremos si ya tiene rutina en Firestore
            onNavigateToForm()
            viewModel.resetState()
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Logo y título
            Image(
                painter = painterResource(id = R.drawable.ic_gymera_logo),
                contentDescription = "Logo Gymera",
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = "Gymera",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )
            Text(
                text = "Tu entrenador personal con IA",
                fontSize = 16.sp,
                color = MutedGray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de Google Sign-In o indicador de carga
            when (uiState) {
                is LoginUiState.Loading -> {
                    CircularProgressIndicator(color = PurplePrimary)
                }
                is LoginUiState.Error -> {
                    Text(
                        text = (uiState as LoginUiState.Error).message,
                        color = RedError,
                        fontSize = 14.sp
                    )
                    GoogleSignInButton(onTokenReceived = { viewModel.signInWithGoogle(it) })
                }
                else -> {
                    GoogleSignInButton(onTokenReceived = { viewModel.signInWithGoogle(it) })
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(onTokenReceived: (String) -> Unit) {
    // El web_client_id lo vas a sacar de google-services.json
    // Busca la entrada con client_type: 3
    val webClientId = "684744802241-ad4bk511d0vupa833eigvvvmtvlu2u5b.apps.googleusercontent.com"

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)   // Pedimos el token para Firebase
        .requestEmail()
        .build()

    // El launcher maneja el resultado del intent de Google
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { onTokenReceived(it) }
        } catch (e: ApiException) {
            // El usuario canceló o hubo un error
        }
    }

    // Necesitamos el context para crear el GoogleSignInClient
    val context = androidx.compose.ui.platform.LocalContext.current

    Button(
        onClick = {
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            launcher.launch(googleSignInClient.signInIntent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
    ) {
        Text(
            text = "Iniciar sesión con Google",
            color = OnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}