package com.xis.mypower.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.xis.mypower.ui.theme.BackgroundDark
import com.xis.mypower.ui.theme.PrimaryGreen
import com.xis.mypower.ui.theme.TextGray

@Suppress("DEPRECATION")
@Composable
fun BottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = BackgroundDark,
        contentColor = PrimaryGreen
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("DASHBOARD", fontSize = 10.sp) },
            selected = currentRoute == "dashboard",
            onClick = { onNavigate("dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                unselectedIconColor = TextGray,
                selectedTextColor = PrimaryGreen,
                unselectedTextColor = TextGray,
                indicatorColor = BackgroundDark // Transparent indicator like reference
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ShowChart, contentDescription = "Histori") },
            label = { Text("HISTORI", fontSize = 10.sp) },
            selected = currentRoute == "history",
            onClick = { onNavigate("history") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                unselectedIconColor = TextGray,
                selectedTextColor = PrimaryGreen,
                unselectedTextColor = TextGray,
                indicatorColor = BackgroundDark
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Tune, contentDescription = "Kontrol") },
            label = { Text("KONTROL", fontSize = 10.sp) },
            selected = currentRoute == "control",
            onClick = { onNavigate("control") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                unselectedIconColor = TextGray,
                selectedTextColor = PrimaryGreen,
                unselectedTextColor = TextGray,
                indicatorColor = BackgroundDark
            )
        )
    }
}
