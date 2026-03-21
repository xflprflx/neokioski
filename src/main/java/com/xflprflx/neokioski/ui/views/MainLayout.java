package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

/**
 * Layout principal da aplicação que define o cabeçalho (Header) e o menu lateral (Drawer).
 * Todas as outras vistas (@Route) são renderizadas dentro deste layout.
 */
@PermitAll // Garante que apenas usuários autenticados vejam o layout base
public class MainLayout extends AppLayout {

    private final transient AuthenticationContext authContext;

    /**
     * Construtor que monta a estrutura básica da página.
     */
    public MainLayout(AuthenticationContext authContext) {
        this.authContext = authContext;
        createHeader(); // Cria o topo da página
        createDrawer(); // Cria a barra de navegação lateral
    }

    /**
     * Configura o cabeçalho com o logo, o botão de menu e o botão de sair.
     */
    private void createHeader() {
        H1 logo = new H1("NeoKioski"); // Título/Logo da aplicação
        // Aplica classes de utilidade do tema Lumo para estilização rápida
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        Button logout = new Button("Sair");
        logout.addClickListener(event -> authContext.logout());
        logout.getStyle().set("cursor", "pointer"); // Muda o cursor ao passar o mouse
        logout.getStyle().set("margin-right", "20px"); // Espaçamento à direita

        // HorizontalLayout organiza o DrawerToggle, o Logo e o Logout em linha
        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER); // Alinha verticalmente ao centro
        header.expand(logo); // Faz o logo ocupar o espaço central disponível, empurrando o logout para a direita
        header.setWidthFull(); // Ocupa 100% da largura
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header); // Adiciona o componente criado à barra de topo do AppLayout
    }

    /**
     * Configura o menu de navegação lateral (Drawer).
     */
    private void createDrawer() {
        SideNav nav = new SideNav(); // Componente de navegação lateral do Vaadin
        // Adiciona links para as rotas da aplicação com ícones correspondentes



        // Scroller permite rolagem no menu caso haja muitos itens
        Scroller scroller = new Scroller(nav);
        scroller.setClassName(LumoUtility.Padding.SMALL);

        addToDrawer(scroller); // Adiciona o scroller à área lateral expandível
    }
}
