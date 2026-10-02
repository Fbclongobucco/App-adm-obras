package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.accommodation.AccommodationResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.application.mapper.AccommodationMapper;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.repository.AccommodationRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccommodationUseCaseTest {

    private AccommodationRepository accommodationRepository;
    private ProjectRepository projectRepository;
    private EmployeeRepository employeeRepository;
    private AccommodationUseCase useCase;

    @BeforeEach
    void setUp() {
        accommodationRepository = mock(AccommodationRepository.class);
        projectRepository = mock(ProjectRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        useCase = new AccommodationUseCase(accommodationRepository, projectRepository, employeeRepository);
    }

    private static AccommodationRequestDTO newRequest(UUID projectId, Set<UUID> employeeIds) {
        return new AccommodationRequestDTO("Maria Souza", "11988887777",
                new AddressRequestDTO("Rua das Flores", "120", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100"),
                8, 30, false, projectId, employeeIds, new BigDecimal("4500.00"));
    }

    @Test
    void shouldCreateAccommodation() {
        when(accommodationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccommodationResponseDTO response = useCase.create(newRequest(null, null));

        assertThat(response.id()).isNotNull();
        assertThat(response.hostName()).isEqualTo("Maria Souza");
        verify(accommodationRepository).save(any(Accommodation.class));
    }

    @Test
    void shouldRejectCreateWithUnknownProject() {
        UUID projectId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.create(newRequest(projectId, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("project.not_found");
        verify(accommodationRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWithUnknownEmployee() {
        UUID employeeId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.create(newRequest(null, Set.of(employeeId))))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("employee.not_found");
        verify(accommodationRepository, never()).save(any());
    }

    @Test
    void shouldFindById() {
        Accommodation accommodation = AccommodationMapper.toDomain(newRequest(null, null), null, null);
        when(accommodationRepository.findById(accommodation.getId())).thenReturn(accommodation);

        assertThat(useCase.findById(accommodation.getId()).id()).isEqualTo(accommodation.getId());
    }

    @Test
    void shouldRejectFindByUnknownId() {
        assertThatThrownBy(() -> useCase.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("accommodation.not_found");
    }

    @Test
    void shouldFindAll() {
        when(accommodationRepository.findAll()).thenReturn(List.of(
                AccommodationMapper.toDomain(newRequest(null, null), null, null),
                AccommodationMapper.toDomain(newRequest(null, null), null, null)));

        assertThat(useCase.findAll()).hasSize(2);
    }

    @Test
    void shouldUpdateKeepingId() {
        Accommodation existing = AccommodationMapper.toDomain(newRequest(null, null), null, null);
        when(accommodationRepository.findById(existing.getId())).thenReturn(existing);
        AccommodationRequestDTO request = new AccommodationRequestDTO("Jose Lima", "11988887778",
                new AddressRequestDTO("Rua das Flores", "121", "Sao Paulo", "SP", "Brasil", "Centro", "01310-100"),
                4, 15, true, null, null, new BigDecimal("500.50"));

        AccommodationResponseDTO response = useCase.update(existing.getId(), request);

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.hostName()).isEqualTo("Jose Lima");
        assertThat(response.address().id()).isEqualTo(existing.getAddress().getId());
        verify(accommodationRepository).update(eq(existing.getId()), any(Accommodation.class));
    }

    @Test
    void shouldDeleteExistingAccommodation() {
        Accommodation existing = AccommodationMapper.toDomain(newRequest(null, null), null, null);
        when(accommodationRepository.findById(existing.getId())).thenReturn(existing);

        useCase.delete(existing.getId());

        verify(accommodationRepository).deleteById(existing.getId());
    }

    @Test
    void shouldRejectDeleteOfUnknownAccommodation() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(accommodationRepository, never()).deleteById(any());
    }
}
