# Fluxo de Alertas NeoKioski

Este documento detalha o fluxo técnico dos alertas configurados no sistema.

## Diagrama de Fluxo

```mermaid
sequenceDiagram
    participant U as "Usuário"
    participant TS as "TaskService"
    participant TAS as "TaskAlertService"
    participant CS as "ChatService"
    participant DB as "Banco de Dados"
    participant SCHED as "TaskScheduler"

    Note over U, SCHED: 1. Criação de Tarefa
    U->>TS: Solicita criação
    TS->>DB: Salva tarefa
    TS->>TAS: Agenda alertas iniciais
    TAS->>SCHED: Registra timers em background

    Note over U, SCHED: 2. Execução de Step (Avanço)
    U->>TS: Executa ação (Step)
    TS->>DB: Atualiza estado da tarefa
    
    rect rgb(240, 240, 240)
        Note right of TS: Notificações por Menção (@)
        alt Tem @criador ou @autorizadores configurado
            TS->>CS: Envia mensagem definida no template
        else Tem @autor_anterior configurado
            TS->>CS: Envia mensagem definida no template
        end
    end

    TS->>TAS: Cancela alertas do step alcançado
    TS->>TAS: Agenda novos alertas temporais

    Note over U, SCHED: 3. Alertas por Tempo (Timeout)
    SCHED->>TAS: Timer dispara
    TAS->>DB: Verifica se step alvo foi atingido
    
    opt Pendente (Ainda não atingiu alvo)
        TAS->>CS: Envia alerta de timeout (ex: Prazos)
        TAS->>DB: Marca alerta como disparado
    end
```

## Regras de Notificação

1. **Silêncio por Padrão**: O sistema não envia mensagens automáticas como "Tarefa criada" ou "Tarefa avançou".
2. **Controle Total pelo Template**: Mensagens de chat só ocorrem se houver uma configuração de **@menção** associada ao Step no Template da tarefa.
3. **Alertas por Papel (Role)**: Quando uma mensagem é disparada (por menção ou tempo), ela carrega os metadados das roles interessadas para que o som seja reproduzido apenas nos dispositivos autorizados.

## Variáveis Disponíveis nos Templates
- **@criador**: Autor original da tarefa.
- **@autorizadores**: Usuários com permissão de assinatura.
- **@usuario**: Quem executou a ação atual.
- **@autor_anterior**: Quem executou o passo imediatamente anterior.
- **@resposta**: Texto inserido no campo de resposta do passo.
