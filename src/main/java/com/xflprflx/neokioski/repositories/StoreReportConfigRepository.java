package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.StoreReportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StoreReportConfigRepository extends JpaRepository<StoreReportConfig, Long> {

    @Query("SELECT c FROM StoreReportConfig c " +
           "WHERE c.startDate <= :now " +
           "AND (c.endDate IS NULL OR c.endDate >= :now) " +
           "ORDER BY c.startDate DESC")
    List<StoreReportConfig> findActiveConfigs(@Param("now") LocalDate now);

    default Optional<StoreReportConfig> findActiveConfig(LocalDate now) {
        return findActiveConfigs(now).stream().findFirst();
    }
}
