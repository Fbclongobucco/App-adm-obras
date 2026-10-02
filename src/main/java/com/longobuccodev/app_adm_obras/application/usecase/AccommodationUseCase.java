package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.AccommodationMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.repository.AccommodationRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AccommodationUseCase {

    private final AccommodationRepository accommodationRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    public AccommodationUseCase(AccommodationRepository accommodationRepository,
                                ProjectRepository projectRepository,
                                EmployeeRepository employeeRepository) {
        this.accommodationRepository = accommodationRepository;
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
    }

    public AccommodationResponseDTO create(AccommodationRequestDTO dto) {
        Accommodation accommodation = AccommodationMapper.toDomain(dto, findProject(dto.projectId()),
                findEmployees(dto.employeeIds()));
        return AccommodationMapper.toResponse(accommodationRepository.save(accommodation));
    }

    public AccommodationResponseDTO findById(UUID id) {
        return AccommodationMapper.toResponse(findAccommodation(id));
    }

    public List<AccommodationResponseDTO> findAll() {
        return accommodationRepository.findAll().stream()
                .map(AccommodationMapper::toResponse)
                .toList();
    }

    public PageResponseDTO<AccommodationResponseDTO> findAll(PageRequest request) {
        return PageMapper.toResponse(accommodationRepository.findAll(request), AccommodationMapper::toResponse);
    }

    public List<AccommodationResponseDTO> findByProjectId(UUID projectId) {
        return accommodationRepository.findByProjectId(projectId).stream()
                .map(AccommodationMapper::toResponse)
                .toList();
    }

    public AccommodationResponseDTO update(UUID id, AccommodationRequestDTO dto) {
        Accommodation existing = findAccommodation(id);
        Accommodation updated = AccommodationMapper.toDomain(existing, dto, findProject(dto.projectId()),
                findEmployees(dto.employeeIds()));
        accommodationRepository.update(id, updated);
        return AccommodationMapper.toResponse(updated);
    }

    public void delete(UUID id) {
        findAccommodation(id);
        accommodationRepository.deleteById(id);
    }

    private Accommodation findAccommodation(UUID id) {
        return EntityLookup.require(id, accommodationRepository::findById, ResourceNotFoundException::accommodation);
    }

    private Project findProject(UUID projectId) {
        return EntityLookup.optional(projectId, projectRepository::findById, ResourceNotFoundException::project);
    }

    private Set<Employee> findEmployees(Set<UUID> employeeIds) {
        return EntityLookup.all(employeeIds, employeeRepository::findById, ResourceNotFoundException::employee);
    }
}
