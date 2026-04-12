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

@Component
public class SecurityService {

    private final UserRepository userRepository;
    private final com.xflprflx.neokioski.services.PresenceService presenceService;

    public SecurityService(UserRepository userRepository, com.xflprflx.neokioski.services.PresenceService presenceService) {
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    public Optional<UserDetails> getAuthenticatedUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Object principal = context.getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return Optional.of((UserDetails) principal);
        }
        return Optional.empty();
    }

    public Optional<User> getAuthenticatedInternalUser() {
        return getAuthenticatedUser()
                .flatMap(userDetails -> userRepository.findByUsername(userDetails.getUsername()));
    }

    public void logout() {
        getAuthenticatedInternalUser().ifPresent(u -> presenceService.forceOffline(u.getId()));
        UI.getCurrent().getPage().setLocation("/");
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
    }
}
