# Card Statement Lifecycle and Payment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fechar faturas automaticamente pela data, permitir baixa com data/valor/conta na Home e em Cartões, e reutilizar a Home para pagar despesas comuns e provisionadas.

**Architecture:** O backend mantém `Paid` persistido e deriva `Open`/`Closed` de `PeriodEnd` em uma função pura compartilhada. Um endpoint dedicado persiste a baixa e a Home separa faturas reservadas de realizadas. O app carrega faturas reais, usa um sheet próprio para pagamento e um coordenador isolado para abrir os fluxos de despesas a partir da Home.

**Tech Stack:** .NET 10, EF Core, PostgreSQL 16, FluentValidation, xUnit; Kotlin, Jetpack Compose Material 3, Hilt, Retrofit, kotlinx.serialization, JUnit 4 e coroutines-test.

## Global Constraints

- Trabalhar em `main`, criar commits locais por tarefa e **não fazer push**.
- Seguir TDD estrito: escrever o teste, observar RED pela causa esperada, implementar o mínimo e observar GREEN.
- Fatura `Open` quando `today < PeriodEnd`; `Closed` quando `today >= PeriodEnd`; `Paid` sempre prevalece.
- Compra feita na data efetiva do fechamento (já ajustada a meses curtos) entra na próxima fatura; histórico já materializado não é movido.
- Somente fatura efetivamente `Closed` pode ser paga.
- Pagamento registra data, valor real e conta de origem opcional.
- A baixa deve trocar reservado por realizado sem alterar `remaining` quando valor real = total.
- Toda escrita do app passa por `DataChangeNotifier.notifyingOnSuccess`.
- Backend roda com `TreatWarningsAsErrors`, Meziantou e globalization-invariant.
- Não criar CultureInfo no backend; textos de erro em português são literais.
- Migração SQL deve ser idempotente; não há enum novo nem restart especial do Npgsql.
- Não instalar o app nem acessar produção durante a implementação.

---

## File Map

### Backend (`/Users/jesstoselli/Repos/BillFolder`)

- `CardStatementExtensions.cs`: única fonte da regra `EffectiveStatus`.
- `CardCycleCalculator.cs`: fronteira de alocação de compra no fechamento.
- `CardStatementsService.cs`: consulta por status efetivo e baixa transacional.
- `CardStatementDtos.cs` / `CardStatementValidators.cs`: contrato do pagamento.
- `CardStatement.cs` / `CardStatementConfiguration.cs`: persistência da baixa.
- `HomeService.cs` / `HomeDtos.cs`: buckets reservado/realizado e payload.
- `CardEntriesService.cs`: proteção de assinaturas pelo status efetivo.
- `db/schema.sql`, `db/migrations/20260801120000_AddCardStatementPayments.sql` e snapshot EF: schema aditivo.

### Android (`/Users/jesstoselli/Repos/BillFolderApp`)

- `CardStatementDtos.kt`: DTOs de fatura e pagamento.
- `CardStatementsRepository.kt`: leitura/escrita de faturas.
- `PayCardStatementViewModel.kt` / `PayCardStatementSheet.kt`: formulário reutilizado por Home e Cartões.
- `CardsViewModel.kt` / `CardsScreen.kt`: fatura selecionada real, badge e CTA.
- `HomePaymentViewModel.kt`: carrega o detalhe da despesa sem acoplar esse estado ao `HomeViewModel`.
- `HomePaymentTarget.kt`: decisão pura de ação por tipo/status da linha.
- `HomeScreen.kt` / `HomeListRow.kt`: linhas acionáveis e abertura dos três fluxos.
- `CardCycle.kt`: mesma fronteira temporal do backend.
- `FakeBillFolderApi.kt`: hooks determinísticos para os testes JVM.

---

### Task 1: Status efetivo e fronteira do dia de fechamento

**Files:**
- Create: `../BillFolder/src/BillFolder.Application/UseCases/Cards/CardStatementExtensions.cs`
- Modify: `../BillFolder/src/BillFolder.Application/UseCases/Cards/CardCycleCalculator.cs`
- Modify: `../BillFolder/src/BillFolder.Application/UseCases/Cards/CardEntriesService.cs`
- Create: `../BillFolder/tests/BillFolder.Api.Tests/Cards/CardStatementLifecycleTests.cs`
- Test: `../BillFolder/tests/BillFolder.Api.Tests/Cards/SubscriptionScopeDeleteTests.cs`
- Test: `../BillFolder/tests/BillFolder.Api.Tests/Cards/SubscriptionScopeRepriceTests.cs`

**Interfaces:**
- Produces: `CardStatementStatus EffectiveStatus(this CardStatement statement, DateOnly today)`.
- Produces: `CardStatementStatus EffectiveStatus(CardStatementStatus persistedStatus, DateOnly periodEnd, DateOnly today)` para projeções sem entidade completa.
- Produces: `bool MatchesEffectiveStatus(CardStatementStatus persistedStatus, DateOnly periodEnd, DateOnly today, CardStatementStatus requested)`.
- Changes: `ComputeStatementForPurchase` compara `purchaseDate` com o `PeriodEnd` candidato já clampeado.
- Changes: helpers de escopo recebem status efetivo calculado com uma data explícita.

- [ ] **Step 1: Escrever testes RED da regra temporal e da fronteira**

```csharp
[Theory]
[InlineData("2026-07-16", CardStatementStatus.Open)]
[InlineData("2026-07-17", CardStatementStatus.Closed)]
[InlineData("2026-07-18", CardStatementStatus.Closed)]
public void EffectiveStatus_derives_open_or_closed_from_period_end(
    string todayIso, CardStatementStatus expected)
{
    var status = CardStatementExtensions.EffectiveStatus(
        CardStatementStatus.Open,
        new DateOnly(2026, 7, 17),
        DateOnly.Parse(todayIso));

    Assert.Equal(expected, status);
}

[Fact]
public void EffectiveStatus_keeps_paid()
{
    var status = CardStatementExtensions.EffectiveStatus(
        CardStatementStatus.Paid,
        new DateOnly(2026, 8, 17),
        new DateOnly(2026, 7, 1));

    Assert.Equal(CardStatementStatus.Paid, status);
}

[Fact]
public void MatchesEffectiveStatus_treats_persisted_open_as_closed_on_period_end()
{
    var matches = CardStatementExtensions.MatchesEffectiveStatus(
        CardStatementStatus.Open,
        new DateOnly(2026, 7, 17),
        new DateOnly(2026, 7, 17),
        CardStatementStatus.Closed);

    Assert.True(matches);
}

[Fact]
public void Purchase_on_closing_day_goes_to_next_statement()
{
    var result = CardCycleCalculator.ComputeStatementForPurchase(
        new DateOnly(2026, 6, 17), closingDay: 17, dueDay: 25);

    Assert.Equal(new DateOnly(2026, 7, 17), result.PeriodEnd);
    Assert.Equal(new DateOnly(2026, 7, 25), result.DueDate);
}

[Fact]
public void Purchase_on_clamped_closing_day_goes_to_next_statement()
{
    var result = CardCycleCalculator.ComputeStatementForPurchase(
        new DateOnly(2026, 2, 28), closingDay: 31, dueDay: 10);

    Assert.Equal(new DateOnly(2026, 3, 31), result.PeriodEnd);
}
```

- [ ] **Step 2: Executar e confirmar RED**

Run:

```bash
cd /Users/jesstoselli/Repos/BillFolder
dotnet test --filter "FullyQualifiedName~CardStatementLifecycleTests"
```

Expected: falha de compilação porque `CardStatementExtensions` não existe e/ou o teste do dia 17 recebe a fatura de junho.

- [ ] **Step 3: Implementar a função pura e mudar a comparação**

```csharp
public static class CardStatementExtensions
{
    public static CardStatementStatus EffectiveStatus(this CardStatement statement, DateOnly today) =>
        EffectiveStatus(statement.Status, statement.PeriodEnd, today);

    public static CardStatementStatus EffectiveStatus(
        CardStatementStatus persistedStatus,
        DateOnly periodEnd,
        DateOnly today) =>
        persistedStatus == CardStatementStatus.Paid
            ? CardStatementStatus.Paid
            : today >= periodEnd
                ? CardStatementStatus.Closed
                : CardStatementStatus.Open;

    public static bool MatchesEffectiveStatus(
        CardStatementStatus persistedStatus,
        DateOnly periodEnd,
        DateOnly today,
        CardStatementStatus requested) =>
        EffectiveStatus(persistedStatus, periodEnd, today) == requested;
}
```

Em `CardCycleCalculator`, calcular primeiro
`candidatePeriodEnd = ClampDay(purchaseDate.Year, purchaseDate.Month, closingDay)`.
Usar o mês candidato somente se `purchaseDate < candidatePeriodEnd`; caso
contrário, usar o fechamento do mês seguinte. Atualizar os comentários.

Em `CardEntriesService.SubscriptionStatementStatuses`, aceitar `DateOnly today`
e mapear cada statement com `statement.EffectiveStatus(today)`. Passar um `today`
único calculado no início de cada operação de delete/reprice.

- [ ] **Step 4: Executar juntas as provas da composição de regras**

Rodar `CardStatementLifecycleTests`, `SubscriptionScopeDeleteTests` e
`SubscriptionScopeRepriceTests`. A primeira classe prova que persisted `Open`
vira efetivamente `Closed` no `PeriodEnd`; as duas classes de escopo já provam
que entradas mapeadas como `Closed` ficam fora de delete/reprice.

- [ ] **Step 5: Executar a suíte backend**

```bash
dotnet test
dotnet build --no-incremental
```

Expected: todos os testes passam; 0 warnings e 0 errors.

- [ ] **Step 6: Commit**

```bash
git add src/BillFolder.Application/UseCases/Cards/CardStatementExtensions.cs \
  src/BillFolder.Application/UseCases/Cards/CardCycleCalculator.cs \
  src/BillFolder.Application/UseCases/Cards/CardEntriesService.cs \
  tests/BillFolder.Api.Tests/Cards/CardStatementLifecycleTests.cs \
  tests/BillFolder.Api.Tests/Cards/SubscriptionScopeDeleteTests.cs \
  tests/BillFolder.Api.Tests/Cards/SubscriptionScopeRepriceTests.cs
git commit -m "fix(cards): deriva fechamento da fatura pela data"
```

---

### Task 2: Persistência e endpoint dedicado de pagamento

**Files:**
- Modify: `../BillFolder/src/BillFolder.Domain/Entities/CardStatement.cs`
- Modify: `../BillFolder/src/BillFolder.Infrastructure/Persistence/Configurations/CardStatementConfiguration.cs`
- Modify: `../BillFolder/src/BillFolder.Infrastructure/Migrations/ApplicationDbContextModelSnapshot.cs`
- Modify: `../BillFolder/src/BillFolder.Application/Dtos/Cards/CardStatementDtos.cs`
- Create: `../BillFolder/src/BillFolder.Application/Validators/Cards/CardStatementValidators.cs`
- Modify: `../BillFolder/src/BillFolder.Application/UseCases/Cards/CardStatementsService.cs`
- Modify: `../BillFolder/src/BillFolder.Api/Endpoints/CardStatementsEndpoints.cs`
- Modify: `../BillFolder/db/schema.sql`
- Create: `../BillFolder/db/migrations/20260801120000_AddCardStatementPayments.sql`
- Create: `../BillFolder/tests/BillFolder.Api.Tests/Cards/CardStatementPaymentTests.cs`

**Interfaces:**
- Produces: `PayCardStatementRequest(DateOnly PaidDate, decimal ActualAmount, Guid? PaidFromAccountId)`.
- Produces: `Task<OperationResult<CardStatementResponse>> PayAsync(Guid userId, Guid id, PayCardStatementRequest request, CancellationToken ct)`.
- Produces: `PaymentError(CardStatementStatus, DateOnly, DateOnly)`, `AccountError(Guid, Guid?, Guid?)` e `ApplyPayment(CardStatement, PayCardStatementRequest)` como helpers internos testáveis.
- Produces: `POST /v1/card-statements/{id}/pay`.
- Removes: o `PATCH /v1/card-statements/{id}` genérico e `UpdateCardStatementRequest`, evitando bypass das invariantes.

- [ ] **Step 1: Escrever testes RED das regras puras de pagamento e do validator**

```csharp
[Fact]
public void PaymentError_returns_statement_open_before_period_end()
{
    var error = CardStatementsService.PaymentError(
        CardStatementStatus.Open,
        periodEnd: new DateOnly(2026, 8, 17),
        today: new DateOnly(2026, 8, 16));

    Assert.Equal("statement_open", error);
}

[Fact]
public void PaymentError_allows_effectively_closed_statement()
{
    var error = CardStatementsService.PaymentError(
        CardStatementStatus.Open,
        periodEnd: new DateOnly(2026, 8, 17),
        today: new DateOnly(2026, 8, 17));

    Assert.Null(error);
}

[Fact]
public void PaymentError_rejects_paid_statement()
{
    var error = CardStatementsService.PaymentError(
        CardStatementStatus.Paid,
        periodEnd: new DateOnly(2026, 7, 17),
        today: new DateOnly(2026, 8, 1));

    Assert.Equal("statement_paid", error);
}

[Fact]
public void AccountError_rejects_account_from_another_user()
{
    var error = CardStatementsService.AccountError(
        userId: Guid.Parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        requestedAccountId: Guid.Parse("cccccccc-cccc-cccc-cccc-cccccccccccc"),
        accountOwnerId: Guid.Parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    Assert.Equal("invalid_account", error);
}

[Fact]
public void ApplyPayment_persists_all_payment_fields_on_entity()
{
    var statement = new CardStatement { Status = CardStatementStatus.Open };
    var request = new PayCardStatementRequest(
        new DateOnly(2026, 8, 1), 790m,
        Guid.Parse("cccccccc-cccc-cccc-cccc-cccccccccccc"));

    CardStatementsService.ApplyPayment(statement, request);

    Assert.Equal(CardStatementStatus.Paid, statement.Status);
    Assert.Equal(request.PaidDate, statement.PaidDate);
    Assert.Equal(request.ActualAmount, statement.ActualAmount);
    Assert.Equal(request.PaidFromAccountId, statement.PaidFromAccountId);
}

[Fact]
public void Validator_rejects_non_positive_amount()
{
    var result = new PayCardStatementRequestValidator().Validate(
        new PayCardStatementRequest(new DateOnly(2026, 8, 1), 0m, null));

    Assert.False(result.IsValid);
}
```

- [ ] **Step 2: Executar e confirmar RED**

```bash
dotnet test --filter "FullyQualifiedName~CardStatementPaymentTests"
```

Expected: falha de compilação por ausência de request, validator e `PaymentError`.

- [ ] **Step 3: Adicionar modelo, configuração e contrato**

Adicionar à entidade:

```csharp
public DateOnly? PaidDate { get; set; }
public decimal? ActualAmount { get; set; }
public Guid? PaidFromAccountId { get; set; }
public CheckingAccount? PaidFromAccount { get; set; }
```

Configurar `actual_amount` como `numeric(12,2)`, FK `paid_from_account_id` com
`DeleteBehavior.SetNull` e índice `ix_card_statements_paid_from_account`.

Adicionar os três campos opcionais e `PaidFromAccountName` aos response DTOs,
mantendo a mesma ordem em todas as construções. Criar:

```csharp
public sealed record PayCardStatementRequest(
    DateOnly PaidDate,
    decimal ActualAmount,
    Guid? PaidFromAccountId);
```

O validator exige `PaidDate != default` e `ActualAmount > 0m`.

- [ ] **Step 4: Implementar `PayAsync` e o endpoint**

Carregar statement com `Card`, `Installments` e `PaidFromAccount`. Quando houver
`PaidFromAccountId`, buscar a conta por id + user e passar seu owner para
`AccountError`. Validar request, ownership e estado. Em sucesso,
`ApplyPayment(statement, request)` executa:

```csharp
statement.Status = CardStatementStatus.Paid;
statement.PaidDate = request.PaidDate;
statement.ActualAmount = request.ActualAmount;
statement.PaidFromAccountId = request.PaidFromAccountId;
await _db.SaveChangesAsync(ct);
```

Mapear erros: `statement_open`, `statement_paid` e `invalid_account` para 400;
`not_found` para 404. Remover o MapPatch genérico e seu DTO.

Na mesma alteração, fazer `ListAsync` materializar as projeções do usuário,
filtrar o parâmetro `status` com `MatchesEffectiveStatus` e mapear responses com
`EffectiveStatus`. `GetAsync` também mapeia o status efetivo. Isso garante que
GET de lista, detalhe e filtros compartilhem a regra testada na Task 1.

- [ ] **Step 5: Criar SQL idempotente, atualizar schema e snapshot**

```sql
BEGIN;

ALTER TABLE card_statements ADD COLUMN IF NOT EXISTS paid_date DATE;
ALTER TABLE card_statements ADD COLUMN IF NOT EXISTS actual_amount NUMERIC(12,2);
ALTER TABLE card_statements ADD COLUMN IF NOT EXISTS paid_from_account_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_card_statements_paid_from_account'
    ) THEN
        ALTER TABLE card_statements
            ADD CONSTRAINT fk_card_statements_paid_from_account
            FOREIGN KEY (paid_from_account_id)
            REFERENCES checking_accounts(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_card_statements_paid_from_account
    ON card_statements (paid_from_account_id);

COMMIT;
```

Espelhar as colunas/FK em `db/schema.sql` e no snapshot EF sem gerar migration
automática (o snapshot possui drift conhecido de enums).

- [ ] **Step 6: Executar testes e build**

```bash
dotnet test --filter "FullyQualifiedName~CardStatementPaymentTests"
dotnet test
dotnet build --no-incremental
```

Expected: GREEN, 0 warnings, 0 errors.

- [ ] **Step 7: Commit**

```bash
git add src/BillFolder.Domain/Entities/CardStatement.cs \
  src/BillFolder.Infrastructure/Persistence/Configurations/CardStatementConfiguration.cs \
  src/BillFolder.Infrastructure/Migrations/ApplicationDbContextModelSnapshot.cs \
  src/BillFolder.Application/Dtos/Cards/CardStatementDtos.cs \
  src/BillFolder.Application/Validators/Cards/CardStatementValidators.cs \
  src/BillFolder.Application/UseCases/Cards/CardStatementsService.cs \
  src/BillFolder.Api/Endpoints/CardStatementsEndpoints.cs \
  db/schema.sql db/migrations/20260801120000_AddCardStatementPayments.sql \
  tests/BillFolder.Api.Tests/Cards/CardStatementPaymentTests.cs
git commit -m "feat(cards): baixa de pagamento da fatura"
```

---

### Task 3: Buckets financeiros e status efetivo na Home

**Files:**
- Modify: `../BillFolder/src/BillFolder.Application/Dtos/Home/HomeDtos.cs`
- Modify: `../BillFolder/src/BillFolder.Application/UseCases/Home/HomeService.cs`
- Create: `../BillFolder/tests/BillFolder.Api.Tests/Home/CardStatementBucketsTests.cs`

**Interfaces:**
- Produces: `HomeBalanceDto.PaidCardStatements` como último campo do record.
- Produces: helper puro `ComputeCardStatementBuckets` retornando `(Expected, Paid, TotalCommitted)`.
- Changes: `HomeCardStatementDto.Status` recebe status efetivo.

- [ ] **Step 1: Escrever testes RED dos buckets**

```csharp
[Fact]
public void Unpaid_statement_is_reserved_by_installment_total()
{
    var result = HomeService.ComputeCardStatementBuckets(new[]
    {
        new HomeStatementFinancialProjection(CardStatementStatus.Closed, 800m, null),
    });

    Assert.Equal(800m, result.Expected);
    Assert.Equal(0m, result.Paid);
    Assert.Equal(800m, result.TotalCommitted);
}

[Fact]
public void Paid_statement_uses_actual_amount()
{
    var result = HomeService.ComputeCardStatementBuckets(new[]
    {
        new HomeStatementFinancialProjection(CardStatementStatus.Paid, 800m, 790m),
    });

    Assert.Equal(0m, result.Expected);
    Assert.Equal(790m, result.Paid);
    Assert.Equal(790m, result.TotalCommitted);
}
```

- [ ] **Step 2: Executar e confirmar RED**

```bash
dotnet test --filter "FullyQualifiedName~CardStatementBucketsTests"
```

Expected: falha de compilação por ausência da projection/helper.

- [ ] **Step 3: Implementar projection, buckets e payload**

Projetar `PeriodEnd`, `ActualAmount` e status persistido. Após materializar,
calcular o status efetivo uma única vez por item e montar:

```csharp
internal sealed record HomeStatementFinancialProjection(
    CardStatementStatus Status,
    decimal Total,
    decimal? ActualAmount);

internal readonly record struct CardStatementBuckets(
    decimal Expected,
    decimal Paid,
    decimal TotalCommitted);
```

`Expected` soma `Total` de não pagas; `Paid` soma
`ActualAmount ?? Total` das pagas; `TotalCommitted = Expected + Paid`.

Adicionar `PaidCardStatements` ao `HomeBalanceDto`; usar `TotalCommitted` no
`remaining`; mapear `EffectiveStatus` em `HomeCardStatementDto`.

- [ ] **Step 4: Executar testes e build**

```bash
dotnet test --filter "FullyQualifiedName~CardStatementBucketsTests"
dotnet test
dotnet build --no-incremental
```

- [ ] **Step 5: Commit**

```bash
git add src/BillFolder.Application/Dtos/Home/HomeDtos.cs \
  src/BillFolder.Application/UseCases/Home/HomeService.cs \
  tests/BillFolder.Api.Tests/Home/CardStatementBucketsTests.cs
git commit -m "fix(home): separa faturas reservadas e realizadas"
```

---

### Task 4: Contrato Android, repositórios e fake

**Files:**
- Create: `app/src/main/java/com/billfolder/android/data/dto/CardStatementDtos.kt`
- Modify: `app/src/main/java/com/billfolder/android/data/dto/HomeDtos.kt`
- Modify: `app/src/main/java/com/billfolder/android/data/api/BillFolderApi.kt`
- Create: `app/src/main/java/com/billfolder/android/data/repository/CardStatementsRepository.kt`
- Modify: `app/src/main/java/com/billfolder/android/data/repository/ExpensesRepository.kt`
- Modify: `app/src/test/java/com/billfolder/android/testutil/FakeBillFolderApi.kt`
- Modify: `app/src/test/java/com/billfolder/android/data/repository/RepositoryNotifyTest.kt`

**Interfaces:**
- Produces: `CardStatementResponse`, `PayCardStatementRequest`.
- Produces: `CardStatementsRepository.list(cardId)` e `pay(id, request)`.
- Produces: `ExpensesRepository.get(id)`.
- Adds: `HomeBalanceDto.paidCardStatements` com default `0.0`.

- [ ] **Step 1: Escrever testes RED de repositório**

```kotlin
@Test
fun `payCardStatement dispara bump e propaga request`() = runTest {
    val expected = statement(status = "paid")
    api.onPayCardStatement = { id, request ->
        assertEquals("statement-1", id)
        assertEquals("2026-08-01", request.paidDate)
        assertEquals(800.0, request.actualAmount, 0.001)
        expected
    }
    val versions = mutableListOf<Long>()
    val job = launch { notifier.version.drop(1).take(1).toList(versions) }

    val result = repository.pay(
        "statement-1",
        PayCardStatementRequest("2026-08-01", 800.0, "account-1"),
    )

    assertEquals(expected, result)
    assertEquals(listOf(1L), versions)
    job.cancel()
}
```

Adicionar teste separado de `ExpensesRepository.get("expense-1")` confirmando
que retorna o detalhe configurado no fake sem disparar bump.

- [ ] **Step 2: Executar e confirmar RED**

```bash
./gradlew :app:testDebugUnitTest --tests '*RepositoryNotifyTest*'
```

Expected: falha de compilação por DTOs/endpoints/repositório ausentes.

- [ ] **Step 3: Implementar DTOs, API, repositórios e fake**

```kotlin
@Serializable
data class PayCardStatementRequest(
    @SerialName("paidDate") val paidDate: String,
    @SerialName("actualAmount") val actualAmount: Double,
    @SerialName("paidFromAccountId") val paidFromAccountId: String? = null,
)

@Serializable
data class CardStatementResponse(
    val id: String,
    val cardId: String,
    val cardName: String,
    val periodStart: String,
    val periodEnd: String,
    val dueDate: String,
    val status: String,
    val totalAmount: Double,
    val installmentsCount: Int,
    val paidDate: String? = null,
    val actualAmount: Double? = null,
    val paidFromAccountId: String? = null,
    val paidFromAccountName: String? = null,
    val linkedExpenseId: String? = null,
    val createdAt: String,
    val updatedAt: String,
)
```

Retrofit:

```kotlin
@GET("card-statements")
suspend fun getCardStatements(@Query("cardId") cardId: String? = null): List<CardStatementResponse>

@POST("card-statements/{id}/pay")
suspend fun payCardStatement(
    @Path("id") id: String,
    @Body request: PayCardStatementRequest,
): CardStatementResponse

@GET("expenses/{id}")
suspend fun getExpense(@Path("id") id: String): ExpenseResponse
```

O `pay` usa `notifyingOnSuccess`; reads não notificam. Adicionar hooks, listas de
calls e overrides equivalentes no fake.

- [ ] **Step 4: Executar testes e assemble**

```bash
./gradlew :app:testDebugUnitTest --tests '*RepositoryNotifyTest*'
./gradlew :app:assembleDebug
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/billfolder/android/data/dto/CardStatementDtos.kt \
  app/src/main/java/com/billfolder/android/data/dto/HomeDtos.kt \
  app/src/main/java/com/billfolder/android/data/api/BillFolderApi.kt \
  app/src/main/java/com/billfolder/android/data/repository/CardStatementsRepository.kt \
  app/src/main/java/com/billfolder/android/data/repository/ExpensesRepository.kt \
  app/src/test/java/com/billfolder/android/testutil/FakeBillFolderApi.kt \
  app/src/test/java/com/billfolder/android/data/repository/RepositoryNotifyTest.kt
git commit -m "feat(data): contrato e repositório de pagamento de fatura"
```

---

### Task 5: Sheet e ViewModel de pagamento da fatura

**Files:**
- Create: `app/src/main/java/com/billfolder/android/ui/screens/cards/PayCardStatementViewModel.kt`
- Create: `app/src/main/java/com/billfolder/android/ui/screens/cards/PayCardStatementSheet.kt`
- Create: `app/src/test/java/com/billfolder/android/ui/screens/cards/PayCardStatementViewModelTest.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `CardStatementPaymentSummary(id, cardName, dueDate, totalAmount)` e conversores a partir de `CardStatementResponse` e `HomeCardStatementDto`.
- Produces: `PayCardStatementFormState` e `PayCardStatementViewModel`.
- Produces: `PayCardStatementSheet(summary, onDismiss, onSaved)`.

- [ ] **Step 1: Escrever testes RED do ViewModel**

Cobrir quatro casos independentes:

```kotlin
@Test fun `initializeFor preenche total e seleciona conta principal`()
@Test fun `submit rejeita valor zero sem chamar api`()
@Test fun `submit envia data valor e conta`()
@Test fun `submit permite conta nula`()
```

No caso válido, configurar `api.onPayCardStatement`, chamar:

```kotlin
vm.initializeFor(CardStatementPaymentSummary("st-1", "Nubank", "2026-08-10", 800.0))
vm.onPaidDateChange("2026-08-01")
vm.onAccountChange("acc-1")
vm.submit("valor inválido")
```

e assertar a chamada gravada e `savedSuccessfully == true`.

- [ ] **Step 2: Executar e confirmar RED**

```bash
./gradlew :app:testDebugUnitTest --tests '*PayCardStatementViewModelTest*'
```

Expected: falha de compilação porque ViewModel/form state não existem.

- [ ] **Step 3: Implementar ViewModel mínimo**

Espelhar o fluxo de `PayExpenseViewModel`: `resetForm`, `initializeFor`, handlers,
validação via `parseAmount`, carregamento de checking accounts com seleção da
principal e submit para `CardStatementsRepository.pay`.

Não assertar `LocalDate.now()` nos testes; a data alterada explicitamente é a
fonte determinística.

- [ ] **Step 4: Executar testes GREEN**

```bash
./gradlew :app:testDebugUnitTest --tests '*PayCardStatementViewModelTest*'
```

- [ ] **Step 5: Criar o sheet com componentes existentes**

Criar o resumo compartilhado no mesmo arquivo do sheet:

```kotlin
data class CardStatementPaymentSummary(
    val id: String,
    val cardName: String,
    val dueDate: String,
    val totalAmount: Double,
)
```

Usar `BillFolderTransactionSheet`, `BillFolderDateField`,
`BillFolderMoneyField`, `BillFolderDropdown` e `BillFolderPrimaryButton`.
Resumo mostra `cardName`, `formatBrl(totalAmount)` e `formatShortDate(dueDate)`.
Strings lowercase:

```xml
<string name="pay_card_statement_title">pagar fatura</string>
<string name="pay_card_statement_date">data do pagamento</string>
<string name="pay_card_statement_amount">valor real pago</string>
<string name="pay_card_statement_account">conta de origem</string>
<string name="pay_card_statement_cta">confirmar pagamento</string>
```

- [ ] **Step 6: Executar suíte e assemble**

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/billfolder/android/ui/screens/cards/PayCardStatementViewModel.kt \
  app/src/main/java/com/billfolder/android/ui/screens/cards/PayCardStatementSheet.kt \
  app/src/test/java/com/billfolder/android/ui/screens/cards/PayCardStatementViewModelTest.kt \
  app/src/main/res/values/strings.xml
git commit -m "feat(cards): sheet de pagamento da fatura"
```

---

### Task 6: Fatura real, badge e CTA na tela de Cartões

**Files:**
- Modify: `app/src/main/java/com/billfolder/android/ui/screens/cards/CardsViewModel.kt`
- Modify: `app/src/main/java/com/billfolder/android/ui/screens/cards/CardsScreen.kt`
- Modify: `app/src/test/java/com/billfolder/android/ui/screens/cards/CardsViewModelTest.kt`
- Modify: `app/src/test/java/com/billfolder/android/ui/util/CardCycleTest.kt`
- Modify: `app/src/main/java/com/billfolder/android/ui/util/CardCycle.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Adds: `CardsUiState.Content.statements: List<CardStatementResponse>`.
- Produces: `selectedStatementResponse(): CardStatementResponse?` por `cardId + dueDate`.
- UI opens: `PayCardStatementSheet` somente quando status é `closed`.

- [ ] **Step 1: Escrever testes RED da fronteira Kotlin e resolução**

```kotlin
@Test
fun `compra no dia do fechamento cai na proxima fatura`() {
    val s = stmt("2026-06-17", closingDay = 17, dueDay = 25)
    assertEquals(LocalDate.parse("2026-07-17"), s.periodEnd)
    assertEquals(LocalDate.parse("2026-07-25"), s.dueDate)
}

@Test
fun `compra no fechamento clampeado de fevereiro cai na proxima fatura`() {
    val s = stmt("2026-02-28", closingDay = 31, dueDay = 10)
    assertEquals(LocalDate.parse("2026-03-31"), s.periodEnd)
}

@Test
fun `selectedStatementResponse combina cartao e vencimento`() {
    val state = content(
        selectedCardId = "card-1",
        statements = listOf(
            statement(id = "wrong-card", cardId = "card-2", dueDate = "2026-07-25"),
            statement(id = "right", cardId = "card-1", dueDate = "2026-07-25"),
        ),
    )
    assertEquals("right", state.selectedStatementResponse()?.id)
}
```

Adicionar teste do load confirmando que cartões, entries e statements chegam ao
`Content`, e do observer confirmando refetch das três fontes.

- [ ] **Step 2: Executar e confirmar RED**

```bash
./gradlew :app:testDebugUnitTest --tests '*CardCycleTest*' --tests '*CardsViewModelTest*'
```

- [ ] **Step 3: Implementar estado, fetch e helper**

Injetar `CardStatementsRepository`; carregar cards, entries e statements em
paralelo no load/pullRefresh. O helper compara:

```kotlin
val due = currentStatement()?.dueDate?.toString() ?: return null
return statements.firstOrNull { it.cardId == selectedCardId && it.dueDate == due }
```

Calcular o `candidatePeriodEnd` clampeado em `CardCycle.kt` e usar o mês atual
somente quando `purchaseDate < candidatePeriodEnd`. Atualizar documentação.

- [ ] **Step 4: Implementar UI**

No bloco do total, obter `selectedStatementResponse()`. Se existir, mostrar
`StatusChip(statement.status)`. Se `status.equals("closed", true)`, mostrar
`BillFolderPrimaryButton("pagar fatura")` que seleciona a fatura em estado local
e abre `PayCardStatementSheet`. Em sucesso, fechar; o notifier fará o refresh.

Fatura vazia (`null`) não mostra badge nem CTA.

- [ ] **Step 5: Executar testes e assemble**

```bash
./gradlew :app:testDebugUnitTest --tests '*CardCycleTest*' --tests '*CardsViewModelTest*'
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/billfolder/android/ui/screens/cards/CardsViewModel.kt \
  app/src/main/java/com/billfolder/android/ui/screens/cards/CardsScreen.kt \
  app/src/main/java/com/billfolder/android/ui/util/CardCycle.kt \
  app/src/test/java/com/billfolder/android/ui/screens/cards/CardsViewModelTest.kt \
  app/src/test/java/com/billfolder/android/ui/util/CardCycleTest.kt \
  app/src/main/res/values/strings.xml
git commit -m "feat(cards): badge e baixa da fatura selecionada"
```

---

### Task 7: Ações de pagamento nas linhas da Home

**Files:**
- Create: `app/src/main/java/com/billfolder/android/ui/screens/home/HomePaymentTarget.kt`
- Create: `app/src/main/java/com/billfolder/android/ui/screens/home/HomePaymentViewModel.kt`
- Modify: `app/src/main/java/com/billfolder/android/ui/screens/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeListRow.kt`
- Create: `app/src/test/java/com/billfolder/android/ui/screens/home/HomePaymentTargetTest.kt`
- Create: `app/src/test/java/com/billfolder/android/ui/screens/home/HomePaymentViewModelTest.kt`

**Interfaces:**
- Produces: `HomePaymentTarget.None`, `.Expense(id)` e `.Statement(dto)`.
- Produces: `HomePaymentViewModel.loadExpense(id)`, `selectedExpense` e `dismiss()`.
- Changes: `HomeListRow(onClick: (() -> Unit)? = null)`.

- [ ] **Step 1: Escrever testes RED da decisão de ação**

```kotlin
@Test fun `despesa comum e provisionada pedem detalhe por id`() {
    assertEquals(HomePaymentTarget.Expense("e-1"), normalExpense.paymentTarget())
    assertEquals(HomePaymentTarget.Expense("e-2"), provisionedExpense.paymentTarget())
}

@Test fun `fatura fechada e acionavel`() {
    assertEquals(HomePaymentTarget.Statement(closed), closed.paymentTarget())
}

@Test fun `fatura aberta nao e acionavel`() {
    assertEquals(HomePaymentTarget.None, open.paymentTarget())
}
```

- [ ] **Step 2: Escrever teste RED do coordenador de despesa**

Configurar `api.onGetExpense`, chamar `vm.loadExpense("e-1")` e assertar que
`selectedExpense` recebe o response completo. Cobrir IOException: loading termina,
selected permanece null e `errorMessage` é preenchida.

- [ ] **Step 3: Executar e confirmar RED**

```bash
./gradlew :app:testDebugUnitTest --tests '*HomePaymentTargetTest*' --tests '*HomePaymentViewModelTest*'
```

- [ ] **Step 4: Implementar helper, coordenador e row clicável**

```kotlin
sealed interface HomePaymentTarget {
    data object None : HomePaymentTarget
    data class Expense(val id: String) : HomePaymentTarget
    data class Statement(val statement: HomeCardStatementDto) : HomePaymentTarget
}

fun HomeUpcomingExpenseDto.paymentTarget(): HomePaymentTarget =
    HomePaymentTarget.Expense(id)

fun HomeCardStatementDto.paymentTarget(): HomePaymentTarget =
    if (status.equals("closed", ignoreCase = true))
        HomePaymentTarget.Statement(this)
    else HomePaymentTarget.None
```

`HomeListRow` usa `Card(onClick=...)` somente quando callback não é null, seguindo
o padrão de `CreditCardRow`: `onClick = onClick ?: {}` e `enabled = onClick != null`.

- [ ] **Step 5: Conectar os três sheets na Home**

Ao tocar em `Expense`, chamar `HomePaymentViewModel.loadExpense`. Quando o detalhe
chegar:

- `expense.isProvisionedInProgress()` abre `PayOccurrenceSheet`;
- caso contrário abre `PayExpenseSheet`.

Ao tocar em `Statement`, converter o DTO da Home para
`CardStatementPaymentSummary` e abrir `PayCardStatementSheet` diretamente.

Aplicar a ação em Próximos e Atrasadas. `Recent` permanece read-only. Em sucesso,
fechar o sheet; notifier atualiza a Home. Mostrar erro recuperável do coordenador
sem substituir `HomeUiState.Content`.

- [ ] **Step 6: Executar testes e assemble**

```bash
./gradlew :app:testDebugUnitTest --tests '*HomePaymentTargetTest*' --tests '*HomePaymentViewModelTest*'
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/billfolder/android/ui/screens/home/HomePaymentTarget.kt \
  app/src/main/java/com/billfolder/android/ui/screens/home/HomePaymentViewModel.kt \
  app/src/main/java/com/billfolder/android/ui/screens/home/HomeScreen.kt \
  app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeListRow.kt \
  app/src/test/java/com/billfolder/android/ui/screens/home/HomePaymentTargetTest.kt \
  app/src/test/java/com/billfolder/android/ui/screens/home/HomePaymentViewModelTest.kt
git commit -m "feat(home): baixa de despesas e faturas pelas linhas"
```

---

### Task 8: Hero realizado, regressão cruzada e documentação operacional

**Files:**
- Create: `app/src/main/java/com/billfolder/android/ui/screens/home/HomeBalance.kt`
- Modify: `app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeHeroCard.kt`
- Create: `app/src/test/java/com/billfolder/android/ui/screens/home/HomeBalanceTest.kt`
- Modify: `app/src/test/java/com/billfolder/android/ui/screens/home/HomeViewModelTest.kt`
- Modify: `/Users/jesstoselli/Repos/BillFolder-Session-Handoff.md`

**Interfaces:**
- Changes: `realized = paidExpenses + paidCardStatements + dailyExpensesSpent`.
- Produces: handoff com migração e ordem de deploy.

- [ ] **Step 1: Extrair e testar cálculo puro do realizado**

Confirmar que o campo `paidCardStatements` adicionado na Task 4 permanece como o
último de `HomeBalanceDto`, com default `0.0`, preservando factories e
desserialização contra backend antigo. Criar em `HomeBalance.kt` a função:

```kotlin
fun HomeBalanceDto.realizedAmount(): Double =
    paidExpenses + paidCardStatements + dailyExpensesSpent
```

Criar `HomeBalanceTest.kt` com:

```kotlin
@Test fun `realizado inclui despesas faturas pagas e avulsas`() {
    val balance = balance(paidExpenses = 200.0, paidCardStatements = 800.0, dailyExpensesSpent = 100.0)
    assertEquals(1100.0, balance.realizedAmount(), 0.001)
}
```

- [ ] **Step 2: Executar e confirmar RED**

```bash
./gradlew :app:testDebugUnitTest --tests '*HomeBalanceTest*'
```

- [ ] **Step 3: Implementar helper e usá-lo no Hero**

Substituir a soma inline de `HomeHeroCard` por `balance.realizedAmount()`.
Atualizar factories explícitas de `HomeBalanceDto` nos testes com
`paidCardStatements = 0.0`; factories que usam o construtor com default não
precisam de alteração.

- [ ] **Step 4: Rodar verificação completa dos dois repositórios**

Backend:

```bash
cd /Users/jesstoselli/Repos/BillFolder
dotnet test
dotnet build --no-incremental
git diff --check
git status --short
```

App:

```bash
cd /Users/jesstoselli/Repos/BillFolderApp
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug
git diff --check
git status --short
```

Expected: suites totalmente verdes, backend sem warnings, app BUILD SUCCESSFUL e
somente alterações previstas.

- [ ] **Step 5: Atualizar handoff**

Registrar:

- status efetivo e comparação da compra com o fechamento efetivo clampeado;
- endpoint `/v1/card-statements/{id}/pay`;
- colunas/migração `20260801120000_AddCardStatementPayments.sql`;
- Home como central de baixa;
- `PaidCardStatements` no realizado;
- deploy backend primeiro, app depois; sem restart especial de enum.

- [ ] **Step 6: Commit app**

```bash
cd /Users/jesstoselli/Repos/BillFolderApp
git add app/src/main/java/com/billfolder/android/ui/screens/home/HomeBalance.kt \
  app/src/main/java/com/billfolder/android/ui/screens/home/components/HomeHeroCard.kt \
  app/src/test/java/com/billfolder/android/ui/screens/home/HomeBalanceTest.kt \
  app/src/test/java/com/billfolder/android/ui/screens/home/HomeViewModelTest.kt
git commit -m "fix(home): inclui faturas pagas no realizado"
```

O handoff fica fora dos dois repositórios Git; atualizá-lo no filesystem, sem
forçar sua inclusão em um commit do app ou backend.

- [ ] **Step 7: Solicitar code review antes do handoff final**

Usar `superpowers:requesting-code-review` para revisar:

- consistência do status efetivo em todas as queries;
- transição financeira reservado → realizado;
- contrato backend/app e defaults de serialização;
- ausência de bypass pelo PATCH antigo;
- migração idempotente e snapshot;
- ações da Home e impossibilidade de pagar fatura aberta.

Não fazer push nem `installDebug` nesta tarefa.
