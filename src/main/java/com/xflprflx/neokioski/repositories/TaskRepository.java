package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositório de acesso a dados para gerenciamento principal da entidade de tarefas (Task).
 */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("SELECT DISTINCT t FROM Task t WHERE t.group = :group ORDER BY t.createdAt DESC")
     /**
     * Busca todas as tarefas associadas a um grupo específico, carregando suas coleções relacionadas.
     * @param group O grupo associado.
     * @return Lista de tarefas pertencentes ao grupo.
     */
    List<Task> findByGroupWithCollections(@Param("group") Group group);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN t.taskEvents e WHERE t.group = :group AND (t.creator = :user OR e.user = :user) ORDER BY t.createdAt DESC")
 /**
     * Retorna tarefas do grupo em que o usuário participou como criador ou agindo em algum evento.
     * @param group O grupo de tarefas.
     * @param user O usuário a ser filtrado.
     * @return Lista ordenada decrescente por data de criação.
     */
    List<Task> findMyTasksByGroup(@Param("group") Group group, @Param("user") User user);

    @Query("""
           SELECT t FROM Task t
           LEFT JOIN TemplateStepRelation sr
             ON sr.taskTemplate = t.taskTemplate
            AND sr.taskStepDefinition = t.currentStep
           WHERE (t.currentStep IS NULL OR sr IS NULL OR sr.isFinalizer = false)
           """)
 /**
     * Consulta todas as tarefas pendentes, isto é, que não estão em um passo considerado "finalizador".
     * @return Lista de todas as tarefas globais pendentes.
     */
    List<Task> findAllPending();
    
    @Query("""
           SELECT COUNT(t) FROM Task t
           LEFT JOIN TemplateStepRelation sr
             ON sr.taskTemplate = t.taskTemplate
            AND sr.taskStepDefinition = t.currentStep
           WHERE (t.currentStep IS NULL OR sr IS NULL OR sr.isFinalizer = false)
           """)
    long countAllPending();

    @Query("""
           SELECT DISTINCT t FROM Task t
           LEFT JOIN TemplateStepRelation sr
             ON sr.taskTemplate = t.taskTemplate
            AND sr.taskStepDefinition = t.currentStep
           WHERE t.group = :group
             AND (t.currentStep IS NULL OR sr IS NULL OR sr.isFinalizer = false)
           ORDER BY t.createdAt DESC
           """)
    List<Task> findPendingByGroup(@Param("group") Group group);

    @Query("""
           SELECT COUNT(t) FROM Task t
           LEFT JOIN TemplateStepRelation sr
             ON sr.taskTemplate = t.taskTemplate
            AND sr.taskStepDefinition = t.currentStep
           WHERE t.group = :group
             AND (t.currentStep IS NULL OR sr IS NULL OR sr.isFinalizer = false)
           """)
    long countPendingByGroup(@Param("group") Group group);

    @Query("""
           SELECT COUNT(t) FROM Task t
           LEFT JOIN TemplateStepRelation sr
             ON sr.taskTemplate = t.taskTemplate
            AND sr.taskStepDefinition = t.currentStep
           WHERE t.group IN :groups
             AND (t.currentStep IS NULL OR sr IS NULL OR sr.isFinalizer = false)
           """)
    long countPendingByGroups(@Param("groups") List<Group> groups);

    @Query("SELECT DISTINCT t FROM Task t WHERE t.group = :group AND t.createdAt BETWEEN :start AND :end ORDER BY t.createdAt DESC")
    List<Task> findByGroupAndCreatedAtBetweenOrderByCreatedAtDesc(@Param("group") Group group, @Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    List<Task> findAllByCreatedAtBetween(java.time.Instant start, java.time.Instant end);

    @Query("SELECT t.group.name, COUNT(t) FROM Task t GROUP BY t.group.name")
    List<Object[]> countTasksByGroup();

    @Query("SELECT t.group.name, COUNT(t) FROM Task t WHERE t.group IN :groups GROUP BY t.group.name")
    List<Object[]> countTasksByGroupIn(@Param("groups") List<Group> groups);

    @Query("SELECT CAST(t.createdAt AS date) as day, COUNT(t) FROM Task t WHERE t.createdAt >= :start GROUP BY day ORDER BY day")
    List<Object[]> countTasksByDay(@Param("start") java.time.Instant start);

    @Query("SELECT CAST(t.createdAt AS date) as day, COUNT(t) FROM Task t WHERE t.createdAt >= :start AND t.group IN :groups GROUP BY day ORDER BY day")
    List<Object[]> countTasksByDayAndGroups(@Param("start") java.time.Instant start, @Param("groups") List<Group> groups);
}
