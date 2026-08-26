package com.DeBiaseRamiro.gymera.data.repository

import com.DeBiaseRamiro.gymera.data.local.entity.UserProfileEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

// ── Helpers compartidos por FormViewModel y ProfileViewModel ──────────────────
// Extraen la lógica que ambos ViewModels duplicaban en paralelo: construcción
// del UserProfileEntity, cálculo de edad desde fecha de nacimiento y sync del
// perfil físico a Firestore (merge, best-effort con log si falla).
//
// Reciben las dependencias por parámetro en lugar de inyectarlas para que los
// ViewModels conserven sus constructores y los tests existentes sigan operando
// sobre los mismos mocks de UserProfileDao/FirebaseFirestore.
// ─────────────────────────────────────────────────────────────────────────────

fun buildUserProfileEntity(
    uid: String,
    birthDateMillis: Long,
    weightKg: Float,
    heightCm: Int,
    gender: String
): UserProfileEntity = UserProfileEntity(
    uid             = uid,
    age             = calculateAge(birthDateMillis),
    weightKg        = weightKg,
    heightCm        = heightCm,
    gender          = gender,
    birthDateMillis = birthDateMillis
)

// Sync best-effort a Firestore — Room ya tiene los datos, así que un fallo acá
// solo se loguea (el usuario no nota nada y no interrumpe el flujo).
suspend fun syncProfileToFirestore(
    firestore: FirebaseFirestore,
    uid: String,
    age: Int,
    weightKg: Float,
    heightCm: Int,
    gender: String,
    birthDateMillis: Long
) {
    try {
        firestore.collection("users")
            .document(uid)
            .set(
                mapOf(
                    "age"             to age,
                    "weightKg"        to weightKg,
                    "heightCm"        to heightCm,
                    "gender"          to gender,
                    "birthDateMillis" to birthDateMillis
                ),
                SetOptions.merge()
            )
            .await()
    } catch (e: Exception) {
        android.util.Log.w("GYM_PROFILE", "Sync perfil falló: ${e.message}")
    }
}

// Edad exacta desde la fecha de nacimiento (compartida por ambos ViewModels).
fun calculateAge(birthDateMillis: Long): Int {
    if (birthDateMillis <= 0L) return 0
    val birthDate = Instant.ofEpochMilli(birthDateMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return Period.between(birthDate, LocalDate.now()).years
}
