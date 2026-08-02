# Despesa recorrente mensal — Plano

Ref: spec `docs/superpowers/specs/2026-07-08-monthly-recurring-expense-design.md`.
TDD onde há lógica. Backend commita local (push depois); app `installDebug`.

## Backend (BillFolder)

### B1 — Rename + helper de data compartilhado
- Renomear `ProvisionedExpenseExpansion` → `ExpenseRecurrenceExpansion` (arquivo,
  classe, call sites: `CyclesService` L180/L282, `ExpenseRecurrencesService`
  L102, e testes que citam a classe).
- Extrair `MonthlyDateInRange` + `ClampedDate` de `CardEntryRecurrenceExpansion`
  pra `RecurrenceDates` (static, namespace Recurrences); card e expense usam.
  Redirecionar os testes existentes do card.
- Verde: `dotnet build`/`dotnet test`. Commit.

### B2 — Motor materializa Monthly (não-retroativo)
- [ ] RED: teste do predicado + do ramo Monthly.
  - `ShouldMaterializeMonthly(dueDate, notBefore)`: null→true; `<`→false; `>=`→true.
  - Monthly materializa 1 `Expense` comum (occurrence fields null/0) no DueDay do
    ciclo; clamp dia 31; idempotência; fora do range não gera.
- [ ] GREEN:
  - `ExpandForCycleAsync`/`ExpandForTemplateAsync`: remover filtro `== Weekly`
    (pegar Weekly+Monthly ativos no ciclo). `MaterializeAsync` recebe
    `DateOnly? notBefore`; Template passa `DateOnly.FromDateTime(DateTime.UtcNow.Date)`,
    Cycle passa `null`.
  - `MaterializeAsync` ramifica por `Frequency`:
    - Weekly → lógica atual.
    - Monthly:
      ```
      var due = RecurrenceDates.MonthlyDateInRange(effStart, effEnd, recurrence.DueDay ?? 1);
      if (due is not { } d) return;
      if (!ShouldMaterializeMonthly(d, notBefore)) return;
      // idempotência (user, template, d) → Add Expense comum:
      ExpectedAmount = DefaultAmount; OccurrenceAmount = null; OccurrencesTotal = null;
      OccurrencesPaid = 0; PaidToDate = 0; Status = Pending; TemplateId = recurrence.Id;
      Label = DefaultLabel; CategoryId = DefaultCategoryId; DueDate = d;
      ```
  - `internal static bool ShouldMaterializeMonthly(DateOnly d, DateOnly? notBefore) => notBefore is null || d >= notBefore;`
- [ ] Verde + commit.

### B3 — ExpenseResponse.TemplateId
- `ExpenseDtos.cs`: adicionar `Guid? TemplateId` no record; `MapToResponse`
  (`ExpensesService`) emite `e.TemplateId`. Verde + commit.

## App (BillFolderApp)

### A1 — templateId + isRecurring()
- `data/dto/ExpenseDtos.kt`: `@SerialName("templateId") val templateId: String? = null`.
- `ui/screens/expenses/ProvisionedExpense.kt`: `fun ExpenseResponse.isRecurring() = templateId != null`.
- Compila. Commit.

### A2 — Toggle "repetir todo mês" (default ON) no AddExpense
Espelha `AddCardEntryViewModel`/`AddCardEntrySheet`.
- [ ] RED: `AddExpenseViewModelTest` — `repeatMonthly=true` (default) chama
  `expenseRecurrencesRepository.create` com `frequency="monthly"` e `dueDay`=dia
  do dueDate; `repeatMonthly=false` chama `createExpense`. (Hooks no FakeApi:
  `onCreateExpenseRecurrence` já existe? senão add.)
- [ ] GREEN:
  - `AddExpenseViewModel`: injetar `ExpenseRecurrencesRepository`; form state
    `repeatMonthly: Boolean = true` + `onRepeatMonthlyChange`; no submit (só
    `existing == null`): se `repeatMonthly` → `expenseRecurrencesRepository.create(
    CreateExpenseRecurrenceRequest(defaultLabel=label, defaultAmount=amount,
    defaultCategoryId=categoryId, frequency="monthly", dueDay=dueDate.takeLast(2).toInt(),
    weekday=null, startDate=dueDate))`; senão `createExpense(...)` (atual).
  - `AddExpenseSheet`: `Switch` "repetir todo mês" (default on), só quando
    `existing == null`. String `add_expense_repeat_monthly`. Reusar visual do
    toggle do `AddCardEntrySheet`.
- [ ] Verde + commit.

### A3 — Modal de escopo no delete pra recorrente
- `ExpensesScreen`: no branch do `pendingDelete`, trocar `pending.isProvisioned()`
  → `pending.isRecurring()` (mantém o resto igual). Provisionada tem template →
  segue coberta. Compila. Commit.

## Verificação final
- `dotnet test` (backend) + `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- E2E: criar despesa com toggle on (venc. futuro) → aparece no ciclo; venc. já
  passado → não aparece este mês; virar ciclo → gera o do mês novo; swipe-delete
  → modal "só esta / esta e as próximas".

## Deploy
Backend `git push` (sem migration). App `installDebug`.
