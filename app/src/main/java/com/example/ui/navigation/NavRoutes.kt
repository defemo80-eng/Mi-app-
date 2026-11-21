package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface DeFittRoute {
    @Serializable data object Splash : DeFittRoute
    @Serializable data object Auth : DeFittRoute
    @Serializable data object Home : DeFittRoute
    @Serializable data object Registro : DeFittRoute
    @Serializable data object Tienda : DeFittRoute
    @Serializable data object Logros : DeFittRoute
    @Serializable data object Perfil : DeFittRoute
    @Serializable data object Workout : DeFittRoute
    @Serializable data object FoodScanner : DeFittRoute
    @Serializable data object BodyScanner : DeFittRoute
    @Serializable data object Wallet : DeFittRoute
    @Serializable data object Evolution : DeFittRoute
}
