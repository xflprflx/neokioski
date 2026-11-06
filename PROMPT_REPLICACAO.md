# Prompt para Replicação do Sistema Neokioski em Python (FastAPI + HTMX)

Você é um Engenheiro de Software Sênior especializado em Python. Sua tarefa é replicar a estrutura completa de um sistema chamado "Neokioski" usando a seguinte stack:
- **Backend:** FastAPI (para máxima performance e tipagem com Pydantic)
- **Banco de Dados:** SQLite (com SQLAlchemy ou SQLModel como ORM e Alembic para migrações)
- **Frontend:** HTML renderizado via Jinja2 + HTMX (para interatividade sem precisar de um SPA pesado) + TailwindCSS (para estilização rápida e bonita)
- **Inicialização:** Um arquivo `run.bat` que configura e inicia tudo no Windows.

## 1. Escopo e Funcionalidades a Replicar (TUDO)
O Neokioski é um sistema robusto de gestão de tarefas em tempo real para lojas. Você deve replicar *absolutamente todas* as funcionalidades a seguir:

1. **Gestão de Usuários e Grupos (Autenticação/Autorização):**
   - Usuários têm nome, username, senha (hasheada com bcrypt), role (ADMIN, SELLER, LOGISTICS), last_seen, e flags de notificação (`notify_store_order`, `notify_chat_sounds`, `is_active` para soft delete).
   - Grupos contêm múltiplos usuários.
   - Login e controle de sessão (cookies ou JWT em HttpOnly).

2. **Templates de Tarefas (Workflow Dinâmico):**
   - Criação de **Templates de Tarefas** (`tb_task_templates`): definem nome, som de alerta, se requer assinatura, se requer unicidade (`requires_uniqueness`). Suporte a soft delete.
   - **Step Definitions** (Etapas): como "Pendente", "Em Andamento", "Concluído", definindo cor e rótulo.
   - O Template liga-se aos Steps (`tb_template_step_relations`), definindo ordem, qual step é finalizador (`is_finalizer`) e qual step conta para o ranking de conclusão (`counts_as_completion`).
   - O Template possui **Campos Customizados** (`tb_task_field_definitions` e `tb_template_field_relations`) com tipos: TEXT, NUMBER, RADIO, CHECKBOX, DATE, TIME, DROPDOWN, TEXT_LIST. Com suporte a opções (para radio/dropdown).

3. **Execução de Tarefas:**
   - Criação de uma `Task` a partir de um Template e Grupo.
   - Armazenar valores dos campos dinâmicos preenchidos (`tb_task_field_values`).
   - Registro de histórico de eventos (`tb_task_events`): quem mudou qual campo ou moveu de qual passo para qual passo.

4. **SLA (Service Level Agreement):**
   - **Configuração de SLA** (`tb_task_sla_configs`): define tempo alvo (em segundos, minutos ou horas) de um passo origem para um passo destino dentro de um template.
   - **Resultados de SLA** (`tb_task_sla_results`): Quando uma tarefa transita entre os passos alvo, o sistema calcula o tempo gasto, se estourou (is_breached), e exige justificativa caso tenha estourado.

5. **Chat em Tempo Real:**
   - Chat atrelado a uma Tarefa (ou Grupo).
   - Mensagens com texto, timestamp, e menções a regras de alerta.
   - Suporte a Edição de mensagens (`tb_message_edits`).
   - Suporte a Reações em mensagens (`tb_message_reactions` - emojis atrelados ao usuário).
   - Suporte a Responder mensagens (`reply_to_id`).
   - Utilizar WebSockets do FastAPI em conjunto com HTMX (extensão `ws`) para atualização real-time sem reload.

6. **Relatórios, Alertas e Dashboard:**
   - Alertas configurados em templates para disparar após X minutos num determinado passo (`tb_task_template_alerts`).
   - Registros de alertas acionados (`tb_task_triggered_alerts`).
   - Menções fixas do template (`tb_task_template_mentions`).
   - **Relatórios:** Configuração de colunas e propriedades (PDF e Excel). Modelos como `tb_store_report_config`. A nova aplicação deve usar bibliotecas como `reportlab` ou `pdfkit` para PDF, e `openpyxl` ou `pandas` para Excel.
   - Dashboard exibindo métricas de SLA e tarefas concluídas.

## 2. Instruções Técnicas para a Nova Stack

1. **Backend (FastAPI):**
   - Use `FastAPI` com rotas bem divididas (ex: routers para auth, tasks, chat, reports).
   - Use `SQLAlchemy` (v2.0+) e configure as models baseadas nos requisitos acima (incluindo as Foreign Keys, relações Many-to-Many e campos de soft delete).
   - Crie dependências para pegar o usuário logado e injetar a sessão do banco.

2. **Frontend (Jinja2 + HTMX + Tailwind):**
   - Sirva arquivos HTML renderizados via `Jinja2Templates`.
   - Inclua o HTMX e a extensão WebSocket via CDN no layout base.
   - Inclua o TailwindCSS via CDN (para ambiente de dev/protótipo).
   - A UI deve ser responsiva e equilibrar perfeitamente leveza com qualidade visual (use componentes estilo Tailwind UI).
   - Operações como criar tarefa, mudar status da tarefa, enviar mensagem e adicionar reação DEVEM ser feitas via requisições assíncronas do HTMX (ex: `hx-post`, `hx-swap`), atualizando apenas fragmentos da tela.

3. **Arquivo de Inicialização (`run.bat`):**
   Crie um script batch super robusto que, com um único duplo clique:
   1. Verifica se o Python está instalado.
   2. Cria um ambiente virtual (venv) se a pasta não existir.
   3. Ativa o venv.
   4. Instala as dependências (`pip install -r requirements.txt`).
   5. Roda as migrações do banco de dados (Alembic) ou cria as tabelas do SQLite via script.
   6. Inicia o servidor uvicorn (`uvicorn main:app --reload`).

## 3. O que você deve me entregar na sua resposta

1. A estrutura de diretórios ideal sugerida.
2. O código do arquivo `requirements.txt`.
3. O código do arquivo `run.bat` (exatamente como solicitado).
4. O código das Models do SQLAlchemy (cobrindo todas as entidades citadas).
5. Um exemplo da rota principal de Tasks (FastAPI) retornando HTML e processando requests HTMX.
6. Um exemplo do template HTML base e da tela de detalhes da Tarefa (com o Chat em WebSocket e mudança de passos via HTMX).

Seja o mais eficiente e limpo possível. Performance e UI de qualidade são essenciais.
