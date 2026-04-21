# Análise Detalhada de Problemas de N+1 Queries no NeoKioski

A seguinte análise foi realizada rastreando mapeamentos JPA, métodos de repositório Spring Data e iterações ao longo dos serviços e interfaces da aplicação. O objetivo foi listar todas as potenciais situações que geram **N+1 queries**, explicar suas causas e apresentar a melhor solução técnica em cada caso, garantindo que a **lógica de negócios permaneça intacta**.

---

## 1. Mapeamentos de Entidades com `FetchType.EAGER` e Ausência de `@EntityGraph`/`JOIN FETCH` em Repositórios

O uso de `FetchType.EAGER` acompanhado de `@Fetch(FetchMode.SUBSELECT)` resolve parcialmente o N+1 transformando-o num N+2 (um select na tabela pai, e um grande select `IN` nas filhas). Contudo, isso mascara problemas com outros relacionamentos Lazy não fetcheados explícitos nos repositórios, especialmente quando as próprias classes filhas também têm relacionamento com outras tabelas ou quando a listagem no repositório é extensa.

### Problema 1.1: `TaskRepository` não utiliza `JOIN FETCH` adequadamente nas coleções `@ManyToOne` (User/Group) ou para forçar o Eager.
- **Onde ocorre**: `TaskRepository` nos métodos `findAllPending()`, `findPendingByGroup()`, `findByGroupAndCreatedAtBetweenOrderByCreatedAtDesc()`, `countAllPending()`, e `findMyTasksByGroup()`.
- **Objetivo da Query**: Retornar a lista de tarefas, muitas vezes de um grupo específico ou do sistema, para exibição no Chat, Dashboard ou Views genéricas.
- **Por que tem problema**: O Hibernate dispara uma query principal para buscar as instâncias de `Task`. Dentro de `Task`, os mapeamentos para `@ManyToOne` (como `creator`, `group`, `currentStep`, `taskTemplate`) são implicitamente EAGER na JPA, mas como a query customizada JPQL não usa `JOIN FETCH`, o Hibernate faz 1 query para trazer N Tasks, e em seguida **N queries adicionais** para carregar os usuários, templates, passos atuais e grupos de cada tarefa recuperada.
- **Possíveis soluções**:
  1. Alterar todos os `@ManyToOne` na entidade `Task` para `FetchType.LAZY`.
  2. Usar anotações `@EntityGraph` para especificar de forma dinâmica o que carregar nas queries JPQL.
  3. Reescrever as queries JPQL utilizando `JOIN FETCH`.
- **Melhor solução**: Usar `JOIN FETCH` explicitamente nos métodos mais utilizados (como buscar tarefas do grupo do chat ativo) ou `@EntityGraph(attributePaths = {"taskTemplate", "creator", "group", "currentStep"})`. A alteração nas entidades (solução 1) seria invasiva e alteraria o comportamento padrão da aplicação, o que vai contra a restrição estrita, portanto a adoção de `@EntityGraph` nas queries existentes é a mais transparente.

---

### Problema 1.2: `ChatMessageRepository` busca mensagens sem realizar fetch nos relacionamentos `User` e `Group`.
- **Onde ocorre**: `ChatMessageRepository.findByGroupAndTimestampBetweenOrderByTimestampAsc`, `findByGroupOrderByTimestampAsc()`.
- **Objetivo da Query**: Retornar o histórico de mensagens de um chat dentro de um intervalo de tempo para visualização no componente do Vaadin.
- **Por que tem problema**: Os métodos de `ChatMessage` buscam milhares de mensagens sem realizar JOIN. Mesmo os métodos que se chamam `findByGroupWithTask...` dão `FETCH` no campo `task`, `taskTemplate` e `currentStep`, **mas esquecem** do `user` (autor da mensagem). Com isso, cada `ChatMessage` retornado causa uma query `SELECT * FROM tb_users WHERE id = ?`. Um chat com 200 mensagens pode gerar 200 queries para carregar o autor de cada mensagem.
- **Possíveis soluções**:
  1. Modificar os JPQLs existentes e adicionar `LEFT JOIN FETCH m.user`.
  2. Implementar `@EntityGraph(attributePaths = {"user", "task", "task.taskTemplate", "task.currentStep"})`.
- **Melhor solução**: Aplicar `@EntityGraph(attributePaths = {"user"})` nos métodos padrão ou atualizar as strings JPQL em `findByGroupWithTaskAndTimestampBetweenOrderByTimestampAsc` para também darem `JOIN FETCH m.user`.

---

## 2. Acessos Iterativos (Loops) a Associações Lazy na Camada de Serviços/Dashboard

Quando uma coleção de objetos é carregada e logo em seguida passa por um laço `for()` que chama getters em propriedades Lazy não carregadas, o Hibernate lança a query para aquele objeto no exato momento da iteração.

### Problema 2.1: Cálculo de SLA (TMA) no `DashboardService.getDashboardData()`
- **Onde ocorre**: `DashboardService.java` ao montar os dados da variável `tmaPerStep`.
- **Objetivo da lógica**: Obter os eventos `getTaskEvents()` e cruzar passos ou checar `getFinalizerAt()` e buscar eventos relacionados a "DELIVERY" no método `getTaskFinalizerName()`.
- **Por que tem problema**: O Service faz duas chamadas aos repositórios: `taskRepository.findByGroupAndCreatedAtBetweenOrderByCreatedAtDesc(...)` (ou findAll). O repositório lista as tasks sem fetch para TemplateStepRelation no `getTaskFinalizerName`. No for, ele itera: `for(Task task : templateTasks)` e chama `task.getTaskEvents()`. Apesar do `@Fetch(FetchMode.SUBSELECT)` atenuar, o problema grave é dentro de `getTaskFinalizerName()`, onde ele itera nos eventos da tarefa chamando:
  `stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(t.getTaskTemplate().getId(), event.getTaskStepDefinition().getId())`.
  Isso faz uma query **dentro de um loop** (N x M queries, N=Tasks, M=Eventos)!
- **Possíveis soluções**:
  1. No início do cálculo do Dashboard, pré-carregar um Mapa contendo todas as relações de templates em memória.
  2. Criar uma query na JPQL agregando esses resultados no lado do banco.
- **Melhor solução**: A solução 1 não altera a lógica de negócios e melhora a eficiência do Java. Buscar todas as `TemplateStepRelation` pertinentes em 1 query inicial, armazená-las num `Map<String, TemplateStepRelation>` usando uma chave composta `(templateId + stepDefId)` e usar esse cache de memória dentro do loop.

### Problema 2.2: Verificação iterativa de status das Tasks no histórico (DashboardService)
- **Onde ocorre**: `DashboardService.getDashboardData()` montando os `MixedChartPoint`.
- **Objetivo da lógica**: Separar as tarefas entre "No Prazo" e "Atrasado".
- **Por que tem problema**: Na linha: `t.getSlaResults().stream().noneMatch(r -> r.getStatus() == SlaStatus.DELAYED)`. As Tasks carregadas pela query base não vieram com os resultados de SLA carregados no grafo (a não ser pelo FetchType Eager+Subselect). Se a listagem base de Tarefas no dashboard ficar imensa (por exemplo, visão global anual), o EAGER associado pode colapsar e disparar subselects custosos e repetitivos.
- **Melhor solução**: Adicionar o fetch `slaResults` caso `TaskRepository.findAllByCreatedAtBetween` ganhe um `@EntityGraph`.

### Problema 2.3: Avatar de usuários Online com loop no repositório (`ChatView`)
- **Onde ocorre**: `ChatView.updateOnlineUserAvatars()`.
- **Objetivo da lógica**: O serviço de presença provê uma lista de IDs de usuários online. A view varre essa lista para renderizar os avatares.
- **Por que tem problema**:
  ```java
  for (Long userId : onlineIds) {
      userService.findById(userId).ifPresent(user -> { ... });
  }
  ```
  Se houver 50 usuários online, serão 50 queries sequenciais por batimento (ou por atualização de status).
- **Possíveis soluções**:
  1. Alterar `userService` para receber a lista de IDs (`findByIdIn(Set<Long> ids)`).
- **Melhor solução**: Implementar `userService.findAllById(onlineIds)` que retorna `List<User>` de uma vez (1 query), e iterar sobre a lista retornada, preenchendo a interface gráfica sem múltiplas queries de banco.

---

## 3. Vazamentos de N+1 no Mapeamento Dinâmico de Template vs Passos (`TaskTemplateService`)

### Problema 3.1: Edição/Leitura do Grafo de Templates (`TaskTemplateService.getTemplateDto()`)
- **Onde ocorre**: Muitas requisições no `TaskTemplateEditorView` iteram por relações como `TemplateFieldRelation`, `TemplateStepRelation`, `TaskSlaConfig` da seguinte forma:
  `for(TemplateStepRelation sr : templateService.getStepRelationsByTemplateId(parameter))` e então acessam coleções das filhas (`sr.getAllowedNextSteps()`).
- **Objetivo da Query**: Alimentar o DTO para carregar no editor de fluxo visual do Vaadin.
- **Por que tem problema**: Carregar as relações filhas iterando no banco de dados e em seguida iterar em coleções filhas delas causa mini-rajadas de queries, pois `getAllowedNextSteps()` na Entidade `TemplateStepRelation` usará SUBSELECT (1 query extra), e se precisar obter campos dessas definições de passos haverá novas rodadas.
- **Melhor solução**: Criar um método `findTemplateWithFullGraph(Long id)` em `TaskTemplateRepository` com multiplos `JOIN FETCH` (ou anotação `@EntityGraph`) para as tabelas `templateStepRelation`, `templateFieldRelation`, e `alerts` carregando em um passe único do banco.

---

### 📝 Resumo das Ações Corretivas Recomendadas (Sem alterar regras de negócio)

1. Adicionar `@EntityGraph(attributePaths = {"user"})` na interface `ChatMessageRepository` para os métodos customizados de pesquisa sem fetch manual.
2. Adicionar `@EntityGraph(attributePaths = {"creator", "group", "currentStep", "taskTemplate"})` ou JPQL com `JOIN FETCH` nas queries do `TaskRepository`.
3. Criar método `findAllById(Iterable<Long> ids)` no `UserService` e utilizá-lo no método `updateOnlineUserAvatars` da `ChatView` no lugar do `findById` de dentro do loop.
4. Refatorar internamente o método privado `getTaskFinalizerName` de `DashboardService` e outros loops do mesmo pacote para usar um `Map` local com os resultados, eliminando as chamadas cíclicas para `stepRelationRepository.findBy...` em cada iteração de evento de tarefa.
## 4. Geração de Relatórios e Exportação em Lote (Excel e PDF)

A rotina de relatórios e exportação frequentemente lida com um volume enorme de registros (`Tasks`) sendo processados simultaneamente em um laço de iteração `for (Task task : tasks)`. Como a consulta principal que provê esses registros (`TaskService.findTasksWithFilters`) não efetua fetch adequado das coleções, ocorrem massivas N+1 queries durante a exportação.

### Problema 4.1: Exportação do Relatório Geral (`ExcelService` e `PdfService`)
- **Onde ocorre**: `ExcelService.generateGeneralTaskReport(List<Task> tasks)` e `PdfService.generateGeneralTaskReportPdf(List<Task> tasks)`.
- **Objetivo da lógica**: O usuário aciona os botões "Baixar Excel" ou "Baixar PDF" na `GeneralTaskReportView`. A view busca a lista filtrada de `Tasks` através do `TaskService` e passa para os geradores. O `ExcelService`, ao iterar pelas tarefas no `for (Task task : tasks)`, chama os métodos:
  - `task.getTaskFieldValues().stream()...`
  - `task.getTaskEvents().stream()...`
- **Por que tem problema**: O método `TaskService.findTasksWithFilters` retorna `Task` com suas coleções (`taskFieldValues`, `taskEvents`) como proxies (Lazy) ou via Subselect. Se 5.000 tarefas são retornadas para o relatório Excel, a iteração vai forçar a carga dessas coleções (e das subentidades nelas, como `getTaskFieldDefinition()` e `getTaskStepDefinition()`) dezenas de milhares de vezes no banco.
- **Possíveis soluções**:
  1. Modificar o construtor dinâmico (`Specifications` ou `Criteria API` que alimenta o `findTasksWithFilters`) para forçar `JOIN FETCH` de `taskFieldValues`, `taskFieldValues.taskFieldDefinition`, `taskEvents` e `taskEvents.taskStepDefinition`.
- **Melhor solução**: Para relatórios gigantes, o uso de `JOIN FETCH` múltiplo de coleções causará Produto Cartesiano na memória (múltiplos sacos no JPA), podendo causar falha de memória ou query inviável. A solução ideal (sem alterar regras de negócio) seria implementar uma consulta dedicada para relatórios onde o JPA é bypassado por um `JdbcTemplate` ou uma DTO Projection, buscando os dados já de forma tabular (achatada). Alternativamente, configurar corretamente `@EntityGraph` combinados com batch-size nas coleções, ou fragmentar o processamento do Excel em blocos paginados.

### Problema 4.2: Relatório Específico de Lojas (`PdfService.generateStoreReport`)
- **Onde ocorre**: `PdfService.generateStoreReport(...)`
- **Por que tem problema**: O PDF customizado itera na classe auxiliar `TaskService.StoreReportEntry` chamando repetidas vezes `taskService.getTaskAttributeValue(entry.task(), col.getAttributeKey())`. Por sua vez, `getTaskAttributeValue` faz chamadas à `getTaskEvents()` internamente (`getCollectedAt`, `getCollectorName`, busca dinâmica por "FIELD:").
- **Melhor solução**: Idêntica ao Problema 4.1. Para relatórios, extrair e projetar somente os campos pré-definidos (`ReportColumnConfig`) num DTO diretamente na consulta SQL no Repository, evitando instanciar milhares de entidades completas do Hibernate que só serão usadas para extração de strings (gerando N+1 em cascata nas dependências de Evento/TemplateStep/etc).
