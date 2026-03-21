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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configura a cadeia de filtros de segurança (Security Filter Chain) e
     * integra com o Vaadin.
     *
     * @param http O construtor de segurança HTTP do Spring.
     * @return A cadeia construída.
     * @throws Exception Em caso de erro na configuração.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Permite o console do H2 para facilitar testes
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll());

        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        // Mágica do Vaadin: Avisa o Spring Security que a view oficial é o LoginView
        http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin
                .loginView(LoginView.class));

        return http.build();
    }

    /**
     * Serviço que busca os usuários do banco de dados na hora do login.
     *
     * @param userRepository Repositório de usuários.
     * @return A implementação do {@link UserDetailsService}.
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(u -> User.builder()
                        .username(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }

    /**
     * Define o BCrypt como codificador oficial de senhas.
     *
     * @return O {@link PasswordEncoder}.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
