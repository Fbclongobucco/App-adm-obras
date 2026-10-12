package com.longobuccodev.app_adm_obras.infra.adapters.mapper;

import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.infra.entities.CostCenterEntity;
import com.longobuccodev.app_adm_obras.infra.entities.EmployeeEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;

import java.util.Collection;
import java.util.List;

public final class EmployeeEntityMapper {

    private EmployeeEntityMapper() {
    }

    public static Employee toDomain(EmployeeEntity entity) {
        return toDomain(entity, List.of());
    }

    public static Employee toDomain(EmployeeEntity entity, Collection<ProjectEntity> projects) {
        if (entity == null) {
            return null;
        }
        Employee employee = new Employee(entity.getId(), entity.getName(), entity.getEmail(), entity.getCpf(),
                entity.getPhone(), AddressEntityMapper.toDomain(entity.getAddress()),
                entity.getBirthDate(), CostCenterEntityMapper.toDomain(entity.getCostCenter()), entity.getRole());
        if (projects != null) {
            projects.forEach(project -> employee.addProject(ProjectEntityMapper.toDomainShallow(project)));
        }
        return AuditMapping.restore(employee, entity);
    }

    /**
     * @param costCenter referencia gerenciada ao centro de custo do dominio (ou null); o colaborador
     *                   so aponta para ela, sem copiar dados para nenhum centro de custo
     */
    public static EmployeeEntity toEntity(Employee domain, CostCenterEntity costCenter) {
        if (domain == null) {
            return null;
        }
        EmployeeEntity entity = EmployeeEntity.create();
        entity.setId(domain.getId());
        apply(domain, entity, costCenter);
        return entity;
    }

    public static void apply(Employee domain, EmployeeEntity entity, CostCenterEntity costCenter) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setName(domain.getName());
        entity.setEmail(domain.getEmail());
        entity.setCpf(domain.getCpf());
        entity.setPhone(domain.getPhone());
        entity.setBirthDate(domain.getBirthDate());
        entity.setRole(domain.getRole());
        AuditMapping.copy(domain, entity);
        if (domain.getAddress() == null) {
            entity.setAddress(null);
        } else if (entity.getAddress() == null) {
            entity.setAddress(AddressEntityMapper.toEntity(domain.getAddress()));
        } else {
            AddressEntityMapper.apply(domain.getAddress(), entity.getAddress());
        }
        entity.setCostCenter(costCenter);
    }
}
