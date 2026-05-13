package com.DeBiaseRamiro.gymera.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.DeBiaseRamiro.gymera.ui.theme.*

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Routine : BottomNavItem("routine", "Mi Rutina", Icons.Default.CalendarMonth)
    object Search  : BottomNavItem("search",  "Ejercicios", Icons.Default.Search)
    object Profile : BottomNavItem("profile", "Perfil",    Icons.Default.Person)
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Routine,
        BottomNavItem.Search,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item.route) },
                icon = {
                    Icon(imageVector = item.icon, contentDescription = item.label)
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
                    selectedIconColor   = PurplePrimary,
                    selectedTextColor   = PurplePrimary,
                    unselectedIconColor = MutedGray,
                    unselectedTextColor = MutedGray,
                    indicatorColor      = PurplePrimary.copy(alpha = 0.15f)
                )
            )
        }
    }
}