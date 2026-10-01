package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Project;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProjectRepository {

    Project save(Project project);
    Project findById(UUID id);
    Project findByName(String name);
    List<Project> findAll();
    List<Project> findByClient(Client client);
    List<Project> findByCostCenter(CostCenter costCenter);
    List<Project> findByEmployee(Employee employee);
    List<Project> findByStartDate(LocalDate startDate);
    List<Project> findByEndDate(LocalDate endDate);
    List<Project> findByDateBetween(LocalDate startDate, LocalDate endDate);
    void delete(Project project);
    void deleteById(UUID id);
    void update(Project project);

}
