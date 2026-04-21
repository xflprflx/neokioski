package com.xflprflx.neokioski.config;

import tools.jackson.datatype.hibernate7.Hibernate7Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração para o Jackson lidar corretamente com entidades Lazy do Hibernate.
 * Isso evita a tentativa de serializar proxies não inicializados, prevenindo LazyInitializationException
 * e garantindo que apenas dados carregados pelo EntityGraph sejam enviados para a UI/API.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate7Module hibernate7Module() {
        Hibernate7Module module = new Hibernate7Module();
        // Configurações adicionais se necessário, o padrão é seguro
        return module;
    }
}
