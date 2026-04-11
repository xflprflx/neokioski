package com.xflprflx.neokioski.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tb_store_report_config")
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
}
