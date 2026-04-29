package com.DeBiaseRamiro.gymera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.DeBiaseRamiro.gymera.ui.navigation.NavGraph
import com.DeBiaseRamiro.gymera.ui.theme.GymeraTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// @AndroidEntryPoint le dice a Hilt que esta Activity puede recibir dependencias
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymeraTheme {
                NavGraph(
                    isUserLoggedIn = firebaseAuth.currentUser != null
                )
            }
        }
    }
}