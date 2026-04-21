package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.StoreReportConfig;
import com.xflprflx.neokioski.repositories.StoreReportConfigRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
/**
 * Serviço para gerenciar as regras de negócio de configuração dos relatórios customizados de loja.
 */
public class StoreReportConfigService {

    private final StoreReportConfigRepository repository;

    public StoreReportConfigService(StoreReportConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<StoreReportConfig> findAll() {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return repository.findAll();
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void save(StoreReportConfig config) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (config == null) {
            return;
        }

        // Validação de sobreposição de vigência
        List<StoreReportConfig> all = repository.findAll();
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (StoreReportConfig existing : all) {
            // Ignorar o próprio registro em caso de edição
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (config.getId() != null && config.getId().equals(existing.getId())) {
                continue;
            }

            LocalDate nStart = config.getStartDate();
            LocalDate nEnd = config.getEndDate() != null ? config.getEndDate() : LocalDate.MAX;
            LocalDate eStart = existing.getStartDate();
            LocalDate eEnd = existing.getEndDate() != null ? existing.getEndDate() : LocalDate.MAX;

            // Regra de sobreposição: (Início1 <= Fim2) E (Início2 <= Fim1)
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (!nStart.isAfter(eEnd) && !eStart.isAfter(nEnd)) {
                String period = existing.getStartDate().toString() + " até " + 
                               (existing.getEndDate() != null ? existing.getEndDate().toString() : "Indefinido");
                throw new RuntimeException("Conflito de vigência: O período selecionado se sobrepõe ao modelo '" + 
                                          existing.getIdentifier() + "' (" + period + ").");
            }
        }

        repository.save(config);
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void delete(StoreReportConfig config) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (config == null) {
            return;
        }
        repository.delete(config);
    }
}
