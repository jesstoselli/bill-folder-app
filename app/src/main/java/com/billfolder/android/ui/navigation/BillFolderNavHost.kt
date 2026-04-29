package com.billfolder.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.billfolder.android.ui.components.DrawerDestination
import com.billfolder.android.ui.screens.auth.LoginScreen
import com.billfolder.android.ui.screens.auth.SignupScreen
import com.billfolder.android.ui.screens.dailyexpenses.DailyExpensesScreen
import com.billfolder.android.ui.screens.expenses.ExpensesScreen
import com.billfolder.android.ui.screens.home.HomeScreen
import com.billfolder.android.ui.screens.income.IncomeScreen

/**
 * NavHost da raiz. Recebe a flag inicial isLoggedIn pra decidir start destination
 * — assim, o app aberto numa sessão já autenticada pula direto pra Home.
 */
@Composable
fun BillFolderNavHost(
    isLoggedIn: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val start = if (isLoggedIn) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = { navController.navigateClearingAuth() },
                onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
            )
        }
        composable(Routes.SIGNUP) {
            SignupScreen(
                onSignupSuccess = { navController.navigateClearingAuth() },
                onNavigateToLogin = {
                    navController.popBackStack(Routes.LOGIN, inclusive = false)
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        // Sai da Home com back stack limpo — usuário não consegue
                        // voltar pra Home com o botão back depois de deslogar.
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateFromDrawer = { destination ->
                    navController.navigateFromDrawer(destination)
                },
            )
        }
        composable(Routes.DAILY_EXPENSES) {
            DailyExpensesScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.EXPENSES) {
            ExpensesScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.INCOME) {
            IncomeScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Mapeia destinos do drawer pra rotas reais. Telas que ainda não existem
 * (Income, Expenses, Cards, etc.) são no-op por enquanto — só fecham
 * o drawer e ficam onde estão.
 */
private fun NavHostController.navigateFromDrawer(destination: DrawerDestination) {
    val route = when (destination) {
        DrawerDestination.Home          -> Routes.HOME
        DrawerDestination.DailyExpenses -> Routes.DAILY_EXPENSES
        DrawerDestination.Expenses      -> Routes.EXPENSES
        DrawerDestination.Income        -> Routes.INCOME
        else -> return // ainda não temos tela; drawer já fechou no caller
    }
    navigate(route) {
        // Não acumula stack se o usuário fica trocando entre items
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Após login/signup bem-sucedido, vai pra Home zerando o stack pra
 * evitar que o botão "back" leve de volta pra tela de login.
 */
private fun NavHostController.navigateClearingAuth() {
    navigate(Routes.HOME) {
        popUpTo(0) { inclusive = true }
    }
}
