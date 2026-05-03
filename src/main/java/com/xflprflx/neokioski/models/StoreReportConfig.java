package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.enums.ReportOrientation;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_store_report_config")
/**
 * Entidade responsável por armazenar as configurações de relatório personalizadas de uma loja (ou unidade).
 */
public class StoreReportConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String identifier; // Ex: FRM-003921

    private String revision; // Ex: 2

    @Column(nullable = false)
    private String title; // Ex: Controle de Entrega Loja

    private String owner; // Ex: Juliano Souza (U112006)

    @Column(nullable = false)
    private String templateCode; // Ex: FRM-000922

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ReportOrientation orientation = ReportOrientation.PORTRAIT;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_store_report_config_columns", joinColumns = @JoinColumn(name = "config_id"))
    @OrderColumn(name = "order_index")
    private List<ReportColumnConfig> columns = new ArrayList<>();

    public StoreReportConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ReportOrientation getOrientation() {
        return orientation;
    }

    public void setOrientation(ReportOrientation orientation) {
        this.orientation = orientation;
    }

    public List<ReportColumnConfig> getColumns() {
        return columns;
    }

    public void setColumns(List<ReportColumnConfig> columns) {
        this.columns = columns;
    }
}
