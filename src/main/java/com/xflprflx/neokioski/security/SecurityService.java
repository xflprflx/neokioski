package com.xflprflx.neokioski.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.UserRepository;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Serviço responsável por gerenciar a segurança e o contexto de autenticação do usuário.
 * Oferece métodos para obter as credenciais do usuário logado e realizar logout.
 */
@Component
public class SecurityService {

    private final UserRepository userRepository;
    private final com.xflprflx.neokioski.services.PresenceService presenceService;

    /**
     * Construtor da classe de serviço de segurança.
     *
     * @param userRepository Repositório de usuários para consultas de acesso.
     * @param presenceService Serviço de presença para atualização de status ao realizar o logout.
     */
    public SecurityService(UserRepository userRepository, com.xflprflx.neokioski.services.PresenceService presenceService) {
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    /**
     * Obtém os detalhes do usuário autenticado a partir do contexto de segurança do Spring.
     *
     * @return Um Optional contendo o {@link UserDetails} do usuário atual ou vazio, caso não autenticado.
     */
    public Optional<UserDetails> getAuthenticatedUser() {
        /*
         * Acessa o contexto de segurança vigente para extrair as credenciais e
         * as informações do principal autenticado.
         */
        SecurityContext context = SecurityContextHolder.getContext();
        Object principal = context.getAuthentication().getPrincipal();

        /*
         * Valida se o objeto principal resgatado é, de fato, uma instância do UserDetails,
         * o que indica uma autenticação válida dentro do padrão.
         */
        if (principal instanceof UserDetails) {
            return Optional.of((UserDetails) principal);
        }
        return Optional.empty();
    }

    /**
     * Recupera a entidade interna do usuário (do banco de dados) que corresponde
     * aos detalhes de autenticação atual do sistema.
     *
     * @return Um Optional contendo a entidade {@link User} mapeada no banco.
     */
    public Optional<User> getAuthenticatedInternalUser() {
        /*
         * Utiliza o detalhe do usuário autenticado e realiza uma busca no
         * repositório utilizando o username (nome de usuário).
         */
        return getAuthenticatedUser()
                .flatMap(userDetails -> userRepository.findByUsername(userDetails.getUsername()));
    }

    /**
     * Realiza o processo de desconexão (logout) do usuário atual.
     * Atualiza seu status de presença para offline, invalida a sessão de segurança
     * e redireciona o usuário para a página inicial.
     */
    public void logout() {
        /*
         * Verifica se o usuário possui sessão ativa e, se positivo, força o status
         * de presença para offline.
         */
        getAuthenticatedInternalUser().ifPresent(u -> presenceService.forceOffline(u.getId()));

        /*
         * Redireciona a interface de usuário baseada no Vaadin para o contexto raiz (/).
         */
        UI.getCurrent().getPage().setLocation("/");

        /*
         * Instancia o manipulador de logout nativo do Spring Security e aciona
         * a limpeza do contexto passando a requisição corrente.
         */
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
    }
}
