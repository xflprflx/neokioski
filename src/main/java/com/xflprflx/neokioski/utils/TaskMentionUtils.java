package com.xflprflx.neokioski.utils;

import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.template.TaskTemplateMention;

import java.util.Set;
import java.util.stream.Collectors;

public class TaskMentionUtils {

    /**
     * Resolve placeholders no texto (ex: @mencao1) substituindo-os pelas menções reais dos usuários mapeados.
     */
    public static String resolveCustomMentions(String text, Task task) {
        if (text == null || text.isBlank() || task == null || task.getTaskTemplate() == null) {
            return text;
        }

        String result = text;
        Set<TaskTemplateMention> mappings = task.getTaskTemplate().getMentionsMappings();

        if (mappings == null || mappings.isEmpty()) {
            return result;
        }

        // Filtrar apenas mapeamentos do grupo da tarefa
        for (TaskTemplateMention mapping : mappings) {
            if (mapping.getGroup().getId().equals(task.getGroup().getId())) {
                String placeholder = mapping.getPlaceholder();
                String realMentions = mapping.getUsers().stream()
                        .map(u -> "@" + u.getUsername())
                        .collect(Collectors.joining(" "));
                
                // Substituição case-insensitive
                result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(placeholder), realMentions);
            }
        }

        return result;
    }
}
