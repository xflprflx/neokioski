package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.html.H1;
import com.xflprflx.neokioski.ui.components.VersionLabel;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View de Login.
 * Utiliza o componente {@link LoginForm} do Vaadin para uma experiência de
 * usuário padronizada.
 */
@Route("login")
@PageTitle("Login | NeoKioski")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger logger = LoggerFactory.getLogger(LoginView.class);
    private final LoginForm login = new LoginForm();

    /**
     * Construtor da vista de login.
     */
    public LoginView() {
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Estilização do formulário (I18n em PT-BR)
        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form i18nForm = i18n.getForm();
        i18nForm.setTitle("Login");
        i18nForm.setUsername("Usuário");
        i18nForm.setPassword("Senha");
        i18nForm.setSubmit("Entrar");
        i18nForm.setForgotPassword("Esqueceu a senha?");
        i18n.setForm(i18nForm);

        LoginI18n.ErrorMessage i18nError = i18n.getErrorMessage();
        i18nError.setTitle("Usuário ou senha inválidos");
        i18nError.setMessage("Verifique suas credenciais e tente novamente.");
        i18n.setErrorMessage(i18nError);

        login.setI18n(i18n);

        // Define a ação de submissão para o endpoint padrão do Spring Security
        // ("login")
        // Isso fará com que o formulário realize um POST para /login
        login.setAction("login");

        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(new H1("NeoKioski"), login, new VersionLabel());
    }

    @Override
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        // Se houver erro na URL (como ?error), habilita o estado de erro no formulário
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}
