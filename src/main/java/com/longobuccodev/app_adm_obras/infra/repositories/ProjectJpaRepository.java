package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, UUID> {

    Optional<ProjectEntity> findFirstByOs(String os);

    @Query("""
            SELECT project FROM ProjectEntity project
            WHERE project.client.id = :clientId
            """)
    List<ProjectEntity> findByClientId(@Param("clientId") UUID clientId);

    @Query("""
            SELECT project FROM ProjectEntity project
            WHERE project.costCenter.id = :costCenterId
            """)
    List<ProjectEntity> findByCostCenterId(@Param("costCenterId") UUID costCenterId);

    @Query("""
            SELECT DISTINCT project FROM ProjectEntity project
            JOIN project.employees employee
            WHERE employee.id = :employeeId
            """)
    List<ProjectEntity> findByEmployeeId(@Param("employeeId") UUID employeeId);

    List<ProjectEntity> findByStartDate(LocalDate startDate);

    List<ProjectEntity> findByEndDate(LocalDate endDate);

    List<ProjectEntity> findByStartDateBetween(LocalDate startDate, LocalDate endDate);
}
