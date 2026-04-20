package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.repositories.TaskRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Componente responsável por recarregar e agendar os alertas de tarefas pendentes
 * assim que a aplicação é inicializada.
 */
@Component
public class AlertReloader {

    private final TaskRepository taskRepository;
    private final TaskAlertService taskAlertService;

    /**
     * Construtor do componente AlertReloader.
     *
     * @param taskRepository Repositório para busca de tarefas no banco de dados.
     * @param taskAlertService Serviço responsável pelo agendamento em memória dos alertas.
     */
    public AlertReloader(TaskRepository taskRepository, TaskAlertService taskAlertService) {
        /*
         * Injeta a dependência do repositório de tarefas.
         */
        this.taskRepository = taskRepository;
        /*
         * Injeta a dependência do serviço de alertas de tarefas.
         */
        this.taskAlertService = taskAlertService;
    }

    /**
     * Método acionado automaticamente logo após a aplicação do Spring Boot estar pronta
     * (ApplicationReadyEvent). Busca todas as tarefas pendentes e agenda os gatilhos de alerta.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void reloadPendingAlerts() {
        /*
         * Exibe um log no console indicando o início do agendamento dinâmico.
         */
        System.out.println(">>> Inicializando o agendador de alertas dinâmicos...");

        /*
         * Consulta o repositório para listar todas as tarefas globais que ainda
         * não estão em um estado finalizado.
         */
        List<Task> pendingTasks = taskRepository.findAllPending();

        /*
         * Inicializa o contador para rastrear a quantidade total de gatilhos agendados.
         */
        int count = 0;
        
        /*
         * Percorre cada tarefa encontrada na lista de tarefas pendentes.
         */
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (Task task : pendingTasks) {
            /*
             * Verifica se a tarefa tem um template associado, e se este template possui
             * alertas cadastrados e se a lista de alertas não está vazia.
             */
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (task.getTaskTemplate() != null && task.getTaskTemplate().getAlerts() != null && !task.getTaskTemplate().getAlerts().isEmpty()) {
                /*
                 * Delega o agendamento real dos alertas da tarefa ao serviço especializado.
                 */
                taskAlertService.scheduleAlertsForTask(task);

                /*
                 * Incrementa o contador com o número de alertas atrelados ao template da tarefa.
                 */
                count += task.getTaskTemplate().getAlerts().size();
            }
        }
        
        /*
         * Exibe um log no console confirmando a quantidade final de alertas agendados em memória.
         */
        System.out.println(">>> Foram agendados " + count + " gatilhos em RAM para tarefas pendentes.");
    }
}
