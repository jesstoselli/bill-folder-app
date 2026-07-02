package com.billfolder.android.ui.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.billfolder.android.ui.components.BillFolderDrawer
import com.billfolder.android.ui.components.DrawerDestination
import com.billfolder.android.ui.screens.auth.ForgotPasswordScreen
import com.billfolder.android.ui.screens.auth.LoginScreen
import com.billfolder.android.ui.screens.auth.ResetPasswordScreen
import com.billfolder.android.ui.screens.auth.SignupScreen
import com.billfolder.android.ui.screens.cards.CardsScreen
import com.billfolder.android.ui.screens.dailyexpenses.DailyExpensesScreen
import com.billfolder.android.ui.screens.expenses.ExpensesScreen
import com.billfolder.android.ui.screens.home.HomeScreen
import com.billfolder.android.ui.screens.income.IncomeScreen
import com.billfolder.android.ui.screens.managebanks.ManageBanksScreen
import com.billfolder.android.ui.screens.managecards.ManageCardsScreen
import com.billfolder.android.ui.screens.managesavings.ManageSavingsScreen
import com.billfolder.android.ui.screens.savings.SavingsScreen
import kotlinx.coroutines.launch

/**
 * NavHost da raiz. Recebe a flag inicial isLoggedIn pra decidir start destination
 * — assim, o app aberto numa sessão já autenticada pula direto pra Home.
 *
 * O ModalNavigationDrawer mora aqui (fora do NavHost) pra ficar acessível
 * de qualquer tela drawer-rooted (Home, DailyExpenses, Expenses, Income,
 * Cards, ManageCards). Telas de auth (Login, Signup) desativam o gesture
 * pra não abrir o drawer no fluxo de autenticação.
 */
@Composable
fun BillFolderNavHost(
    isLoggedIn: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    val start = if (isLoggedIn) Routes.HOME else Routes.LOGIN

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val currentDrawerDestination = currentRoute.toDrawerDestination()

    // Drawer só fica habilitado em telas de feature. Auth (login/signup)
    // não tem drawer — fora do contexto.
    val drawerEnabled = currentDrawerDestination != null

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerEnabled,
        drawerContent = {
            BillFolderDrawer(
                // Quando estamos em rota não-mapeada (auth), passa Home como
                // selected (placeholder) — o drawer está disabled mesmo, então
                // o user nunca vê esse estado.
                selected = currentDrawerDestination ?: DrawerDestination.Home,
                onNavigate = { destination ->
                    scope.launch { drawerState.close() }
                    if (destination != currentDrawerDestination) {
                        navController.navigateFromDrawer(destination)
                    }
                },
            )
        },
    ) {
        NavHost(navController = navController, startDestination = start) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { navController.navigateClearingAuth() },
                    onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
                    onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
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
            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    onCodeSent = { email ->
                        // Empurra reset-password?email=... por cima. Manter
                        // ForgotPasswordScreen no back stack pra user poder
                        // voltar caso digite o email errado.
                        navController.navigate(Routes.resetPasswordFor(email))
                    },
                    onNavigateBack = {
                        navController.popBackStack(Routes.LOGIN, inclusive = false)
                    },
                )
            }
            composable(
                route = Routes.RESET_PASSWORD_PATTERN,
                arguments = listOf(
                    navArgument(Routes.RESET_PASSWORD_ARG_EMAIL) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                ResetPasswordScreen(
                    onPasswordReset = {
                        // Reset OK: pula direto pra Login, limpando o fluxo
                        // de reset do back stack. User loga com a senha nova.
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        // "recomeçar" — volta pra ForgotPassword pra digitar
                        // email de novo (ex: se não recebeu o código).
                        navController.popBackStack(Routes.FORGOT_PASSWORD, inclusive = false)
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
                    onMenuClick = openDrawer,
                )
            }
            composable(Routes.DAILY_EXPENSES) {
                DailyExpensesScreen(
                    onMenuClick = openDrawer,
                )
            }
            composable(Routes.EXPENSES) {
                ExpensesScreen(
                    onMenuClick = openDrawer,
                )
            }
            composable(Routes.INCOME) {
                IncomeScreen(
                    onMenuClick = openDrawer,
                )
            }
            composable(
                route = Routes.CARDS_PATTERN,
                arguments = listOf(
                    navArgument(Routes.CARDS_ARG_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                CardsScreen(
                    onMenuClick = openDrawer,
                )
            }
            composable(Routes.MANAGE_CARDS) {
                ManageCardsScreen(
                    onMenuClick = openDrawer,
                    onNavigateToCardEntries = { cardId ->
                        navController.navigate(Routes.cardsWithSelected(cardId)) {
                            // Pop até Home pra back voltar pra Home (e não
                            // pra ManageCards). Não usamos saveState/
                            // restoreState aqui de propósito: queremos que
                            // o VM do Cards seja recriado lendo o cardId
                            // novo via SavedStateHandle. Com restoreState=true
                            // o VM antigo seria reusado, ignorando o arg.
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.MANAGE_SAVINGS) {
                ManageSavingsScreen(
                    onMenuClick = openDrawer,
                    onNavigateToTransactions = { savingsAccountId ->
                        navController.navigate(Routes.savingsWithSelected(savingsAccountId)) {
                            // Mesmo padrão do Cards: pop até Home pra back
                            // voltar pra Home (e não pra ManageSavings).
                            // Sem saveState/restoreState aqui — queremos VM
                            // recriado lendo o arg via SavedStateHandle.
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = Routes.SAVINGS_PATTERN,
                arguments = listOf(
                    navArgument(Routes.SAVINGS_ARG_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                SavingsScreen(
                    onMenuClick = openDrawer,
                )
            }
            composable(Routes.MANAGE_BANKS) {
                ManageBanksScreen(
                    onMenuClick = openDrawer,
                )
            }
        }
    }
}

/**
 * Mapeia destinos do drawer pra rotas reais. Telas que ainda não existem
 * (Adjustments) são no-op por enquanto — só fecham o drawer e ficam onde
 * estão.
 */
private fun NavHostController.navigateFromDrawer(destination: DrawerDestination) {
    val route = when (destination) {
        DrawerDestination.Home           -> Routes.HOME
        DrawerDestination.DailyExpenses  -> Routes.DAILY_EXPENSES
        DrawerDestination.Expenses       -> Routes.EXPENSES
        DrawerDestination.Income         -> Routes.INCOME
        DrawerDestination.Cards          -> Routes.CARDS           // consumo
        DrawerDestination.Savings        -> Routes.SAVINGS         // consumo
        DrawerDestination.ManageCards    -> Routes.MANAGE_CARDS    // CRUD
        DrawerDestination.ManageSavings  -> Routes.MANAGE_SAVINGS  // CRUD
        DrawerDestination.ManageBanks    -> Routes.MANAGE_BANKS    // CRUD
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
 * Mapeamento inverso: route string atual → DrawerDestination, pra
 * destacar o item correspondente quando o drawer abre. Retorna null
 * em rotas que não fazem parte do drawer (auth, e mais futuras).
 */
private fun String?.toDrawerDestination(): DrawerDestination? = when (this) {
    Routes.HOME            -> DrawerDestination.Home
    Routes.DAILY_EXPENSES  -> DrawerDestination.DailyExpenses
    Routes.EXPENSES        -> DrawerDestination.Expenses
    Routes.INCOME          -> DrawerDestination.Income
    Routes.CARDS           -> DrawerDestination.Cards
    Routes.SAVINGS         -> DrawerDestination.Savings
    Routes.MANAGE_CARDS    -> DrawerDestination.ManageCards
    Routes.MANAGE_SAVINGS  -> DrawerDestination.ManageSavings
    Routes.MANAGE_BANKS    -> DrawerDestination.ManageBanks
    else                   -> null
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
