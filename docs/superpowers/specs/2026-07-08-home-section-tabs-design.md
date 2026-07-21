# Abas na Home: Próximos | Últimas | Atrasadas — Design

**Data:** 2026-07-08
**App:** BillFolderApp (Kotlin / Jetpack Compose + Hilt + Retrofit + kotlinx.serialization)
**Backend:** nenhuma mudança.

## Contexto

A Home hoje empilha duas seções abaixo do bloco de topo (navegador de ciclo →
hero → "onde meu dinheiro vai"):

- **"próximos"** (`home_section_next_due`) — despesas a vencer + faturas de cartão não pagas no ciclo.
- **"atrasadas"** (`home_section_overdue`) — despesas overdue.

Despesas **avulsas** (daily expenses) NÃO aparecem na Home — têm tela própria
(`DailyExpensesScreen`) e endpoint próprio (`GET /daily-expenses?from=&to=`). O
`HomeResponse` do backend só traz o agregado `balance.dailyExpensesSpent`, não a
lista.

## Objetivo

Substituir as duas seções empilhadas por uma **barra de 3 abas**, adicionando uma
lista nova de avulsas recentes:

1. **Próximos** (default) — o conteúdo do "próximos" atual.
2. **Últimas** — avulsas do ciclo atual, da mais recente pra mais antiga.
3. **Atrasadas** — o conteúdo do "overdue" atual, com contador de urgência.

## Decisões travadas (com a usuária)

- **Escopo de "Últimas"**: avulsas com data **dentro do ciclo atual** que a Home
  está exibindo (cycle-scoped, acompanha o navegador de ciclo), ordenadas por
  data **descendente** (mais recente primeiro).
- **Atrasadas vira a 3ª aba** (ordem: Próximos | Últimas | Atrasadas), com um
  **contador** de itens atrasados na própria aba + o ícone/realce de alerta que já
  existe — pra urgência não se perder atrás da aba menos proeminente.
- **Aba default = Próximos**; a seleção reseta pra Próximos ao trocar de ciclo.
- **Linhas read-only** na Home (painel). Editar/pagar avulsa continua na
  `DailyExpensesScreen`.
- **Sem mudança de backend**: as avulsas são buscadas no app pelo endpoint
  existente, best-effort (falha → lista vazia, não derruba a Home).

## Arquitetura / origem dos dados

O `HomeViewModel` passa a carregar, junto do `getHome()`, as avulsas do ciclo via
`DailyExpensesRepository.list(from = cycle.startDate, to = cycle.endDate)`.

- **Best-effort**: envolto em `runCatching { ... }.getOrDefault(emptyList())`,
  igual ao `hasAnySavingsAccount`/`cycles` já existentes — uma falha no fetch das
  avulsas NÃO derruba a Home.
- O ciclo vem do próprio `HomeResponse.cycle` (`startDate`/`endDate`). No `load()`
  inicial e no `pullRefresh()` usamos `home.cycle`; no `navigate()` (troca de
  ciclo) usamos o `target` (o ciclo destino), pra a lista de avulsas acompanhar.
- Ordenação/limite ficam num **helper puro testável** (não há limite de
  contagem — mostramos todas as avulsas do ciclo, ordenadas desc por data).

## Componentes

### `HomeSectionTabs` (novo, reutilizável)

`app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeSectionTabs.kt`

Barra horizontal de 3 segmentos no estilo do app (PillShape, texto lowercase),
com o segmento ativo destacado (fundo `primary`/`secondaryContainer`, os demais
neutros). Recebe:

- `selected: HomeSection` (enum: `Upcoming`, `Recent`, `Overdue`)
- `onSelect: (HomeSection) -> Unit`
- `overdueCount: Int` — quando `> 0`, a aba Atrasadas mostra o número (ex:
  "atrasadas · 2") e o realce de alerta (cor `error`).

`HomeSection` é um enum simples no mesmo arquivo (ou em `HomeScreen.kt`), com um
label via `stringResource` resolvido na tela.

### `HomeListRow` (modificar)

`app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeListRow.kt`

Tornar o status opcional: `status: String? = null`. Quando `null`, **não**
renderiza o `StatusChip` (a coluna à direita mostra só o valor). Linhas de conta
a vencer/atrasada continuam passando o status; avulsas passam `null`.

### `HomeScreen` (modificar)

`app/src/main/java/com/billfolder/android/ui/screens/home/HomeScreen.kt`

- Estado local `var selectedSection by rememberSaveable { mutableStateOf(HomeSection.Upcoming) }`
  (sem key — sobrevive à rotação preservando a aba escolhida).
- Resetar pra `Upcoming` quando o ciclo muda via
  `LaunchedEffect(data.cycle.id) { selectedSection = HomeSection.Upcoming }`.
  (Na rotação o `cycle.id` não muda → não reseta; ao trocar de ciclo, reseta.)
- No `LazyColumn`, após o card "onde meu dinheiro vai":
  - `item { HomeSectionTabs(selected = selectedSection, onSelect = { selectedSection = it }, overdueCount = overdue.size) }`
  - `when (selectedSection)`:
    - `Upcoming` → `items(nextDue) { HomeListRow(...) }` ou empty state.
    - `Recent`   → `items(recentDaily) { HomeListRow(title=label, subtitle=categoryName, amount=amount, isoDate=date, status=null) }` ou empty state.
    - `Overdue`  → `items(overdue) { HomeListRow(...) }` ou empty state.
- Os helpers `collectNextDue`/`collectOverdue` permanecem. Adicionar uma projeção
  `DailyExpenseResponse.toRow()` (título=label, subtítulo=categoryName,
  valor=amount, data=date) e o helper de ordenação desc.

### `HomeUiState.Content` (modificar)

`app/src/main/java/com/billfolder/android/ui/screens/home/HomeViewModel.kt`

Adicionar `val recentDailyExpenses: List<DailyExpenseResponse> = emptyList()`.
Populado em `load()`, `pullRefresh()` e `navigate()`.

## Estados vazios (por aba)

Cada aba renderiza um empty state curto e específico quando sua lista está vazia
(strings novas em `strings.xml`, estilo lowercase informal):

- Próximos: "nenhuma conta a vencer nesse ciclo"
- Últimas: "nenhuma despesa avulsa nesse ciclo"
- Atrasadas: "nada atrasado 🎉"

## Testes

- **Helper puro de avulsas** (ordenação desc por data + projeção pra row): teste
  unitário direto (sem Compose), cobrindo ordem e mapeamento de campos.
- **VM**: teste com `FakeBillFolderApi` + `MainDispatcherRule` confirmando que
  `load()` popula `recentDailyExpenses` a partir de `getDailyExpenses`, e que uma
  falha no fetch das avulsas cai em lista vazia sem derrubar o `Content`.
- Sem testes de Compose UI (o projeto não tem essa infra); a lógica de seleção de
  aba é UI e fica coberta manualmente no E2E.
- Verde: `./gradlew :app:testDebugUnitTest` + `./gradlew :app:assembleDebug`.

## Fora de escopo

- Mudança no backend / `HomeResponse` (usamos o endpoint de avulsas existente).
- Limite de contagem nas "Últimas" (mostramos todas do ciclo).
- Ações nas linhas da Home (tap-to-edit/pay) — seguem na `DailyExpensesScreen`.
- Persistir a aba selecionada entre sessões (só sobrevive à recomposição/rotação
  via `rememberSaveable`).

## Deploy

Só app. Sem migration, sem push de backend. Instala com
`./gradlew :app:installDebug`.
