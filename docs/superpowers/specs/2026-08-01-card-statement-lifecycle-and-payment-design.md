# Ciclo de vida e pagamento de faturas de cartão — Design

**Data:** 2026-08-01  
**App:** BillFolderApp (Kotlin / Jetpack Compose + Hilt + Retrofit)  
**Backend:** BillFolder (.NET 10 / EF Core / PostgreSQL)

## Contexto e causa raiz

Toda `CardStatement` é criada com `Status = Open`. O backend só devolve o valor
persistido e não há regra que promova a fatura para `Closed` quando sua data de
fechamento chega. Existe um `PATCH /v1/card-statements/{id}` genérico, mas o app
não possui integração nem interface para utilizá-lo.

Por isso:

- o badge permanece "aberta" indefinidamente;
- não há baixa de pagamento no app;
- a Home exibe faturas como linhas somente de leitura;
- faturas não aparecem em Despesas, pois `CardStatement.LinkedExpenseId` existe
  no modelo, mas nunca é preenchido.

O vínculo com `Expense` não será ativado. Fatura continuará sendo uma entidade
financeira própria, evitando duplicidade de obrigação e risco de contagem dupla.

## Objetivos

1. Exibir automaticamente o status correto (`Open`, `Closed` ou `Paid`).
2. Permitir pagar uma fatura fechada, registrando data, valor real e conta de
   origem.
3. Disponibilizar a baixa tanto na Home quanto na tela de Cartões.
4. Permitir também, pela Home, pagar despesas comuns e dar baixa em ocorrências
   semanais provisionadas.
5. Manter saldo disponível, reservado e realizado financeiramente coerentes.
6. Usar a mesma fronteira de fechamento em status, alocação de compras,
   navegação do app e proteções de assinaturas.

## Decisões aprovadas

- Implementar **status efetivo derivado por data**, sem job ou scheduler.
- Uma fatura está:
  - `Open` quando `today < PeriodEnd`;
  - `Closed` quando `today >= PeriodEnd`, desde que não esteja paga;
  - `Paid` quando a baixa foi registrada.
- O próprio dia de fechamento já é um dia de fatura fechada.
- Uma compra feita no dia do fechamento pertence à **próxima fatura**. A regra
  de alocação passa de `purchaseDay <= closingDay` para
  `purchaseDay < closingDay`.
- A mudança de fronteira afeta apenas lançamentos materializados depois do
  deploy; parcelas históricas permanecem nas faturas existentes.
- A baixa só pode ocorrer depois do fechamento.
- A Home e a tela de Cartões abrem o mesmo fluxo de pagamento de fatura.
- O pagamento registra data, valor real e conta corrente de origem.

## Alternativas descartadas

### Materializar a fatura como despesa

Reutilizaria o fluxo da tela de Despesas e o campo `LinkedExpenseId`, mas criaria
duas representações para a mesma obrigação, sincronização bidirecional e risco de
desconto duplicado na Home.

### Job diário para persistir `Open -> Closed`

Manteria o status totalmente materializado no banco, mas adicionaria scheduler,
reprocessamento e pontos de falha para uma regra determinística baseada apenas
em datas.

## Backend

### Status efetivo

Criar uma função pura e centralizada, conceitualmente:

```text
EffectiveStatus(persistedStatus, periodEnd, today):
  if persistedStatus == Paid => Paid
  if today >= periodEnd       => Closed
  else                        => Open
```

O status `Closed` será derivado. No banco, uma fatura não paga pode continuar
armazenada como `Open`; todas as fronteiras que expõem ou usam status devem
consultar o status efetivo:

- listagem e detalhe de faturas;
- payload da Home;
- filtro `status` do endpoint de faturas;
- validação de pagamento;
- seleção das ocorrências de assinatura que podem ser editadas ou excluídas.

Assim, uma assinatura em fatura já fechada não poderá ser alterada apenas porque
o valor persistido ainda é `Open`.

### Fronteira de alocação

`CardCycleCalculator.ComputeStatementForPurchase` passa a enviar compras feitas
no `closingDay` para o período seguinte. O helper equivalente no app deve usar a
mesma comparação, mantendo a navegação e o backend alinhados.

Faturas e parcelas já existentes não serão recalculadas ou movidas por
migração.

### Dados de pagamento

Adicionar a `CardStatement`:

- `PaidDate: DateOnly?`;
- `ActualAmount: decimal?` (`numeric(12,2)`);
- `PaidFromAccountId: Guid?`, FK para `CheckingAccount`, com `ON DELETE SET NULL`.

Os DTOs de lista, detalhe e Home expõem os dados necessários. O nome da conta
pode ser denormalizado nas respostas de detalhe/lista quando útil ao app.

### Endpoint de pagamento

Adicionar uma operação específica:

```http
POST /v1/card-statements/{id}/pay
```

Payload:

```json
{
  "paidDate": "2026-08-01",
  "actualAmount": 1234.56,
  "paidFromAccountId": "uuid-ou-null"
}
```

Regras:

- a fatura deve pertencer ao usuário;
- o status efetivo deve ser `Closed`;
- fatura `Open` retorna erro de domínio claro;
- fatura `Paid` não aceita uma segunda baixa;
- `actualAmount` deve ser maior que zero;
- a conta, quando informada, deve pertencer ao usuário;
- sucesso persiste `Status = Paid` e os três dados da baixa;
- a resposta devolve a fatura atualizada.

O `PATCH` genérico existente não será usado pelo app para pagamento. Sua
superfície deve ser restringida ou preservada apenas de forma compatível, sem
permitir contornar as invariantes do novo endpoint.

### Home e semântica financeira

Separar faturas do ciclo em:

- **reservadas:** faturas não pagas, pelo total das parcelas;
- **realizadas:** faturas pagas, por `ActualAmount ?? TotalAmount`.

Adicionar `PaidCardStatements` a `HomeBalanceDto`. A fórmula fica
conceitualmente:

```text
remaining = entradas
          - despesas pendentes
          - despesas pagas
          - faturas reservadas
          - faturas realizadas
          - avulsas
          - ajustes de saída
```

Dar baixa não deve alterar o saldo se o valor real for igual ao total: apenas
move o valor de reservado para realizado. Se o valor real diferir, `remaining`
passa a refletir o desembolso real. O hero soma `PaidCardStatements` ao
"realizado".

O breakdown por categoria continua baseado nas parcelas, pois uma diferença no
pagamento da fatura não possui categoria confiável para rateio.

### Migração

Criar migração idempotente para as três colunas, FK e índice necessários. Não há
enum novo; portanto, não existe a exigência especial de reiniciar o container
por cache de enum do Npgsql.

## App Android

### Contrato e repositório

Adicionar DTOs de fatura e de `PayCardStatementRequest`, endpoints Retrofit e um
repositório de faturas (ou uma seção claramente delimitada no repositório de
cartões). Escritas passam por `DataChangeNotifier.notifyingOnSuccess`, fazendo
Home e Cartões atualizarem após a baixa.

Adicionar leitura de despesa por id para que a Home obtenha o `ExpenseResponse`
completo somente quando a usuária tocar na linha. Isso permite reutilizar os
sheets existentes sem ampliar excessivamente o `HomeResponse`.

### Home como central de baixas

As linhas financeiras das abas Próximos e Atrasadas ganham ação de acordo com o
tipo:

- despesa comum pending/overdue: abre `PayExpenseSheet`;
- despesa provisionada em andamento: abre `PayOccurrenceSheet`;
- fatura `Open`: somente informativa;
- fatura `Closed`: abre `PayCardStatementSheet`;
- itens pagos continuam fora de Próximos.

Enquanto o detalhe da despesa é carregado, a tela mostra progresso discreto; uma
falha não fecha nem derruba a Home e apresenta mensagem recuperável.

### Tela de Cartões

O `CardsViewModel` passa a carregar as faturas reais, além de cartões e entries.
A fatura selecionada é resolvida pelo `cardId + dueDate`, evitando inventar um
status apenas no cliente.

Quando o período selecionado não possui uma `CardStatement` materializada (fatura
vazia), não há badge nem ação de pagamento.

Próximo ao total da fatura, mostrar `StatusChip` com `aberta`, `fechada` ou
`paga`. Quando `Closed`, mostrar ação "pagar fatura". `Open` e `Paid` não exibem
a ação.

### `PayCardStatementSheet`

Novo bottom sheet no mesmo padrão de `PayExpenseSheet`:

- resumo com nome do cartão, vencimento e total;
- data do pagamento, preenchida com hoje;
- valor real pago, preenchido com o total;
- conta de origem opcional, pré-selecionando a conta principal;
- CTA "confirmar pagamento";
- erro inline e estado de loading;
- sucesso fecha o sheet e deixa o refresh bus atualizar as telas.

## Erros e compatibilidade

- Campos novos opcionais nos DTOs do app recebem defaults quando isso favorecer
  compatibilidade durante deploy desencontrado.
- O backend retorna erros distintos para fatura aberta, já paga, conta inválida,
  valor inválido e fatura inexistente.
- A UI mantém o sheet aberto em falhas de rede/domínio e exibe mensagem.
- A ordem segura de deploy é backend primeiro, app depois. A mudança de banco é
  aditiva.

## Testes

### Backend

- status efetivo na véspera, no dia e depois do fechamento;
- `Paid` permanece pago independentemente da data;
- filtro de status usa o valor efetivo;
- compra na véspera fica na fatura atual;
- compra no dia do fechamento vai para a próxima;
- pagamento de fatura fechada persiste data, valor e conta;
- pagamento de aberta e pagamento duplicado são rejeitados;
- conta de outro usuário é rejeitada;
- reservado/realizado e `remaining` não saltam quando valor real = total;
- valor real diferente altera `remaining` pela diferença;
- assinatura em fatura efetivamente fechada fica protegida de delete/reprice.

### App

- DTO/repositório propagam pagamento e disparam o refresh bus;
- ViewModel do sheet valida valor e monta o request correto;
- resolução da fatura selecionada por cartão + vencimento;
- ações da Home distinguem despesa comum, provisionada e fatura;
- fatura aberta não é acionável e fechada abre o sheet;
- helper de ciclo trata compra no dia de fechamento como próxima fatura;
- Home inclui faturas pagas no realizado.

Verificação final:

```bash
dotnet build
dotnet test
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug
```

## Fora de escopo

- pagamento parcial de uma mesma fatura em múltiplas baixas;
- estorno ou reabertura de fatura paga;
- conciliação bancária automática;
- mover parcelas históricas após a mudança da fronteira do dia de fechamento;
- transformar fatura em `Expense` ou exibi-la na tela de Despesas;
- scheduler/background job para fechamento.

## Deploy

1. Aplicar a migração aditiva no banco.
2. Fazer push/deploy do backend.
3. Instalar o app atualizado com `./gradlew :app:installDebug`.

Como não há enum novo, não é necessário o restart adicional do container por
cache de tipos do Npgsql.
