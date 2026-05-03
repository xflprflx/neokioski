package com.xflprflx.neokioski;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

/**
 * Teste de Integração para Inspeção e Diagnóstico do Banco de Dados.
 * 
 * PROPÓSITO:
 * Validar se a estrutura física do banco de dados (tabelas, colunas, relacionamentos)
 * criada pelo Flyway corresponde às expectativas do negócio, especificamente para
 * os templates de tarefas e seus fluxos de trabalho (workflows).
 * 
 * INFRAESTRUTURA (TESTCONTAINERS):
 * Ao contrário de testes unitários que usam Mocks, este teste utiliza um banco
 * real dentro de um container Docker. Isso garante 100% de paridade com o ambiente
 * de produção, permitindo testar sintaxes SQL específicas do PostgreSQL que o H2
 * não suportaria (como operadores de JSONB ou tipos específicos).
 * 
 * CONFIGURAÇÃO:
 * - @SpringBootTest: Carrega o contexto Spring completo para disponibilizar o JdbcTemplate.
 * - @Testcontainers: Gerencia o ciclo de vida do Docker (start/stop automático).
 * - @ActiveProfiles("test"): Garante que configurações de produção não interfiram aqui.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Diagnóstico de Estrutura de Templates e Passos no DB")
public class CheckDBTest {

    /**
     * Define o container do PostgreSQL.
     * Versão: 16-alpine (otimizada para velocidade e tamanho).
     * O JUnit 5 detecta esta variável estática e sobe o container antes de qualquer teste.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Injeção Dinâmica de Propriedades.
     * Como o Docker sobe o banco em uma porta aleatória do host para evitar conflitos,
     * precisamos dizer ao Spring em tempo de execução qual é a URL, usuário e senha
     * gerados pelo Testcontainers.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    /**
     * JdbcTemplate é utilizado aqui por ser uma abstração de baixo nível.
     * Ele nos permite interagir diretamente com o SQL sem passar pelas camadas do Hibernate,
     * o que é ideal para testes de diagnóstico de esquema.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Este teste imprime a hierarquia de templates e passos no console.
     * 
     * UTILIDADE:
     * Em sistemas complexos de workflow, é comum haver erros nas tabelas de transição
     * (quem pode ir para qual passo). Este teste serve como um "mapa visual" para
     * o desenvolvedor confirmar que o SQL de migração (Flyway) fez o que deveria.
     */
    @Test
    @DisplayName("Listar Templates e seus Passos de Workflow")
    public void printTemplatesAndSteps() {
        // Título formatado para facilitar a leitura no log do Maven/IDE
        System.out.println("\n" + "=".repeat(50));
        System.out.println("RELATÓRIO DE ESTRUTURA DE WORKFLOW (MODO TESTE)");
        System.out.println("=".repeat(50));

        // Busca os últimos 5 templates cadastrados para amostragem
        List<Map<String, Object>> templates = jdbcTemplate.queryForList(
            "SELECT id, name, requires_signature FROM tb_task_templates ORDER BY id DESC LIMIT 5"
        );

        for (Map<String, Object> t : templates) {
            System.out.println(String.format("Template [%d]: %s (Assinatura: %b)", 
                t.get("id"), t.get("name"), t.get("requires_signature")));
            
            // Busca a relação de passos para este template específico
            // Join entre a tabela de relação (tsr) e a definição de passo (tsd)
            List<Map<String, Object>> steps = jdbcTemplate.queryForList(
                "SELECT tsd.id, tsd.label, tsr.step_order, tsr.is_global, tsr.id as tsr_id " +
                "FROM tb_template_step_relations tsr " +
                "JOIN tb_task_step_definitions tsd ON tsr.task_step_definition_id = tsd.id " +
                "WHERE tsr.task_template_id = ? ORDER BY tsr.step_order", t.get("id")
            );
            
            for (Map<String, Object> s : steps) {
                System.out.println(String.format("  -> Passo %d: %s [Ordem: %d, Global: %b]", 
                    s.get("id"), s.get("label"), s.get("step_order"), s.get("is_global")));
                
                // Busca quais são os próximos passos permitidos a partir deste passo atual
                // Isso valida a integridade da máquina de estados do NeoKioski
                List<Map<String, Object>> nextSteps = jdbcTemplate.queryForList(
                    "SELECT next_step_definition_id FROM tb_template_step_next_allowed WHERE template_step_relation_id = ?", 
                    s.get("tsr_id")
                );
                
                if (!nextSteps.isEmpty()) {
                    System.out.print("     Próximos passos permitidos: ");
                    nextSteps.forEach(ns -> System.out.print(ns.get("next_step_definition_id") + " "));
                    System.out.println();
                }
            }
        }
        System.out.println("=".repeat(50) + "\n");
    }
}
