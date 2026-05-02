package com.xflprflx.neokioski.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import com.xflprflx.neokioski.repositories.UserRepository;
import com.xflprflx.neokioski.ui.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança global da aplicação.
 * Utiliza o Spring Security em conjunto com a integração para Vaadin.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configura a cadeia de filtros de segurança (Security Filter Chain) e
     * integra com o Vaadin.
     *
     * @param http O construtor de segurança HTTP do Spring.
     * @return A cadeia construída contendo as permissões e restrições.
     * @throws Exception Em caso de erro na configuração das rotas de segurança.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        /*
         * Permite o acesso irrestrito ao console do H2 Database para facilitar os
         * testes de desenvolvimento, bem como acesso público aos arquivos estáticos
         * de áudio na pasta /sounds/**.
         */
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/sounds/**").permitAll());

        /*
         * Desativa a proteção CSRF apenas para os endpoints do console H2,
         * já que ele necessita de postbacks e não é compatível nativamente.
         */
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));

        /*
         * Configura os headers de resposta HTTP, especificamente ajustando o
         * frameOptions para permitir iframe na mesma origem (necessário pro H2 console funcionar
         * adequadamente no browser).
         */
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        /*
         * Mágica do Vaadin: Integra o Vaadin ao Spring Security avisando qual
         * será a página de visualização oficial para a realização de login (LoginView.class).
         * Essa configuração configura implicitamente o restante do tráfego para exigir autenticação.
         */
        http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin
                .loginView(LoginView.class));

        /*
         * Constrói e finaliza a configuração da cadeia de segurança (SecurityFilterChain).
         */
        return http.build();
    }

    /**
     * Serviço responsável por buscar os usuários armazenados no banco de dados
     * durante o processo de tentativa de login (autenticação).
     *
     * @param userRepository Repositório de dados onde os usuários estão armazenados.
     * @return A implementação funcional de {@link UserDetailsService} contendo a lógica de busca.
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        /*
         * Retorna uma função lambda que pesquisa o usuário pelo nome de login.
         * Mapeia a entidade do domínio para o objeto de User do Spring Security.
         * Define a role e o status (desabilitado se isActive for falso ou se estiver deletado).
         * Caso contrário, lança UsernameNotFoundException sinalizando falha.
         */
        return username -> userRepository.findByUsername(username)
                .filter(u -> !u.isDeleted()) // <--- Adicionado: Ignora o usuário se estiver marcado como deletado
                .map(u -> User.builder()
                        .username(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRole().name())
                        .disabled(!u.isActive())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado ou desativado."));
    }

    /**
     * Define a utilização do algoritmo BCrypt como o codificador (encoder) oficial
     * para o hash de senhas no sistema.
     *
     * @return Uma instância de {@link PasswordEncoder} configurada como {@link BCryptPasswordEncoder}.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        /*
         * Cria e retorna o encoder responsável pela criptografia segura.
         */
        return new BCryptPasswordEncoder();
    }
}
