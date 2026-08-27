# Feature Specification: Sistema de Cashback em Compras

**Feature Branch**: `001-cashback-purchase-flow`

**Created**: 2026-08-27

**Status**: Draft

**Input**: User description: "Sistema de cashback em compras, com 3 microserviços: compras-service registra compras, cancelamentos e devoluções e dispara cálculo/estorno de cashback; cashback-service calcula cashback por regras de categoria com piso e teto, e repassa crédito ou estorno; carteira-service mantém saldo, histórico e permite resgate, podendo ficar negativo quando um estorno chega após o resgate já ter ocorrido."

## Clarifications

### Session 2026-08-27

- Q: Se cashback-service ou carteira-service estiver indisponível quando compras-service tentar disparar o cálculo/crédito de cashback (ou o estorno), a compra (ou cancelamento/devolução) deve ser registrada mesmo assim e o passo de cashback repetido depois, ou a operação inteira deve falhar para o chamador tentar de novo? → A: A compra/cancelamento/devolução é registrada imediatamente independentemente da disponibilidade dos serviços seguintes; o passo de cálculo/crédito ou estorno de cashback é repetido até ter sucesso, podendo existir uma janela em que o cashback fica "pendente".
- Q: Quem pode registrar um cancelamento ou devolução de uma compra — apenas o próprio usuário final, ou também um processo interno de atendimento/operações agindo em nome dele? → A: Tanto o próprio usuário final quanto um processo interno de atendimento/operações podem registrar um cancelamento ou devolução.
- Q: Uma vez registrada a compra (e sem indisponibilidade de serviço a bloquear o cashback), em quanto tempo o cashback deve aparecer creditado no saldo do usuário? → A: Em poucos segundos (quase em tempo real), sob operação normal.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ganhar cashback em uma compra elegível (Priority: P1)

Um usuário faz uma compra. Se o valor da compra atingir o mínimo exigido, o sistema
calcula o cashback de acordo com a categoria da compra e credita esse valor no saldo do
usuário automaticamente, sem qualquer ação manual do usuário.

**Why this priority**: É o motivo de existir do produto — sem crédito automático de
cashback não há proposta de valor. Todo o resto (estorno, resgate) só faz sentido em cima
de um crédito que já aconteceu.

**Independent Test**: Registrar uma compra acima do valor mínimo em uma categoria com
regra definida e verificar que o saldo do usuário aumenta no valor correto, sem depender
de nenhum outro fluxo.

**Acceptance Scenarios**:

1. **Given** um usuário sem saldo de cashback, **When** ele faz uma compra de R$100 em
   uma categoria com regra de 5%, **Then** o sistema credita R$5 no saldo do usuário.
2. **Given** uma compra em uma categoria com regra de 2% e valor de R$5.000, **When** o
   cashback calculado (R$100) ultrapassa o teto máximo de R$50 por compra, **Then** o
   sistema credita apenas R$50.
3. **Given** uma compra abaixo do valor mínimo configurado, **When** ela é registrada,
   **Then** nenhum cashback é calculado nem creditado.
4. **Given** uma categoria sem regra específica cadastrada, **When** uma compra é
   registrada nessa categoria, **Then** o sistema aplica a regra geral de cashback.

---

### User Story 2 - Resgatar o saldo de cashback acumulado (Priority: P2)

Um usuário consulta seu saldo de cashback e histórico de movimentações, e solicita o
resgate/saque do saldo disponível.

**Why this priority**: É o momento em que o cashback creditado vira valor de fato
percebido pelo usuário. Sem essa etapa, o crédito da User Story 1 é apenas um número.

**Independent Test**: Com um saldo positivo já existente, solicitar o resgate e verificar
que o valor resgatado é debitado do saldo e aparece no extrato como uma movimentação de
resgate.

**Acceptance Scenarios**:

1. **Given** um usuário com saldo positivo de R$30, **When** ele solicita o resgate do
   saldo total, **Then** o saldo disponível passa a R$0 e o extrato registra o resgate de
   R$30.
2. **Given** um usuário consultando seu extrato, **When** a consulta é feita, **Then** o
   sistema mostra todos os créditos, estornos e resgates em ordem cronológica, incluindo
   qualquer saldo devedor existente.
3. **Given** um usuário com saldo zero ou negativo, **When** ele tenta resgatar, **Then**
   o sistema recusa o resgate por não haver saldo disponível para saque.

---

### User Story 3 - Estorno de cashback por cancelamento total de compra (Priority: P3)

O próprio usuário, ou um processo interno de atendimento/operações agindo em nome dele,
cancela integralmente uma compra já registrada. O cashback que havia sido creditado por essa compra é estornado do saldo do
usuário, mesmo que isso deixe o saldo negativo.

**Why this priority**: Cancelamento é o caso de reversão mais simples (tudo ou nada) e
precisa existir para que o saldo nunca fique inflado por compras desfeitas — mas depende
de já existir um crédito (User Story 1) para reverter.

**Independent Test**: Com uma compra já creditada, registrar seu cancelamento total e
verificar que o valor integral do cashback creditado é debitado do saldo, gerando saldo
negativo se o valor já havia sido resgatado.

**Acceptance Scenarios**:

1. **Given** uma compra que gerou R$5 de cashback já creditado e ainda não resgatado,
   **When** a compra é cancelada integralmente, **Then** os R$5 são estornados do saldo do
   usuário.
2. **Given** uma compra que gerou R$5 de cashback já resgatado pelo usuário, **When** a
   compra é cancelada integralmente, **Then** o saldo do usuário fica negativo em R$5,
   essa dívida é visível no extrato, e o cancelamento não é bloqueado pelo fato de o
   cashback já ter sido resgatado.
3. **Given** uma compra que não atingiu o valor mínimo e não gerou cashback, **When** ela
   é cancelada, **Then** nenhum estorno é gerado.

---

### User Story 4 - Estorno proporcional de cashback por devolução parcial (Priority: P4)

O próprio usuário, ou um processo interno de atendimento/operações agindo em nome dele,
devolve parte dos itens de uma compra já registrada. O sistema estorna do saldo
do usuário a parcela do cashback proporcional ao percentual devolvido da compra, podendo
também deixar o saldo negativo.

**Why this priority**: É uma variação mais específica do estorno (proporcional em vez de
total) e depende da mesma base de cálculo e crédito das histórias anteriores.

**Independent Test**: Com uma compra já creditada, registrar uma devolução parcial de um
percentual conhecido do valor da compra e verificar que o estorno debitado do saldo
corresponde a esse mesmo percentual do cashback originalmente creditado.

**Acceptance Scenarios**:

1. **Given** uma compra que gerou R$10 de cashback, **When** o usuário devolve itens
   correspondentes a 30% do valor da compra, **Then** R$3 são estornados do saldo do
   usuário.
2. **Given** uma compra já parcialmente devolvida uma vez, **When** uma segunda devolução
   parcial da mesma compra é registrada, **Then** o estorno é calculado sobre a parcela da
   compra ainda não devolvida, e a soma de todas as devoluções da compra nunca reverte mais
   cashback do que o total originalmente creditado para ela.

---

### Edge Cases

- O que acontece quando chega um estorno (cancelamento ou devolução) referente a uma
  compra que nunca gerou cashback (por estar abaixo do valor mínimo)? O sistema não deve
  gerar nenhum débito no saldo.
- O que acontece quando a soma de devoluções parciais de uma mesma compra atinge 100% do
  valor da compra? O tratamento deve ser equivalente a um cancelamento total para fins de
  estorno, sem debitar cashback além do que foi creditado para aquela compra.
- O que acontece quando chega um cancelamento total para uma compra que já teve
  devoluções parciais registradas? Apenas a parcela de cashback ainda não estornada deve
  ser debitada.
- Como o sistema trata uma tentativa de registrar cancelamento ou devolução duplicada para
  o mesmo evento (por exemplo, reenvio de uma notificação)? O estorno não deve ser
  aplicado mais de uma vez para o mesmo evento de cancelamento/devolução.
- O que acontece quando o usuário solicita resgate e, em seguida, um estorno referente a
  uma compra anterior chega? O saldo deve refletir o débito do estorno mesmo que isso
  resulte em saldo negativo (dívida), sem que o resgate já concluído seja desfeito.
- O que acontece quando o serviço responsável por calcular/creditar ou estornar cashback
  está indisponível no momento em que uma compra, cancelamento ou devolução é registrada?
  O registro do evento (compra, cancelamento ou devolução) MUST ser concluído com sucesso
  independentemente dessa indisponibilidade; o cashback correspondente fica com estado
  pendente e o sistema MUST repetir a tentativa de cálculo/crédito ou estorno até que ela
  seja concluída com sucesso.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir o registro de uma compra com, no mínimo, valor,
  categoria, data e identificador do usuário.
- **FR-002**: Ao registrar uma compra, o sistema MUST determinar automaticamente se ela é
  elegível a cashback, com base em um valor mínimo configurável.
- **FR-003**: Para compras elegíveis, o sistema MUST calcular o valor de cashback
  aplicando a regra percentual da categoria da compra, ou a regra geral quando a categoria
  não tiver regra específica.
- **FR-004**: O sistema MUST limitar o cashback calculado por compra a um teto máximo
  configurável, mesmo quando o percentual da regra resultaria em valor maior.
- **FR-005**: O sistema MUST creditar automaticamente o cashback calculado no saldo do
  usuário correspondente à compra, sem exigir ação manual do usuário.
- **FR-006**: O sistema MUST permitir o registro de um cancelamento total de uma compra já
  existente, feito pelo próprio usuário ou por um processo interno de atendimento/operações
  agindo em nome dele.
- **FR-007**: Ao registrar um cancelamento total, o sistema MUST estornar do saldo do
  usuário a parcela do cashback daquela compra que ainda não havia sido estornada.
- **FR-008**: O sistema MUST permitir o registro de uma ou mais devoluções parciais de uma
  compra já existente, feito pelo próprio usuário ou por um processo interno de
  atendimento/operações agindo em nome dele, cada devolução associada a um percentual ou
  valor devolvido da compra.
- **FR-009**: Ao registrar uma devolução parcial, o sistema MUST estornar do saldo do
  usuário o valor de cashback proporcional à parcela devolvida da compra.
- **FR-010**: O sistema MUST garantir que a soma de todos os estornos (cancelamento e/ou
  devoluções) referentes a uma mesma compra nunca exceda o valor de cashback originalmente
  creditado para essa compra.
- **FR-011**: O sistema MUST permitir que o saldo de um usuário fique negativo quando um
  estorno é aplicado após o valor correspondente já ter sido resgatado.
- **FR-012**: O sistema MUST manter um histórico consultável de todas as movimentações de
  saldo de cada usuário — créditos, estornos e resgates — incluindo saldo devedor quando
  existente.
- **FR-013**: O sistema MUST permitir que um usuário solicite o resgate do saldo de
  cashback disponível.
- **FR-014**: O sistema MUST impedir o resgate quando o saldo disponível for zero ou
  negativo.
- **FR-015**: O sistema MUST NOT bloquear preventivamente o registro de um resgate por
  existirem compras cujo cancelamento ou devolução ainda possa ocorrer no futuro.
- **FR-016**: O sistema MUST NOT aplicar o estorno de um mesmo evento de cancelamento ou
  devolução mais de uma vez, mesmo que a notificação desse evento seja recebida
  repetidamente.
- **FR-017**: Os valores mínimo para elegibilidade, percentuais por categoria, percentual
  geral e teto máximo de cashback MUST ser configuráveis sem exigir uma nova versão do
  sistema para alterá-los.
- **FR-018**: O registro de uma compra, cancelamento ou devolução MUST ser concluído com
  sucesso independentemente da disponibilidade do cálculo/crédito ou estorno de cashback
  correspondente; esse registro MUST NOT falhar nem ficar bloqueado aguardando o resultado
  do passo de cashback.
- **FR-019**: Quando o cálculo/crédito ou o estorno de cashback não puder ser concluído no
  momento em que é disparado, o sistema MUST manter esse cashback em estado pendente e
  MUST repetir a tentativa até que ela seja concluída com sucesso, sem exigir nova ação do
  usuário ou reenvio do evento original.

### Key Entities

- **Compra**: Representa uma compra feita por um usuário. Atributos-chave: identificador,
  usuário, valor, categoria, data, estado (ativa, cancelada, parcialmente devolvida).
- **Cancelamento**: Representa a anulação total de uma compra existente. Referencia a
  compra original e a data do cancelamento.
- **Devolução**: Representa a devolução parcial de uma compra existente. Referencia a
  compra original, o percentual ou valor devolvido, e a data.
- **Regra de Cashback**: Define o percentual de cashback aplicável a uma categoria (ou à
  regra geral), o valor mínimo de compra elegível e o teto máximo de cashback por compra.
- **Crédito de Cashback**: Representa o valor de cashback gerado por uma compra elegível,
  vinculado a essa compra e ao usuário. Possui um estado (pendente ou concluído) que
  reflete se o crédito já foi efetivado no saldo do usuário.
- **Estorno de Cashback**: Representa a reversão total ou parcial de um crédito de
  cashback, vinculado ao cancelamento ou devolução que o originou. Possui um estado
  (pendente ou concluído) que reflete se o débito já foi efetivado no saldo do usuário.
- **Saldo do Usuário**: Valor acumulado de cashback disponível para um usuário, podendo
  ser negativo (dívida).
- **Movimentação de Extrato**: Registro individual de crédito, estorno ou resgate que
  compõe o histórico do saldo de um usuário.
- **Resgate**: Representa a retirada, pelo usuário, de parte ou todo o saldo disponível de
  cashback.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% das compras elegíveis (acima do valor mínimo) resultam em cashback
  creditado no saldo do usuário sem nenhuma ação manual do usuário ou de um operador.
- **SC-002**: 100% dos cancelamentos totais e devoluções parciais de compras com cashback
  já creditado resultam em um estorno refletido no saldo e no extrato do usuário.
- **SC-003**: Em nenhum caso o total de cashback estornado para uma compra excede o total
  de cashback creditado para essa mesma compra, verificável por auditoria do extrato.
- **SC-004**: Usuários conseguem visualizar, em um único extrato, 100% dos créditos,
  estornos e resgates associados ao seu saldo, incluindo eventuais saldos devedores.
- **SC-005**: Um usuário com saldo disponível consegue concluir uma solicitação de resgate
  em uma única interação, sem etapas de aprovação manual.
- **SC-006**: Solicitações de resgate com saldo disponível igual a zero ou negativo são
  recusadas em 100% dos casos, sem exceção.
- **SC-007**: 100% das compras, cancelamentos e devoluções são registrados com sucesso
  mesmo quando o cálculo/crédito ou estorno de cashback correspondente não pode ser
  concluído no momento do registro; o cashback pendente é efetivado automaticamente assim
  que possível, sem exigir nenhuma ação do usuário.
- **SC-008**: Sob operação normal (sem indisponibilidade de serviços), o cashback de uma
  compra elegível aparece creditado no saldo do usuário em poucos segundos após o registro
  da compra.

## Assumptions

- O identificador do usuário já existe e é fornecido por um sistema de cadastro/autenticação
  externo a este conjunto de serviços; este sistema não cria nem gerencia identidades de
  usuário.
- Uma devolução parcial é sempre expressa (ou pode ser convertida) em um percentual do
  valor da compra, e a soma dos percentuais de todas as devoluções de uma mesma compra
  MUST NOT exceder 100%.
- Não há prazo limite para que um cancelamento ou devolução seja registrado após a compra
  original; qualquer compra existente pode ser cancelada ou parcialmente devolvida a
  qualquer momento, exceto quando já totalmente estornada.
- O resgate do saldo de cashback é tratado, nesta fase, como uma solicitação que apenas
  debita o saldo e registra a movimentação; a forma de pagamento efetivo ao usuário
  (transferência bancária, voucher, etc.) está fora do escopo desta especificação.
- Não há múltiplas moedas; todos os valores monetários (compra, cashback, saldo) são
  expressos na mesma moeda.
- Categorias de compra formam uma lista conhecida e configurável; uma compra sempre pertence
  a exatamente uma categoria.
