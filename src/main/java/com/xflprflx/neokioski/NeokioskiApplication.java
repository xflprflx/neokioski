package com.xflprflx.neokioski;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.component.page.Push;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principal responsável por inicializar a aplicação Neokioski.
 * Configura o Spring Boot, o suporte a WebSockets via Vaadin (@Push),
 * agendamento de tarefas e o tema visual da interface.
 */
@Push
@EnableScheduling
@SpringBootApplication
@Theme(value = "neokioski")
public class NeokioskiApplication implements AppShellConfigurator {

	/**
	 * Método principal (entry point) da aplicação.
	 *
	 * @param args Argumentos de linha de comando fornecidos durante a execução.
	 */
	public static void main(String[] args) {
		/*
		 * Inicia o contexto da aplicação Spring Boot passando a classe
		 * NeokioskiApplication como referência de configuração primária e
		 * repassando os argumentos recebidos.
		 */
		SpringApplication.run(NeokioskiApplication.class, args);
	}

}
