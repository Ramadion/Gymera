package com.DeBiaseRamiro.gymera.ui.screens.search

import com.DeBiaseRamiro.gymera.data.remote.dto.FreeExerciseDto
import java.text.Normalizer

// ── BÚSQUEDA EN ESPAÑOL ─────────────────────────────────────────────────────
// Ayudantes para que la barra de búsqueda entienda palabras clave en español
// además de los nombres en inglés (el asset solo trae nombres en inglés).
// No tocan los datos: solo amplían la lógica de filtrado en memoria.

// Mapas de español/sinónimos → término en inglés que usa el asset.
// Las claves van NORMALIZADAS (minúsculas y sin acentos: "biceps", "cuadriceps").
private val MUSCLE_KEYWORDS: Map<String, List<String>> = mapOf(
    "abdominal" to listOf("abdominals"),
    "abdominales" to listOf("abdominals"),
    "abs" to listOf("abdominals"),
    "abdomen" to listOf("abdominals"),
    "abductor" to listOf("abductors"),
    "abductores" to listOf("abductors"),
    "adductor" to listOf("adductors"),
    "aductores" to listOf("adductors"),
    "biceps" to listOf("biceps"),
    "bicep" to listOf("biceps"),
    "gemelo" to listOf("calves"),
    "gemelos" to listOf("calves"),
    "pantorrilla" to listOf("calves"),
    "pantorrillas" to listOf("calves"),
    "pecho" to listOf("chest"),
    "pectoral" to listOf("chest"),
    "pectorales" to listOf("chest"),
    "torax" to listOf("chest"),
    "antebrazo" to listOf("forearms"),
    "antebrazos" to listOf("forearms"),
    "gluteo" to listOf("glutes"),
    "gluteos" to listOf("glutes"),
    "isquio" to listOf("hamstrings"),
    "isquios" to listOf("hamstrings"),
    "isquiotibial" to listOf("hamstrings"),
    "isquiotibiales" to listOf("hamstrings"),
    "femoral" to listOf("hamstrings"),
    "femorales" to listOf("hamstrings"),
    "cadera" to listOf("hip flexors"),
    "caderas" to listOf("hip flexors"),
    "flexores de cadera" to listOf("hip flexors"),
    "banda iliotibial" to listOf("it band"),
    "dorsal" to listOf("lats", "upper back", "middle back"),
    "dorsales" to listOf("lats", "upper back", "middle back"),
    "lat" to listOf("lats", "upper back", "middle back"),
    "lats" to listOf("lats", "upper back", "middle back"),
    "espalda" to listOf("lats", "upper back", "middle back", "traps"),
    "espalda ancha" to listOf("lats", "upper back", "middle back", "traps"),
    "espalda alta" to listOf("traps", "upper back"),
    "lumbar" to listOf("lower back"),
    "lumbares" to listOf("lower back"),
    "espalda baja" to listOf("lower back"),
    "espalda media" to listOf("middle back"),
    "cuello" to listOf("neck"),
    "cuadriceps" to listOf("quadriceps"),
    "cuadricep" to listOf("quadriceps"),
    "muslo" to listOf("quadriceps"),
    "muslos" to listOf("quadriceps"),
    "quads" to listOf("quadriceps"),
    "hombro" to listOf("shoulders"),
    "hombros" to listOf("shoulders"),
    "deltoide" to listOf("shoulders"),
    "deltoides" to listOf("shoulders"),
    "triceps" to listOf("triceps"),
    "tricep" to listOf("triceps"),
    "trapecio" to listOf("traps"),
    "trapecios" to listOf("traps"),
    "traps" to listOf("traps"),
    "soleo" to listOf("soleus")
)

private val EQUIPMENT_KEYWORDS: Map<String, List<String>> = mapOf(
    "sin equipamiento" to listOf("body only"),
    "solo cuerpo" to listOf("body only"),
    "peso corporal" to listOf("body only"),
    "bodyweight" to listOf("body only"),
    "cuerpo" to listOf("body only"),
    "libre" to listOf("body only"),
    "barra" to listOf("barbell"),
    "barras" to listOf("barbell"),
    "barra libre" to listOf("barbell"),
    "barra olimpica" to listOf("barbell"),
    "mancuerna" to listOf("dumbbell"),
    "mancuernas" to listOf("dumbbell"),
    "cable" to listOf("cable"),
    "polea" to listOf("cable"),
    "poleas" to listOf("cable"),
    "maquina" to listOf("machine"),
    "aparato" to listOf("machine"),
    "kettlebell" to listOf("kettlebells"),
    "kettlebells" to listOf("kettlebells"),
    "pesa rusa" to listOf("kettlebells"),
    "pesas rusas" to listOf("kettlebells"),
    "banda" to listOf("bands"),
    "bandas" to listOf("bands"),
    "bandas elasticas" to listOf("bands"),
    "elastico" to listOf("bands"),
    "elasticos" to listOf("bands"),
    "goma" to listOf("bands"),
    "gomas" to listOf("bands"),
    "balon" to listOf("medicine ball", "exercise ball"),
    "balones" to listOf("medicine ball", "exercise ball"),
    "balon medicinal" to listOf("medicine ball"),
    "pelota" to listOf("medicine ball", "exercise ball"),
    "pelotas" to listOf("medicine ball", "exercise ball"),
    "pelota de ejercicio" to listOf("exercise ball"),
    "rodillo" to listOf("foam roll"),
    "rodillo de espuma" to listOf("foam roll"),
    "espuma" to listOf("foam roll"),
    "foam" to listOf("foam roll"),
    "foam roller" to listOf("foam roll"),
    "ez" to listOf("e-z curl bar"),
    "barra ez" to listOf("e-z curl bar"),
    "barra z" to listOf("e-z curl bar"),
    "dominada" to listOf("pullup bar"),
    "dominadas" to listOf("pullup bar"),
    "barra de dominadas" to listOf("pullup bar"),
    "pullup" to listOf("pullup bar"),
    "pull up" to listOf("pullup bar"),
    "otro" to listOf("other"),
    "otros" to listOf("other")
)

/** Resultado del análisis del query: grupos musculares y equipos en inglés. */
data class SearchKeywords(
    val muscles: List<String>,
    val equipment: List<String>
)

/** Normaliza un texto para comparar: minúsculas, sin acentos, espacios colapsados. */
fun normalizeForSearch(text: String): String {
    val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return decomposed
        .lowercase()
        .trim()
        .replace(Regex("\\s+"), " ")
}

/** Analiza el query y devuelve los términos en inglés que representa. */
fun extractSearchKeywords(query: String): SearchKeywords {
    val muscles = mutableSetOf<String>()
    val equipment = mutableSetOf<String>()
    val q = normalizeForSearch(query)
    if (q.isEmpty()) return SearchKeywords(emptyList(), emptyList())

    for ((alias, targets) in MUSCLE_KEYWORDS) {
        if (aliasMatches(q, alias)) muscles.addAll(targets)
    }
    for ((alias, targets) in EQUIPMENT_KEYWORDS) {
        if (aliasMatches(q, alias)) equipment.addAll(targets)
    }
    return SearchKeywords(muscles.toList(), equipment.toList())
}

// Un alias matchea si está contenido en el query (ej: "ejercicio hombros" → "hombros")
// o si el query es un prefijo del alias (ej: "bar" → "barra"). Los queries de 1-2
// caracteres solo matchean cuando contienen el alias completo, para evitar ruido.
private fun aliasMatches(query: String, alias: String): Boolean =
    query.contains(alias) || (query.length >= 3 && alias.contains(query))

/**
 * Construye un predicado de filtrado para un query dado. El predicado devuelve
 * true si el ejercicio coincide por nombre en inglés, grupo muscular o equipo.
 */
fun buildSearchMatcher(query: String): (FreeExerciseDto) -> Boolean {
    val q = normalizeForSearch(query)
    val keywords = extractSearchKeywords(q)
    return { dto ->
        val nameMatches = q.isNotEmpty() && normalizeForSearch(dto.name).contains(q)
        val equipmentMatches = keywords.equipment.isNotEmpty() &&
            dto.equipment != null &&
            keywords.equipment.contains(normalizeForSearch(dto.equipment))
        val muscleMatches = keywords.muscles.isNotEmpty() &&
            dto.primaryMuscles.any { pm -> keywords.muscles.contains(pm.lowercase()) }
        nameMatches || equipmentMatches || muscleMatches
    }
}