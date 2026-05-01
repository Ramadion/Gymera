package com.DeBiaseRamiro.gymera.domain.repository

import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    // Devuelve el usuario si el login fue exitoso, o null si falló
    suspend fun signInWithGoogle(idToken: String): FirebaseUser?

    // Cierra la sesión del usuario
    fun signOut()

    // Devuelve el usuario actual si hay sesión activa
    fun getCurrentUser(): FirebaseUser?
}