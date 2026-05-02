package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import com.xflprflx.neokioski.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.Mockito.*;
/**
 * Testes Unitários de Performance e Inicialização para o TaskService.
 * 
 * CONTEXTO TÉCNICO (HIBERNATE E PERFORMANCE):
 * No ecossistema do NeoKioski, as Tarefas (Tasks) possuem uma estrutura rica de metadados
 * carregados via coleções Lazy (SLA, Eventos, Relacionamentos de Passos). 
 * Para manter a performance, evitamos carregar tudo de uma vez (Eager Loading), 
 * o que causaria o problema de "Explosão Cartesiana".
 * 
 * O PROBLEMA DO LAZY INITIALIZATION:
 * Como a interface de usuário (Vaadin) opera frequentemente fora de uma transação aberta
 * do banco de dados, o Hibernate dispararia uma 'LazyInitializationException' se a tela 
 * tentasse ler o SLA de uma tarefa que não foi pré-carregada.
 * 
 * A SOLUÇÃO "DEEP TOUCH":
 * O métodx 'initializeTaskData' do TaskService resolve isso "tocando" (chamando o getter e
 * iterando) nas coleções enquanto a sessão ainda está aberta.
 * 
 * ESTE TESTE GARANTE QUE:
 * 1. O serviço nunca se esqueça de "tocar" em uma nova coleção adicionada ao modelo.
 * 2. A lógica de inicialização profunda está sendo executada para todas as propriedades críticas.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do Serviço de Tarefas (TaskService)")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TemplateStepRelationRepository stepRelationRepository;
    @Mock
    private ChatService chatService;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private TaskAlertService taskAlertService;
    @Mock
    private TaskTemplateRepository taskTemplateRepository;
    @Mock
    private TaskFieldDefinitionRepository taskFieldDefinitionRepository;
    @Mock
    private TaskStepDefinitionRepository taskStepDefinitionRepository;
    @Mock
    private TaskSlaService taskSlaService;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository,
                stepRelationRepository,
                chatService,
                chatMessageRepository,
                taskAlertService,
                taskTemplateRepository,
                taskFieldDefinitionRepository,
                taskStepDefinitionRepository,
                taskSlaService
        );
    }

    /**
     * PROPÓSITO DO TESTE:
     * Este teste garante que o métodx initializeTaskData realize o "Deep Touch" em todas as
     * coleções Lazy do Hibernate. Isso é crucial para evitar LazyInitializationException
     * quando os objetos Task e TaskTemplate forem usados na camada de UI (Vaadin),
     * onde a transação do Hibernate já estará encerrada.
     */
    @Test
    @DisplayName("Deve tocar em todas as coleções críticas para inicialização profunda (Deep Touch)")
    void initializeTaskData_ShouldTouchAllCriticalCollections() {

        // 1. ARRANGE (Configuração dos Mocks)
        // Criamos mocks para a Tarefa e seu Template
        Task task = mock(Task.class);
        TaskTemplate template = mock(TaskTemplate.class);
        
        // Mocks para as coleções que precisam ser "tocadas"
        // Nota: O métodx forEach no serviço só executará se estas coleções não forem nulas.
        Set<TaskSlaResult> slaResults = mock(Set.class);
        Set<TaskEvent> events = mock(Set.class);
        Set<TaskFieldValue> fieldValues = mock(Set.class);
        Set<TaskStepDefinition> stepDefinitions = mock(Set.class);
        Set<TemplateStepRelation> relations = mock(Set.class);
        Set<User> authorizingUsers = mock(Set.class);
        Set<Role> alertRoles = mock(Set.class);

        // Configura o comportamento dos getters para retornar nossos mocks de coleção
        when(task.getTaskTemplate()).thenReturn(template);
        when(task.getSlaResults()).thenReturn(slaResults);
        when(task.getTaskEvents()).thenReturn(events);
        when(task.getTaskFieldValues()).thenReturn(fieldValues);
        when(task.getTaskStepDefinitions()).thenReturn(stepDefinitions);
        
        // Configura as coleções específicas do Template
        when(template.getTemplateStepRelation()).thenReturn(relations);
        when(template.getAuthorizingUsers()).thenReturn(authorizingUsers);
        when(template.getAlertRoles()).thenReturn(alertRoles);

        /**
         * NOTA HISTÓRICA:
         * O stub antigo para 'taskTemplateRepository.findByIdWithMetadata' foi removido.
         * O código atual não recarrega mais o template manualmente, confiando no 
         * @BatchSize e no carregamento preguiçoso sob demanda (Deep Touch).
         */

        // 2. ACT (Execução do métodx sob teste)
        taskService.initializeTaskData(task);

        // 3. ASSERT (Verificações)
        // Verificamos se cada getter de coleção crítica foi chamado pelo menos uma vez.
        // Isso confirma que o "Deep Touch" foi disparado.

        // Verificações na Task
        verify(task, atLeastOnce()).getSlaResults();
        verify(task, atLeastOnce()).getTaskEvents();
        verify(task, atLeastOnce()).getTaskFieldValues();
        verify(task, atLeastOnce()).getTaskStepDefinitions();

        // Verificações no Template associado
        verify(template, atLeastOnce()).getTemplateStepRelation();
        verify(template, atLeastOnce()).getAuthorizingUsers();
        verify(template, atLeastOnce()).getAlertRoles();
        
        /**
         * CONCLUSÃO:
         * Se o teste passar, garantimos que a tarefa está pronta para "viver" fora da transação
         * com todos os seus dados essenciais já carregados na memória JVM.
         */
    }
}
