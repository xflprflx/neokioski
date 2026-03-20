package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Início | NeoTMS")
@PermitAll
public class HomeView extends VerticalLayout {
    public HomeView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        H1 title = new H1("Bem-vindo ao NeoKioski");
        Paragraph description = new Paragraph(
                "Você foi autenticado com sucesso. Utilize o menu lateral para navegar pelo sistema.");

        add(title, description);
    }
}
