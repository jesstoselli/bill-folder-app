package com.billfolder.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.ui.theme.BfBrandBill
import com.billfolder.android.ui.theme.DarkOnPrimary

/**
 * Destinos navegáveis a partir do drawer. Estável entre runs (não trocar
 * a ordem das entries do enum sem cuidado — a ViewModel pode estar usando
 * o name como chave em algum momento).
 */
enum class DrawerDestination {
    Home,
    DailyExpenses,
    Income,
    Expenses,
    Cards,
    Savings,
    Adjustments,
    ManageCards,
    ManageSavings,
    ManageBanks,
}

/**
 * Drawer lateral do BillFolder. Estrutura conforme wireframe v0.1 §5.1:
 *  - Header com logo wordmark
 *  - Lista principal (home + 6 features)
 *  - Divisor + seção "gerenciar" (cards + banks — diferenciados de
 *    "despesas no cartão" porque aqui é CRUD da entidade, não consumo)
 *  - Footer com assinatura discreta "app by jess.to"
 */
@Composable
fun BillFolderDrawer(
    selected: DrawerDestination,
    onNavigate: (DrawerDestination) -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
        ) {
            // ----- Header com logo wordmark -----
            Icon(
                painter = painterResource(id = R.drawable.logo_billfolder),
                contentDescription = stringResource(R.string.app_logo_content_description),
                tint = Color.Unspecified, // logo já tem cores próprias
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                    .width(160.dp),
            )

            Spacer(Modifier.height(16.dp))

            // ----- Navegação principal -----
            DrawerItem(
                label = stringResource(R.string.drawer_home),
                icon = Icons.Default.Home,
                selected = selected == DrawerDestination.Home,
                onClick = { onNavigate(DrawerDestination.Home) },
            )
            DrawerItem(
                label = stringResource(R.string.drawer_daily),
                icon = Icons.Default.ShoppingBag,
                selected = selected == DrawerDestination.DailyExpenses,
                onClick = { onNavigate(DrawerDestination.DailyExpenses) },
            )
            DrawerItem(
                label = stringResource(R.string.drawer_income),
                icon = Icons.Default.AttachMoney,
                selected = selected == DrawerDestination.Income,
                onClick = { onNavigate(DrawerDestination.Income) },
            )
            DrawerItem(
                label = stringResource(R.string.drawer_expenses),
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                selected = selected == DrawerDestination.Expenses,
                onClick = { onNavigate(DrawerDestination.Expenses) },
            )
            DrawerItem(
                label = stringResource(R.string.drawer_cards),
                icon = Icons.Default.CreditCard,
                selected = selected == DrawerDestination.Cards,
                onClick = { onNavigate(DrawerDestination.Cards) },
            )
            DrawerItem(
                label = stringResource(R.string.drawer_savings),
                icon = Icons.Default.Savings,
                selected = selected == DrawerDestination.Savings,
                onClick = { onNavigate(DrawerDestination.Savings) },
            )
            DrawerItem(
                label = stringResource(R.string.drawer_adjustments),
                icon = Icons.Default.Tune,
                selected = selected == DrawerDestination.Adjustments,
                onClick = { onNavigate(DrawerDestination.Adjustments) },
            )

            // ----- Seção "gerenciar" -----
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Text(
                text = stringResource(R.string.drawer_section_manage),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(4.dp))

            DrawerItem(
                label = stringResource(R.string.drawer_manage_cards),
                icon = Icons.Default.CreditCard,
                selected = selected == DrawerDestination.ManageCards,
                onClick = { onNavigate(DrawerDestination.ManageCards) },
            )
            DrawerItem(
                label = stringResource(R.string.drawer_manage_savings),
                icon = Icons.Default.Savings,
                selected = selected == DrawerDestination.ManageSavings,
                onClick = { onNavigate(DrawerDestination.ManageSavings) },
            )
            DrawerItem(
                label = stringResource(R.string.drawer_manage_banks),
                icon = Icons.Default.AccountBalance,
                selected = selected == DrawerDestination.ManageBanks,
                onClick = { onNavigate(DrawerDestination.ManageBanks) },
            )

            // ----- Footer -----
            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.drawer_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Wrapper sobre NavigationDrawerItem com paddings + cores do DS aplicadas.
 * Centraliza o styling pra todos os items respeitarem o mesmo look.
 */
@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        icon = { Icon(imageVector = icon, contentDescription = null) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        colors = NavigationDrawerItemDefaults.colors(
            // Verde do logo (BfBrandBill, #86BC65) pra item selecionado.
            // Texto/ícone em onPrimary (verde escuro) garantem contraste
            // legível em cima do BfBrandBill.
            selectedContainerColor = BfBrandBill,
            selectedTextColor = DarkOnPrimary,
            selectedIconColor = DarkOnPrimary,
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
