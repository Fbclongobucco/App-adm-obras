package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationRenewalRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.AccommodationMapper;
import com.longobuccodev.app_adm_obras.application.mapper.PageMapper;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.SearchTerm;
import com.longobuccodev.app_adm_obras.core.repository.AccommodationRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AccommodationUseCase {

    private final AccommodationRepository accommodationRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;

    public AccommodationUseCase(AccommodationRepository accommodationRepository,
                                ProjectRepository projectRepository,
                                EmployeeRepository employeeRepository,
                                Clock clock) {
        this.accommodationRepository = accommodationRepository;
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.clock = clock;
    }

    public AccommodationResponseDTO create(AccommodationRequestDTO dto) {
        Accommodation accommodation = AccommodationMapper.toDomain(dto, findProject(dto.projectId()),
                findEmployees(dto.employeeIds()));
        Instant now = clock.instant();
        accommodation.markCreated(now);
        accommodation.stampPeriods(now);
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

    /** Listagem filtrada pelo termo de busca; vazio ou nulo nao filtra. */
    public PageResponseDTO<AccommodationResponseDTO> findAll(PageRequest request, String query) {
        return PageMapper.toResponse(accommodationRepository.findAll(request, SearchTerm.of(query)), AccommodationMapper::toResponse);
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
        Instant now = clock.instant();
        updated.touchFrom(existing, now);
        updated.stampPeriods(now);
        accommodationRepository.update(id, updated);
        return AccommodationMapper.toResponse(updated);
    }

    /** Renova a locacao a partir do dia seguinte ao termino; campos ausentes usam a sugestao. */
    public AccommodationResponseDTO renew(UUID id, AccommodationRenewalRequestDTO dto) {
        Accommodation accommodation = findAccommodation(id);
        accommodation.renew(dto == null ? null : dto.endDate(), dto == null ? null : dto.price());
        return saveLease(id, accommodation);
    }

    /** Desfaz a ultima renovacao; o primeiro periodo nao sai. */
    public void undoRenewal(UUID id) {
        Accommodation accommodation = findAccommodation(id);
        accommodation.undoRenewal();
        saveLease(id, accommodation);
    }

    private AccommodationResponseDTO saveLease(UUID id, Accommodation accommodation) {
        Instant now = clock.instant();
        accommodation.stampPeriods(now);
        accommodation.touch(now);
        accommodationRepository.update(id, accommodation);
        return AccommodationMapper.toResponse(accommodation);
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
