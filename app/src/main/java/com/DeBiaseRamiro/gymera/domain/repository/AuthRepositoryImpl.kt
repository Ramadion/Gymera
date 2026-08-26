package com.DeBiaseRamiro.gymera.data.repository

import android.content.Context
import com.DeBiaseRamiro.gymera.domain.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        // Convertimos el token de Google en una credencial de Firebase
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        // Le pedimos a Firebase que autentique con esa credencial
        val result = firebaseAuth.signInWithCredential(credential).await()
        val user = result.user ?: return null

        // Guardamos/actualizamos el perfil en Firestore
        val userDoc = mapOf(
            "uid"         to user.uid,
            "displayName" to (user.displayName ?: ""),
            "email"       to (user.email ?: ""),
            "photoUrl"    to (user.photoUrl?.toString() ?: ""),
            "lastLogin"   to System.currentTimeMillis()
        )

        // merge = true para no pisar datos existentes (como hasActiveRoutine)
        firestore.collection("users")
            .document(user.uid)
            .set(userDoc, com.google.firebase.firestore.SetOptions.merge())
            .await()

        return user
    }

    override fun signOut() {
        firebaseAuth.signOut()

        // También desvinculamos la cuenta de Google Play Services. Sin esto,
        // el próximo login auto-selecciona la última cuenta usada y NUNCA se
        // muestra el selector de cuentas (impide cambiar de usuario).
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()
    }

    override fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
}