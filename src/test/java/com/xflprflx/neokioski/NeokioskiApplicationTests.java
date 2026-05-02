package com.xflprflx.neokioski;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.DisplayName;

/**
 * Teste de Fumaça (Smoke Test) do Contexto da Aplicação.
 * 
 * O QUE ESTE TESTE VALIDA?
 * Este é o "ground zero" da estabilidade do sistema. Ele verifica se o bootstrapping
 * do Spring Boot ocorre sem falhas. Isso inclui:
 * 1. Parse de arquivos de configuração (application.yml).
 * 2. Criação de Beans e Injeção de Dependências (@Autowired).
 * 3. Validação do Modelo de Dados JPA (Hibernate) contra o banco real.
 * 4. Execução completa das migrações de banco (Flyway).
 * 
 * POR QUE TESTCONTAINERS AQUI?
 * Como a aplicação NeoKioski depende fortemente do JPA/Hibernate para iniciar,
 * ela falhará se não houver um banco de dados ativo. Usar o PostgreSQL real
 * no Docker garante que não teremos falsos-positivos causados por diferenças
 * entre dialetos de banco (ex: H2 vs Postgres).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Validação de Carregamento do Contexto Spring Boot")
class NeokioskiApplicationTests {

	/**
	 * Instância do PostgreSQL rodando em container isolado.
	 * O JUnit garante que este banco esteja pronto antes do Spring tentar carregar o contexto.
	 */
	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	/**
	 * Registra as credenciais e porta aleatória do banco gerado pelo Testcontainers
	 * no ambiente de propriedades do Spring. Isso substitui os placeholders de URL/User/Pass.
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
	 * Teste unitário automático gerado pelo Spring Boot para validar se o
	 * Application Context (contexto da aplicação) sobe corretamente.
	 */
	@Test
	@DisplayName("O contexto deve carregar sem erros (Context Loads)")
	void contextLoads() {
		/*
		 * O bloco está vazio intencionalmente. Apenas a execução bem-sucedida
		 * deste métodx com a anotação @SpringBootTest já comprova que o contexto
		 * foi carregado sem problemas.
		 */
	}

}
