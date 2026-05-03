package com.xflprflx.neokioski.utils;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.models.template.TaskTemplateMention;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link TaskMentionUtils}.
 * Garante 100% de cobertura das regras de resolução de menções customizadas.
 */
@DisplayName("Utilitários de Menção de Tarefa - Testes de Cobertura")
class TaskMentionUtilsTest {

    @Test
    @DisplayName("Deve retornar o próprio texto quando os parâmetros de entrada forem nulos ou vazios")
    void shouldReturnTextWhenInputsAreNullOrBlank() {
        assertNull(TaskMentionUtils.resolveCustomMentions(null, mock(Task.class)));
        assertEquals("", TaskMentionUtils.resolveCustomMentions("", mock(Task.class)));
        assertEquals(" ", TaskMentionUtils.resolveCustomMentions(" ", mock(Task.class)));
        assertEquals("teste", TaskMentionUtils.resolveCustomMentions("teste", null));
        
        Task taskNoTemplate = mock(Task.class);
        when(taskNoTemplate.getTaskTemplate()).thenReturn(null);
        assertEquals("teste", TaskMentionUtils.resolveCustomMentions("teste", taskNoTemplate));
    }

    @Test
    @DisplayName("Deve retornar o texto original se não houver mapeamentos de menção no template")
    void shouldReturnOriginalTextIfNoMappings() {
        Task task = mock(Task.class);
        TaskTemplate template = mock(TaskTemplate.class);
        when(task.getTaskTemplate()).thenReturn(template);
        
        when(template.getMentionsMappings()).thenReturn(null);
        assertEquals("olá @mencao", TaskMentionUtils.resolveCustomMentions("olá @mencao", task));

        when(template.getMentionsMappings()).thenReturn(Collections.emptySet());
        assertEquals("olá @mencao", TaskMentionUtils.resolveCustomMentions("olá @mencao", task));
    }

    @Test
    @DisplayName("Deve substituir placeholders corretamente para usuários do mesmo grupo")
    void shouldReplacePlaceholdersCorrectlyForSameGroup() {
        // Setup
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(100L);

        Task task = mock(Task.class);
        when(task.getGroup()).thenReturn(group);

        TaskTemplate template = mock(TaskTemplate.class);
        when(task.getTaskTemplate()).thenReturn(template);

        User u1 = mock(User.class);
        when(u1.getUsername()).thenReturn("pedro");
        User u2 = mock(User.class);
        when(u2.getUsername()).thenReturn("maria");

        TaskTemplateMention mapping = mock(TaskTemplateMention.class);
        when(mapping.getGroup()).thenReturn(group);
        when(mapping.getPlaceholder()).thenReturn("@mencao1");
        // Usando LinkedHashSet para ordem determinística no teste se possível, ou validando via contém
        when(mapping.getUsers()).thenReturn(new LinkedHashSet<>(List.of(u1, u2)));

        when(template.getMentionsMappings()).thenReturn(Set.of(mapping));

        // Execução
        String text = "Notificar @mencao1 agora";
        String result = TaskMentionUtils.resolveCustomMentions(text, task);

        // Verificação
        assertTrue(result.contains("@pedro"), "Deve conter menção ao pedro");
        assertTrue(result.contains("@maria"), "Deve conter menção à maria");
        assertFalse(result.contains("@mencao1"), "O placeholder deve ser removido");
    }

    @Test
    @DisplayName("Não deve substituir placeholders se pertencerem a um grupo diferente")
    void shouldNotReplacePlaceholdersOfOtherGroups() {
        Group taskGroup = mock(Group.class);
        when(taskGroup.getId()).thenReturn(1L);

        Group mappingGroup = mock(Group.class);
        when(mappingGroup.getId()).thenReturn(2L);

        Task task = mock(Task.class);
        when(task.getGroup()).thenReturn(taskGroup);

        TaskTemplate template = mock(TaskTemplate.class);
        when(task.getTaskTemplate()).thenReturn(template);

        TaskTemplateMention mapping = mock(TaskTemplateMention.class);
        when(mapping.getGroup()).thenReturn(mappingGroup);
        when(mapping.getPlaceholder()).thenReturn("@placeholder");

        when(template.getMentionsMappings()).thenReturn(Set.of(mapping));

        String text = "Atenção @placeholder";
        assertEquals(text, TaskMentionUtils.resolveCustomMentions(text, task), "Não deve haver substituição para grupos distintos");
    }

    @Test
    @DisplayName("Deve realizar substituição case-insensitive do placeholder")
    void shouldBeCaseInsensitiveOnPlaceholderMatch() {
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(1L);

        Task task = mock(Task.class);
        when(task.getGroup()).thenReturn(group);

        TaskTemplate template = mock(TaskTemplate.class);
        when(task.getTaskTemplate()).thenReturn(template);

        User u = mock(User.class);
        when(u.getUsername()).thenReturn("boss");

        TaskTemplateMention mapping = mock(TaskTemplateMention.class);
        when(mapping.getGroup()).thenReturn(group);
        when(mapping.getPlaceholder()).thenReturn("@AvisoGeral"); // Definido com CamelCase
        when(mapping.getUsers()).thenReturn(Set.of(u));

        when(template.getMentionsMappings()).thenReturn(Set.of(mapping));

        // Testando com texto todo em minúsculo
        String result = TaskMentionUtils.resolveCustomMentions("Olá @avisogeral", task);
        assertEquals("Olá @boss", result);
    }

    @Test
    @DisplayName("Deve manter o texto inalterado se o placeholder mapeado não existir no texto")
    void shouldKeepTextUnchangedIfPlaceholderMissing() {
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(1L);

        Task task = mock(Task.class);
        when(task.getGroup()).thenReturn(group);

        TaskTemplate template = mock(TaskTemplate.class);
        when(task.getTaskTemplate()).thenReturn(template);

        TaskTemplateMention mapping = mock(TaskTemplateMention.class);
        when(mapping.getGroup()).thenReturn(group);
        when(mapping.getPlaceholder()).thenReturn("@existente");

        when(template.getMentionsMappings()).thenReturn(Set.of(mapping));

        String text = "Texto sem menções";
        assertEquals(text, TaskMentionUtils.resolveCustomMentions(text, task));
    }
}
