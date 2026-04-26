package com.xflprflx.neokioski;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class CheckDBTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void printTemplatesAndSteps() {
        System.out.println("=== TEMPLATES ===");
        List<Map<String, Object>> templates = jdbcTemplate.queryForList("SELECT id, name, requires_signature FROM tb_task_templates ORDER BY id DESC LIMIT 5");
        for (Map<String, Object> t : templates) {
            System.out.println("Template " + t.get("id") + ": " + t.get("name") + " | requires_signature=" + t.get("requires_signature"));
            
            List<Map<String, Object>> steps = jdbcTemplate.queryForList(
                "SELECT tsd.id, tsd.label, tsr.step_order, tsr.is_global, tsr.id as tsr_id " +
                "FROM tb_template_step_relations tsr " +
                "JOIN tb_task_step_definitions tsd ON tsr.task_step_definition_id = tsd.id " +
                "WHERE tsr.task_template_id = ? ORDER BY tsr.step_order", t.get("id")
            );
            
            for (Map<String, Object> s : steps) {
                System.out.println("  Step " + s.get("id") + " (order " + s.get("step_order") + ", global " + s.get("is_global") + "): " + s.get("label"));
                
                List<Map<String, Object>> nextSteps = jdbcTemplate.queryForList(
                    "SELECT next_step_definition_id FROM tb_template_step_next_allowed WHERE template_step_relation_id = ?", s.get("tsr_id")
                );
                System.out.print("    Allowed next: ");
                for (Map<String, Object> ns : nextSteps) {
                    System.out.print(ns.get("next_step_definition_id") + " ");
                }
                System.out.println();
            }
        }
    }
}
