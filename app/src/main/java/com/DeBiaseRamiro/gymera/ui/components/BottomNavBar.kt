package com.DeBiaseRamiro.gymera.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.DeBiaseRamiro.gymera.ui.theme.*

/**
 * Representa cada ítem de la barra de navegación inferior.
 *
 * @param route     La ruta de navegación asociada (debe coincidir con Routes.kt)
 * @param label     Texto que aparece debajo del ícono
 * @param icon      Ícono del ítem
 */
sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Routine : BottomNavItem(
        route = "routine",
        label = "Mi Rutina",
        icon = Icons.Default.CalendarMonth
    )
    object Search : BottomNavItem(
        route = "search",
        label = "Ejercicios",
        icon = Icons.Default.Search
    )
}

/**
 * BottomNavBar — Barra de navegación inferior de Gymera.
 *
 * Se muestra únicamente en las pantallas principales (Rutina y Búsqueda).
 * Las pantallas de detalle (DayDetail, ExerciseDetail) no la muestran
 * para no distraer al usuario mientras ve el contenido.
 *
 * @param currentRoute  La ruta activa actualmente (para resaltar el ítem correcto)
 * @param onItemClick   Callback cuando el usuario toca un ítem
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Routine,
        BottomNavItem.Search
    )

    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 0.dp   // sin sombra extra — el color ya lo diferencia
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = androidx.compose.ui.unit.TextUnit(
                            12f,
                            androidx.compose.ui.unit.TextUnitType.Sp
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    // Ítem seleccionado
                    selectedIconColor = PurplePrimary,
                    selectedTextColor = PurplePrimary,
                    // Ítem no seleccionado
                    unselectedIconColor = MutedGray,
                    unselectedTextColor = MutedGray,
                    // El fondo del ícono seleccionado (el "pill" de Material 3)
                    indicatorColor = PurplePrimary.copy(alpha = 0.15f)
                )
            )
        }
    }
}