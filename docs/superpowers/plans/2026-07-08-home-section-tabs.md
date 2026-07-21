# Abas na Home (Próximos | Últimas | Atrasadas) — Plano de Implementação

> **Para workers agênticos:** SUB-SKILL OBRIGATÓRIA: use superpowers:subagent-driven-development (recomendado) ou superpowers:executing-plans pra implementar tarefa a tarefa. Os passos usam checkbox (`- [ ]`).

**Goal:** Trocar as duas seções empilhadas da Home ("próximos"/"atrasadas") por uma barra de 3 abas — Próximos (default) | Últimas (avulsas do ciclo) | Atrasadas (com contador) — buscando as avulsas via endpoint existente, sem mudança de backend.

**Architecture:** App-only. O `HomeViewModel` passa a carregar as avulsas do ciclo (`DailyExpensesRepository.list(from, to)`, best-effort) junto do `getHome()`, guardando em `HomeUiState.Content.recentDailyExpenses`. A `HomeScreen` renderiza uma barra de abas nova (`HomeSectionTabs`) e alterna a lista exibida por aba, com estado local `rememberSaveable` resetado ao trocar de ciclo. `HomeListRow` ganha `status` opcional (avulsa não tem status).

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Hilt, kotlinx.serialization, JUnit4 + `MainDispatcherRule` + `FakeBillFolderApi`.

**Referência:** spec em `docs/superpowers/specs/2026-07-08-home-section-tabs-design.md`.

---

## Estrutura de arquivos

- **Criar** `app/src/main/java/com/billfolder/android/ui/screens/home/HomeRecentDaily.kt` — helper puro `recentFirst()`.
- **Criar** `app/src/test/java/com/billfolder/android/ui/screens/home/HomeRecentDailyTest.kt` — teste do helper.
- **Criar** `app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeSectionTabs.kt` — enum `HomeSection` + barra de abas.
- **Modificar** `app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeListRow.kt` — `status` opcional.
- **Modificar** `app/src/main/java/com/billfolder/android/ui/screens/home/HomeViewModel.kt` — injeta `DailyExpensesRepository`, adiciona `recentDailyExpenses` ao `Content`, popula em `load`/`pullRefresh`/`navigate`.
- **Modificar** `app/src/main/java/com/billfolder/android/ui/screens/home/HomeScreen.kt` — abas, troca de seção, rows de avulsa, empty states, reset por ciclo, limpeza de código morto.
- **Modificar** `app/src/main/res/values/strings.xml` — labels de aba + empty states.
- **Modificar** `app/src/test/java/com/billfolder/android/ui/screens/home/HomeViewModelTest.kt` — novo ctor + testes de `recentDailyExpenses`.

---

## Task 1: Helper puro `recentFirst()` (avulsas mais recentes primeiro)

**Files:**
- Create: `app/src/main/java/com/billfolder/android/ui/screens/home/HomeRecentDaily.kt`
- Test: `app/src/test/java/com/billfolder/android/ui/screens/home/HomeRecentDailyTest.kt`

- [ ] **Step 1: Escrever o teste falho**

```kotlin
// HomeRecentDailyTest.kt
package com.billfolder.android.ui.screens.home

import com.billfolder.android.data.dto.DailyExpenseResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRecentDailyTest {

    private fun daily(id: String, date: String) = DailyExpenseResponse(
        id = id, date = date, label = "x", amount = 10.0,
        categoryId = "cat", categoryName = "Cat", accountId = "acc", accountName = "Conta",
        createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-01T00:00:00Z",
    )

    @Test
    fun `recentFirst ordena por data descendente`() {
        val list = listOf(
            daily("a", "2026-06-05"),
            daily("b", "2026-06-20"),
            daily("c", "2026-06-10"),
        )
        val ids = list.recentFirst().map { it.id }
        assertEquals(listOf("b", "c", "a"), ids)
    }

    @Test
    fun `recentFirst em lista vazia retorna vazia`() {
        assertEquals(emptyList<DailyExpenseResponse>(), emptyList<DailyExpenseResponse>().recentFirst())
    }
}
```

- [ ] **Step 2: Rodar e ver falhar (não compila — `recentFirst` não existe)**

Run: `./gradlew :app:testDebugUnitTest --tests "com.billfolder.android.ui.screens.home.HomeRecentDailyTest"`
Expected: FAIL (Compilation error: unresolved reference `recentFirst`).

- [ ] **Step 3: Implementar o helper**

```kotlin
// HomeRecentDaily.kt
package com.billfolder.android.ui.screens.home

import com.billfolder.android.data.dto.DailyExpenseResponse

/**
 * Avulsas ordenadas da mais recente pra mais antiga. `date` é ISO
 * "yyyy-MM-dd", então ordem lexicográfica descendente == cronológica
 * descendente. Usado na aba "Últimas" da Home.
 */
fun List<DailyExpenseResponse>.recentFirst(): List<DailyExpenseResponse> =
    sortedByDescending { it.date }
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.billfolder.android.ui.screens.home.HomeRecentDailyTest"`
Expected: PASS (2 testes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/billfolder/android/ui/screens/home/HomeRecentDaily.kt \
        app/src/test/java/com/billfolder/android/ui/screens/home/HomeRecentDailyTest.kt
git commit -m "feat(home): helper recentFirst pra ordenar avulsas na aba Últimas

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: ViewModel — carregar avulsas do ciclo em `recentDailyExpenses`

**Files:**
- Modify: `app/src/main/java/com/billfolder/android/ui/screens/home/HomeViewModel.kt`
- Test: `app/src/test/java/com/billfolder/android/ui/screens/home/HomeViewModelTest.kt`

- [ ] **Step 1: Escrever os testes falhos**

Em `HomeViewModelTest.kt`, adicionar o import e um repo de avulsas ao setup, trocar a factory `viewModel()`, e adicionar 2 testes. Primeiro, adicionar aos imports:

```kotlin
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.repository.DailyExpensesRepository
```

Adicionar o repo junto dos outros (após `cyclesRepo`):

```kotlin
    private val dailyExpensesRepo = DailyExpensesRepository(api, notifier)
```

Trocar a factory:

```kotlin
    private fun viewModel() = HomeViewModel(homeRepo, authRepo, savingsRepo, cyclesRepo, dailyExpensesRepo, notifier)
```

Adicionar um factory de avulsa junto dos outros helpers:

```kotlin
    private fun daily(id: String, date: String) = DailyExpenseResponse(
        id = id, date = date, label = "avulsa $id", amount = 10.0,
        categoryId = "cat", categoryName = "Cat", accountId = "acc", accountName = "Conta",
        createdAt = "2026-06-01T00:00:00Z", updatedAt = "2026-06-01T00:00:00Z",
    )
```

Adicionar os testes (na seção "Initial load"):

```kotlin
    @Test
    fun `carrega recentDailyExpenses do ciclo ordenadas desc`() {
        api.onGetHome = { home("c1") }
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        api.dailyExpenses = listOf(daily("d1", "2026-06-05"), daily("d2", "2026-06-20"))

        val state = viewModel().state.value as HomeUiState.Content

        assertEquals(listOf("d2", "d1"), state.recentDailyExpenses.map { it.id })
    }

    @Test
    fun `falha ao listar avulsas nao derruba a Home`() {
        api.onGetHome = { home("c1") }
        api.cycles = listOf(cycle("c1", "2026-06-01"))
        api.onGetDailyExpenses = { _, _, _ -> throw http500() }

        val state = viewModel().state.value

        assertTrue(state is HomeUiState.Content)
        state as HomeUiState.Content
        assertTrue(state.recentDailyExpenses.isEmpty())
    }
```

> **Nota:** o `FakeBillFolderApi` hoje serve avulsas via a var `dailyExpenses` (retorno fixo). Pro segundo teste precisamos de um hook que lança exceção. Adicionar ao fake (ver Step 2).

- [ ] **Step 2: Adicionar o hook de erro ao `FakeBillFolderApi`**

Em `app/src/test/java/com/billfolder/android/testutil/FakeBillFolderApi.kt`, trocar o override de `getDailyExpenses` pra respeitar um hook opcional (mantendo `dailyExpenses` como default):

Adicionar o campo (junto dos outros `on...` de daily):

```kotlin
    var onGetDailyExpenses: ((String?, String?, String?) -> List<DailyExpenseResponse>)? = null
```

Trocar o override:

```kotlin
    override suspend fun getDailyExpenses(from: String?, to: String?, categoryId: String?): List<DailyExpenseResponse> =
        onGetDailyExpenses?.invoke(from, to, categoryId) ?: dailyExpenses
```

- [ ] **Step 3: Rodar e ver falhar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.billfolder.android.ui.screens.home.HomeViewModelTest"`
Expected: FAIL (Compilation error: `recentDailyExpenses` não existe em `Content`; ctor do `HomeViewModel` não bate).

- [ ] **Step 4: Implementar no `HomeViewModel`**

Adicionar o import:

```kotlin
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.data.repository.DailyExpensesRepository
```

Adicionar o campo ao `Content` (após `isRefreshing`):

```kotlin
        /**
         * Avulsas do ciclo atual, da mais recente pra mais antiga. Alimenta a
         * aba "Últimas". Best-effort: falha no fetch cai em lista vazia sem
         * derrubar a Home.
         */
        val recentDailyExpenses: List<DailyExpenseResponse> = emptyList(),
```

Injetar o repo no construtor (antes de `dataChangeNotifier`):

```kotlin
    private val cyclesRepository: CyclesRepository,
    private val dailyExpensesRepository: DailyExpensesRepository,
    private val dataChangeNotifier: DataChangeNotifier,
```

Em `load()`, após obter `cycles`, buscar as avulsas e passar ao `Content`:

```kotlin
                val recentDaily = runCatching {
                    dailyExpensesRepository
                        .list(from = home.cycle.startDate, to = home.cycle.endDate)
                        .recentFirst()
                }.getOrDefault(emptyList())
                HomeUiState.Content(
                    data = home,
                    hasAnySavingsAccount = hasSavings,
                    cycles = cycles,
                    recentDailyExpenses = recentDaily,
                )
```

Em `pullRefresh()`, dentro do `try`, após obter `cycles`:

```kotlin
                val recentDaily = runCatching {
                    dailyExpensesRepository
                        .list(from = home.cycle.startDate, to = home.cycle.endDate)
                        .recentFirst()
                }.getOrDefault(emptyList())
                _state.update {
                    (it as? HomeUiState.Content)?.copy(
                        data = home,
                        hasAnySavingsAccount = hasSavings,
                        cycles = cycles,
                        recentDailyExpenses = recentDaily,
                        isRefreshing = false,
                    ) ?: it
                }
```

Em `navigate()`, dentro do `try`, após obter `home`:

```kotlin
                val recentDaily = runCatching {
                    dailyExpensesRepository
                        .list(from = home.cycle.startDate, to = home.cycle.endDate)
                        .recentFirst()
                }.getOrDefault(emptyList())
                _state.update { s ->
                    (s as? HomeUiState.Content)?.copy(
                        data = home,
                        recentDailyExpenses = recentDaily,
                        isSwitchingCycle = false,
                    ) ?: s
                }
```

- [ ] **Step 5: Rodar e ver passar**

Run: `./gradlew :app:testDebugUnitTest --tests "com.billfolder.android.ui.screens.home.HomeViewModelTest"`
Expected: PASS (todos, incluindo os 2 novos).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/billfolder/android/ui/screens/home/HomeViewModel.kt \
        app/src/test/java/com/billfolder/android/ui/screens/home/HomeViewModelTest.kt \
        app/src/test/java/com/billfolder/android/testutil/FakeBillFolderApi.kt
git commit -m "feat(home): carrega avulsas do ciclo em recentDailyExpenses (best-effort)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: `HomeListRow` — `status` opcional

**Files:**
- Modify: `app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeListRow.kt`

Mudança de UI mecânica (sem lógica testável): quando `status == null`, não renderiza o `StatusChip`. Avulsa passa `null`.

- [ ] **Step 1: Tornar `status` nullable e condicionar o chip**

Trocar a assinatura:

```kotlin
fun HomeListRow(
    title: String,
    subtitle: String,
    amount: Double,
    isoDate: String,
    status: String? = null,
    modifier: Modifier = Modifier,
) {
```

Trocar o bloco do chip (na coluna à direita) por:

```kotlin
                if (status != null) {
                    Spacer(Modifier.height(6.dp))
                    StatusChip(status = status)
                }
```

- [ ] **Step 2: Compilar**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (As chamadas atuais passam `status` posicional — seguem válidas.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeListRow.kt
git commit -m "refactor(home): HomeListRow com status opcional (avulsa sem chip)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Componente `HomeSectionTabs` + enum `HomeSection`

**Files:**
- Create: `app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeSectionTabs.kt`

- [ ] **Step 1: Criar o componente**

```kotlin
package com.billfolder.android.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.billfolder.android.R
import com.billfolder.android.ui.theme.PillShape

/** As três seções da lista da Home, expostas como abas. */
enum class HomeSection { Upcoming, Recent, Overdue }

/**
 * Barra de 3 abas (pills) da Home: próximos | últimas | atrasadas. A aba
 * Atrasadas destaca a urgência com um contador e cor de erro quando há itens
 * atrasados (é a aba menos proeminente, então o contador evita que passe
 * batido).
 */
@Composable
fun HomeSectionTabs(
    selected: HomeSection,
    onSelect: (HomeSection) -> Unit,
    overdueCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTab(
            label = stringResource(R.string.home_tab_upcoming),
            selected = selected == HomeSection.Upcoming,
            onClick = { onSelect(HomeSection.Upcoming) },
            modifier = Modifier.weight(1f),
        )
        SectionTab(
            label = stringResource(R.string.home_tab_recent),
            selected = selected == HomeSection.Recent,
            onClick = { onSelect(HomeSection.Recent) },
            modifier = Modifier.weight(1f),
        )
        SectionTab(
            label = if (overdueCount > 0) {
                stringResource(R.string.home_tab_overdue_count, overdueCount)
            } else {
                stringResource(R.string.home_tab_overdue)
            },
            selected = selected == HomeSection.Overdue,
            onClick = { onSelect(HomeSection.Overdue) },
            isAlert = overdueCount > 0,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SectionTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false,
) {
    val container = when {
        selected && isAlert -> MaterialTheme.colorScheme.errorContainer
        selected            -> MaterialTheme.colorScheme.secondaryContainer
        else                -> MaterialTheme.colorScheme.surfaceContainer
    }
    val content = when {
        selected && isAlert -> MaterialTheme.colorScheme.onErrorContainer
        selected            -> MaterialTheme.colorScheme.onSecondaryContainer
        isAlert             -> MaterialTheme.colorScheme.error
        else                -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        shape = PillShape,
        color = container,
        contentColor = content,
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
        )
    }
}
```

- [ ] **Step 2: Compilar** (as strings entram na Task 5; se compilar antes delas, adicione as strings primeiro)

Run: `./gradlew :app:assembleDebug` (após a Task 5)
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit** (junto da Task 5, já que dependem das strings)

---

## Task 5: Strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Adicionar as strings novas** (junto do bloco da Home)

```xml
<!-- Home section tabs -->
<string name="home_tab_upcoming">próximos</string>
<string name="home_tab_recent">últimas</string>
<string name="home_tab_overdue">atrasadas</string>
<!-- %1$d = qtd de atrasadas -->
<string name="home_tab_overdue_count">atrasadas · %1$d</string>
<string name="home_upcoming_empty">nenhuma conta a vencer nesse ciclo</string>
<string name="home_recent_empty">nenhuma despesa avulsa nesse ciclo</string>
<string name="home_overdue_empty">nada atrasado 🎉</string>
```

- [ ] **Step 2: Remover strings que ficam órfãs** (as seções viram abas)

Conferir uso e, se só a `HomeScreen` usava, remover:
- `home_section_next_due`
- `home_section_overdue`
- `home_overdue_warning_content_description` (o realce de urgência agora é a cor da aba)

Run pra confirmar que não há outros usos:
`grep -rn "home_section_next_due\|home_section_overdue\|home_overdue_warning_content_description" app/src/main`
Expected após a Task 6: nenhum resultado (então pode remover as 3).

- [ ] **Step 3: Commit** (junto da Task 4)

```bash
git add app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeSectionTabs.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(home): componente HomeSectionTabs + strings das abas

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 6: `HomeScreen` — abas, troca de seção, rows de avulsa, empty states

**Files:**
- Modify: `app/src/main/java/com/billfolder/android/ui/screens/home/HomeScreen.kt`

- [ ] **Step 1: Imports**

Adicionar:

```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import com.billfolder.android.data.dto.DailyExpenseResponse
import com.billfolder.android.ui.screens.home.components.HomeSection
import com.billfolder.android.ui.screens.home.components.HomeSectionTabs
```

(`Text`, `MaterialTheme`, `Modifier`, `dp`, `stringResource` já estão importados.)

- [ ] **Step 2: Passar `recentDailyExpenses` pro `HomeContent`**

No `HomeScaffold`, no ramo `is HomeUiState.Content`:

```kotlin
                is HomeUiState.Content -> HomeContent(
                    data = s.data,
                    recentDailyExpenses = s.recentDailyExpenses,
                    isRefreshing = s.isRefreshing,
                    onPullRefresh = onPullRefresh,
                    onPreviousCycle = onPreviousCycle,
                    onNextCycle = onNextCycle,
                )
```

- [ ] **Step 3: Reescrever `HomeContent`**

Trocar a assinatura e o corpo (a partir do cálculo de `nextDue`/`overdue` até o fim do `LazyColumn`):

```kotlin
@Composable
private fun HomeContent(
    data: HomeResponse,
    recentDailyExpenses: List<DailyExpenseResponse>,
    isRefreshing: Boolean,
    onPullRefresh: () -> Unit,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
) {
    val cardStatementSubtitle = stringResource(R.string.home_list_card_statement_subtitle)
    val nextDue = collectNextDue(data, cardStatementSubtitle)
    val overdue = collectOverdue(data)

    // Aba selecionada — sobrevive à rotação; reseta pra Próximos ao trocar de
    // ciclo (o cycle.id muda → LaunchedEffect dispara). Na rotação o id não
    // muda, então preserva a aba escolhida.
    var selectedSection by rememberSaveable { mutableStateOf(HomeSection.Upcoming) }
    LaunchedEffect(data.cycle.id) { selectedSection = HomeSection.Upcoming }

    BillFolderPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onPullRefresh,
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            CycleNavigator(
                cycleLabel = data.cycle.label,
                startIso = data.cycle.startDate,
                endIso = data.cycle.endDate,
                onPrevious = onPreviousCycle,
                onNext = onNextCycle,
            )
        }

        item {
            HomeHeroCard(balance = data.balance)
        }

        if (data.categoryBreakdown.isNotEmpty()) {
            item {
                WhereMoneyGoingCard(breakdown = data.categoryBreakdown)
            }
        }

        item {
            HomeSectionTabs(
                selected = selectedSection,
                onSelect = { selectedSection = it },
                overdueCount = overdue.size,
            )
        }

        when (selectedSection) {
            HomeSection.Upcoming ->
                if (nextDue.isEmpty()) {
                    item { TabEmptyState(text = stringResource(R.string.home_upcoming_empty)) }
                } else {
                    items(nextDue, key = { it.id }) { row -> ProjectionRow(row) }
                }
            HomeSection.Recent ->
                if (recentDailyExpenses.isEmpty()) {
                    item { TabEmptyState(text = stringResource(R.string.home_recent_empty)) }
                } else {
                    items(recentDailyExpenses, key = { it.id }) { d ->
                        HomeListRow(
                            title = d.label,
                            subtitle = d.categoryName,
                            amount = d.amount,
                            isoDate = d.date,
                            status = null,
                        )
                    }
                }
            HomeSection.Overdue ->
                if (overdue.isEmpty()) {
                    item { TabEmptyState(text = stringResource(R.string.home_overdue_empty)) }
                } else {
                    items(overdue, key = { it.id }) { row -> ProjectionRow(row) }
                }
        }

        // Espaço extra no fim pra FAB não cobrir o último item.
        item { Spacer(Modifier.height(80.dp)) }
    }
    }
}

@Composable
private fun ProjectionRow(row: HomeRowProjection) {
    HomeListRow(
        title = row.title,
        subtitle = row.subtitle,
        amount = row.amount,
        isoDate = row.dueDate,
        status = row.status,
    )
}

@Composable
private fun TabEmptyState(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    )
}
```

- [ ] **Step 4: Remover o `SectionHeader` (agora morto)**

O composable `private fun SectionHeader(...)` na `HomeScreen.kt` deixa de ser usado (as abas substituem os cabeçalhos). Removê-lo, junto do import de `Icons.Default.Warning` e `Icon`/`Row`/`size` se ficarem sem uso após a remoção. Rodar o build pra confirmar imports órfãos (o Kotlin avisa; o app trata warnings como aceitáveis, mas limpe os óbvios).

- [ ] **Step 5: Compilar + suíte**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL, todos os testes verdes.

- [ ] **Step 6: Confirmar strings órfãs e removê-las** (fecha a Task 5, Step 2)

Run: `grep -rn "home_section_next_due\|home_section_overdue\|home_overdue_warning_content_description" app/src/main`
Expected: nenhum resultado → remover as 3 do `strings.xml`. Rodar `./gradlew :app:assembleDebug` de novo pra confirmar.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/billfolder/android/ui/screens/home/HomeScreen.kt \
        app/src/main/res/values/strings.xml
git commit -m "feat(home): abas Próximos | Últimas | Atrasadas na lista da Home

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 7: Verificação final

- [ ] **Step 1: Suíte + build limpos**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL; contagem de testes = baseline + 4 (2 de `HomeRecentDailyTest` + 2 de `HomeViewModelTest`).

- [ ] **Step 2: E2E manual no device** (`./gradlew :app:installDebug`)

- Abrir a Home: aba **Próximos** selecionada por default, mostrando contas a vencer.
- Tocar **Últimas**: lista as avulsas do ciclo, mais recente no topo. Sem avulsas → "nenhuma despesa avulsa nesse ciclo".
- Tocar **Atrasadas**: com atrasadas, a aba mostra "atrasadas · N" e cor de alerta; sem → "nada atrasado 🎉".
- Trocar de ciclo pelas setas → volta pra aba **Próximos** e as avulsas acompanham o novo ciclo.
- Adicionar uma avulsa pelo Speed Dial → aparece na aba **Últimas** (bus/refresh).
- Rotacionar a tela numa aba != Próximos → mantém a aba (rememberSaveable).

## Notas

- **Sem backend / sem deploy** — só app, instala com `./gradlew :app:installDebug`.
- **Linhas read-only** na Home (editar/pagar avulsa segue na tela de avulsas) — mantido.
- **Sem teste de Compose UI** (o projeto não tem infra); a seleção de aba é coberta no E2E manual.
