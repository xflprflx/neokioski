package com.xflprflx.neokioski.models;

import jakarta.persistence.Embeddable;

@Embeddable
/**
 * Entidade auxiliar para configuração das colunas que devem ser exibidas em um relatório customizado.
 */
public class ReportColumnConfig {
    
    private String attributeKey;
    private String label;

    public ReportColumnConfig() {
    }

    public ReportColumnConfig(String attributeKey, String label) {
        this.attributeKey = attributeKey;
        this.label = label;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
