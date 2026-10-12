package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ProjectRepository {

    /** Campos pelos quais a listagem paginada de obras pode ser ordenada. */
    Set<String> SORT_FIELDS = Set.of("updatedAt", "createdAt", "os", "startDate");


    Project save(Project project);
    Project findById(UUID id);
    Project findByOs(String os);
    List<Project> findAll();
    default Page<Project> findAll(PageRequest request) {
        return findAll(request, SearchTerm.NONE);
    }

    /** Pagina filtrada pelo termo nos campos pesquisaveis do recurso; {@link SearchTerm#NONE} nao filtra. */
    Page<Project> findAll(PageRequest request, SearchTerm term);
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
