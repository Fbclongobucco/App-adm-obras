package com.longobuccodev.app_adm_obras.core.repository;

import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Project;

import java.util.List;
import java.util.UUID;

public interface EmployeeRepository {

    Employee save(Employee employee);
    Employee findById(UUID id);
    Employee findByName(String name);
    Employee findByCpf(String cpf);
    Employee findByEmail(String email);
    List<Employee> findByCostCenter(CostCenter costCenter);
    List<Employee> findByClient(Client client);
    List<Employee> findByProject(Project project);
    List<Employee> findAll();
    Page<Employee> findAll(PageRequest request);
    void delete(Employee employee);
    void deleteById(UUID id);
    void update(Employee employee);
}
