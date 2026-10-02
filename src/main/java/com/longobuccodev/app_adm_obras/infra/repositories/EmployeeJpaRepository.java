package com.longobuccodev.app_adm_obras.infra.repositories;

import com.longobuccodev.app_adm_obras.infra.entities.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, UUID> {

    Optional<EmployeeEntity> findFirstByName(String name);

    Optional<EmployeeEntity> findFirstByCpf(String cpf);

    Optional<EmployeeEntity> findFirstByEmail(String email);

    @Query("""
            SELECT employee FROM EmployeeEntity employee
            WHERE employee.costCenter.id = :costCenterId
            """)
    List<EmployeeEntity> findByCostCenterId(@Param("costCenterId") UUID costCenterId);

    @Query("""
            SELECT DISTINCT employee FROM EmployeeEntity employee
            JOIN ProjectEntity project ON employee MEMBER OF project.employees
            WHERE project.client.id = :clientId
            """)
    List<EmployeeEntity> findByClientId(@Param("clientId") UUID clientId);

    @Query("""
            SELECT employee FROM EmployeeEntity employee
            JOIN ProjectEntity project ON employee MEMBER OF project.employees
            WHERE project.id = :projectId
            """)
    List<EmployeeEntity> findByProjectId(@Param("projectId") UUID projectId);
}
