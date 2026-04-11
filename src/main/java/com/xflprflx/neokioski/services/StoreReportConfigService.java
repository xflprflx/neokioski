package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.StoreReportConfig;
import com.xflprflx.neokioski.repositories.StoreReportConfigRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class StoreReportConfigService {

    private final StoreReportConfigRepository repository;

    public StoreReportConfigService(StoreReportConfigRepository repository) {
        this.repository = repository;
    }

    public List<StoreReportConfig> findAll() {
        return repository.findAll();
    }

    public void save(StoreReportConfig config) {
        if (config == null) {
            return;
        }

        // Validação de sobreposição de vigência
        List<StoreReportConfig> all = repository.findAll();
        for (StoreReportConfig existing : all) {
            // Ignorar o próprio registro em caso de edição
            if (config.getId() != null && config.getId().equals(existing.getId())) {
                continue;
            }

            LocalDate nStart = config.getStartDate();
            LocalDate nEnd = config.getEndDate() != null ? config.getEndDate() : LocalDate.MAX;
            LocalDate eStart = existing.getStartDate();
            LocalDate eEnd = existing.getEndDate() != null ? existing.getEndDate() : LocalDate.MAX;

            // Regra de sobreposição: (Início1 <= Fim2) E (Início2 <= Fim1)
            if (!nStart.isAfter(eEnd) && !eStart.isAfter(nEnd)) {
                String period = existing.getStartDate().toString() + " até " + 
                               (existing.getEndDate() != null ? existing.getEndDate().toString() : "Indefinido");
                throw new RuntimeException("Conflito de vigência: O período selecionado se sobrepõe ao modelo '" + 
                                          existing.getIdentifier() + "' (" + period + ").");
            }
        }

        repository.save(config);
    }

    public void delete(StoreReportConfig config) {
        if (config == null) {
            return;
        }
        repository.delete(config);
    }
}
