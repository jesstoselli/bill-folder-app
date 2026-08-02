# Despesa recorrente mensal — Design

**Data:** 2026-07-08
**Repos:** BillFolder (backend .NET) + BillFolderApp (Android)

## Contexto

A seção "Despesas" é pra contas fixas mensais (aluguel, internet, academia).
Hoje cada despesa é um lançamento **único** — não repete. Quando o ciclo vira,
só as **provisionadas semanais** (terapia/diarista) e as **assinaturas de
cartão** são geradas automaticamente; as contas fixas comuns não, porque
**recorrência mensal de despesa comum nunca foi implementada**:

- O motor `ProvisionedExpenseExpansion` só materializa `Frequency == Weekly`
  (early-return na linha 31 e filtro na 60).
- O app só cria recorrência de despesa via o sheet semanal (`frequency="weekly"`).

A entidade `ExpenseRecurrence` já suporta `Frequency = Monthly` + `DueDay`, e o
`ExpenseRecurrenceRequestValidator` já aceita monthly exigindo `DueDay`. Falta o
**motor expandir Monthly** e o **app criar** recorrências mensais.

## Decisão de produto (travada com a usuária)

> **Toda despesa comum é recorrente mensal por padrão.** Ao criar uma despesa,
> ela nasce com "repetir todo mês" **ligado** e passa a se gerar sozinha a cada
> ciclo, vencendo num dia fixo do mês. Despesas de uma vez só (ex: psiquiatra
> mês-sim-mês-não) são criadas com o toggle **desligado**.

- **Modelo:** despesa **comum** (sem reserva/parcial), 1 por ciclo, vencendo no
  `DueDay`, paga pelo fluxo normal. É o motor do recebimento mensal / assinatura
  de cartão, produzindo uma `Expense` comum.
- **Criar:** toggle "repetir todo mês" no `AddExpenseSheet`, **ligado por
  padrão** (espelha o toggle da assinatura de cartão).
- **Encerrar:** delete com escopo ("só esta" / "esta e as próximas"), que o
  backend já faz genérico por `TemplateId`.

### Fora do v1

- **Editar valor com escopo** (aluguel subiu → "esta e as próximas"): v2. No v1,
  editar mexe só na despesa do mês atual.
- **Backfill** das despesas já existentes: v1 não converte retroativamente. A
  usuária recadastra as fixas uma vez com o toggle ligado.

## Backend (BillFolder)

### 1. Motor: expandir Monthly

Renomear `ProvisionedExpenseExpansion` → **`ExpenseRecurrenceExpansion`** (o nome
"provisioned" ficou impreciso; alinha com `CardEntryRecurrenceExpansion` /
`IncomeSourceExpansion`). Atualizar os call sites:
`CyclesService` (linhas 180, 282), `ExpenseRecurrencesService` (linha 102) e os
testes que referenciam a classe.

- `ExpandForCycleAsync` / `ExpandForTemplateAsync`: **remover o filtro
  `Frequency == Weekly`** (passam a pegar Weekly e Monthly ativos que cobrem o
  ciclo).
- `MaterializeAsync` ramifica por `Frequency`:
  - **Weekly** → despesa provisionada (lógica atual: `WeekdayDatesInRange`,
    `OccurrenceAmount`/`OccurrencesTotal`, `ExpectedAmount = DefaultAmount ×
    count`).
  - **Monthly** → despesa **comum**:
    - `DueDate` = data mensal no ciclo via helper compartilhado (dia `DueDay`
      clampado ao tamanho do mês; ex: 31 → 28/30). Se não cai no ciclo, ignora.
    - `ExpectedAmount = DefaultAmount`; campos de ocorrência **null/0**
      (`OccurrenceAmount = null`, `OccurrencesTotal = null`, `OccurrencesPaid =
      0`, `PaidToDate = 0`); `Status = Pending`; `Label`/`CategoryId` do template;
      `TemplateId = recurrence.Id`.
  - Idempotência por `(UserId, TemplateId, DueDate)` — como hoje.

- **Não-retroativa (só Monthly, só na criação do template):** ao criar a
  recorrência, **não** materializa a ocorrência do ciclo atual se o vencimento
  já passou (`DueDate < hoje`). Ex: hoje dia 20, vencimento dia 5 → pula este
  mês, começa no próximo. Quando um ciclo **novo** nasce (rollover), a ocorrência
  daquele ciclo é gerada normalmente (a recorrência já existia antes dele — não
  é retroativo). Implementação: `MaterializeAsync` recebe um parâmetro
  `DateOnly? notBefore`; `ExpandForTemplateAsync` passa `hoje`,
  `ExpandForCycleAsync` passa `null`. A regra só se aplica ao ramo Monthly
  (Weekly/provisionada segue gerando o ciclo atual como hoje). A decisão fica num
  predicado puro testável: `ShouldMaterializeMonthly(dueDate, notBefore) =
  notBefore is null || dueDate >= notBefore`.

### 2. Helper de data mensal compartilhado

Extrair a lógica `MonthlyDateInRange` + `ClampedDate` (hoje em
`CardEntryRecurrenceExpansion`) pra um helper estático compartilhado no namespace
`Recurrences` (ex: `RecurrenceDates`), usado pelo card e pelo expense expander.
Manter os testes existentes (redirecionar as chamadas).

### 3. `ExpenseResponse.TemplateId`

Expor `Guid? TemplateId` no `ExpenseResponse` + `MapToResponse` (`ExpensesService`),
pra o app saber que a despesa é recorrente (mostrar o modal de escopo no delete).
Espelha o que já foi feito no `CardEntryResponse`.

### 4. Nada a mudar

DTO `CreateExpenseRecurrenceRequest`, validator (já aceita monthly+dueDay) e o
`DeleteAsync` com escopo (genérico por `TemplateId`) já servem.

## App (BillFolderApp)

### 5. `ExpenseResponse.templateId` + `isRecurring()`

- `data/dto/ExpenseDtos.kt`: adicionar `@SerialName("templateId") val templateId:
  String? = null`.
- `ui/screens/expenses/ProvisionedExpense.kt` (ou um helper novo): `fun
  ExpenseResponse.isRecurring(): Boolean = templateId != null`.

### 6. Toggle "repetir todo mês" no `AddExpenseSheet`

Espelha `AddCardEntryViewModel` / `AddCardEntrySheet`:

- `AddExpenseViewModel`: `repeatMonthly: Boolean = true` no form state (default
  **on**); `onRepeatMonthlyChange`. Injetar `ExpenseRecurrencesRepository`.
- No submit (**apenas no modo criar**, `existing == null`):
  - `repeatMonthly == true` → `expenseRecurrencesRepository.create(
    CreateExpenseRecurrenceRequest(defaultLabel, defaultAmount,
    defaultCategoryId, frequency = "monthly", dueDay = <dia do dueDate>, weekday =
    null, startDate = dueDate))`. Isso cria o template e expande (materializa o
    ciclo atual + futuros).
  - `repeatMonthly == false` → `createExpense(...)` (comportamento atual).
- `dueDay` = dia-do-mês extraído do `dueDate` ("yyyy-MM-dd" → dia).
- `AddExpenseSheet`: linha com `Switch` "repetir todo mês" (default on), visível
  só quando `existing == null`. Reusar o padrão visual do toggle do
  `AddCardEntrySheet`.

### 7. Modal de escopo no delete pra recorrente

Em `ExpensesScreen`, a trava do `RecurrenceScopeDialog` no delete muda de
`pending.isProvisioned()` → `pending.isRecurring()` (tem `templateId`). Assim
tanto provisionada semanal quanto mensal recorrente permitem "só esta / esta e as
próximas". `confirmDelete(scope = choice.deleteLiteral())` já existe.

### 8. Sem UI especial

A despesa mensal gerada é comum → aparece normal na Home e em Despesas. A
`ExpenseRow` só mostra progresso/reservado quando `isProvisioned()`
(occurrencesTotal != null) — uma mensal recorrente não é provisionada, então
renderiza como despesa normal. ✅

## Testes

**Backend:**
- `ExpenseRecurrenceExpansion` Monthly: materializa 1 despesa comum no
  `DueDay` do ciclo, sem campos de ocorrência; clamp de dia 31; idempotência;
  Monthly fora do range do ciclo não gera.
- `ShouldMaterializeMonthly` (predicado puro): `notBefore == null` sempre gera;
  `dueDate < notBefore` não gera; `dueDate >= notBefore` gera. (Determinístico —
  `notBefore` passado explícito, sem `DateTime.Now`.)
- Weekly segue verde (renome não muda comportamento).
- Helper de data compartilhado: testes do card seguem verdes após extração.

**App:**
- `AddExpenseViewModel`: `repeatMonthly = true` roteia pra
  `expenseRecurrencesRepository.create` com `frequency="monthly"` e `dueDay`
  correto; `repeatMonthly = false` chama `createExpense`. (Mirror do teste do
  `AddCardEntryViewModel`; adicionar hooks no `FakeBillFolderApi`.)
- `isRecurring()`: templateId != null.

Verde: `dotnet build`/`dotnet test`; `./gradlew :app:testDebugUnitTest
:app:assembleDebug`.

## Deploy

Backend com **push** (recria container). **Sem migration** — a coluna
`template_id` de `expenses` e o enum de frequency já existem; a mudança é só de
motor + projeção do DTO. App via `installDebug`.

## Notas de ordem (deploy)

O expander e o `TemplateId` do DTO são backend puro; o app depende do
`templateId` só pro modal de escopo. Retrocompat: app novo + backend antigo →
`templateId` vem null → despesa tratada como não-recorrente (degrada sem
quebrar). Backend novo + app antigo → app ignora o campo extra.
