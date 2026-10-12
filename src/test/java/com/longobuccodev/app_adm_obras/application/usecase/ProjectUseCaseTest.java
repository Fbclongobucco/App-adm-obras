package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.project.ProjectResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.core.domain.Accommodation;
import com.longobuccodev.app_adm_obras.core.domain.Client;
import com.longobuccodev.app_adm_obras.core.domain.CostCenter;
import com.longobuccodev.app_adm_obras.core.domain.Employee;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.exception.InvalidProjectException;
import com.longobuccodev.app_adm_obras.core.repository.AccommodationRepository;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.accommodation;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.client;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.costCenter;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.employee;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.meal;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.project;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.projectRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectUseCaseTest {

    static final Instant CLOCK_NOW = Instant.parse("2026-01-15T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(CLOCK_NOW, ZoneOffset.UTC);

    private ProjectRepository projectRepository;
    private CostCenterRepository costCenterRepository;
    private ClientRepository clientRepository;
    private AccommodationRepository accommodationRepository;
    private EmployeeRepository employeeRepository;
    private MealRepository mealRepository;
    private ProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        costCenterRepository = mock(CostCenterRepository.class);
        clientRepository = mock(ClientRepository.class);
        accommodationRepository = mock(AccommodationRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        mealRepository = mock(MealRepository.class);
        useCase = new ProjectUseCase(projectRepository, costCenterRepository, clientRepository,
                accommodationRepository, employeeRepository, mealRepository, CLOCK);
    }

    @Test
    void shouldCreateProjectResolvingRelations() {
        CostCenter costCenter = costCenter();
        Client client = client();
        Accommodation accommodation = accommodation();
        Employee employee = employee();
        when(costCenterRepository.findById(costCenter.getId())).thenReturn(costCenter);
        when(clientRepository.findById(client.getId())).thenReturn(client);
        when(accommodationRepository.findById(accommodation.getId())).thenReturn(accommodation);
        when(employeeRepository.findById(employee.getId())).thenReturn(employee);
        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponseDTO response = useCase.create(projectRequest(costCenter.getId(), client.getId(),
                Set.of(accommodation.getId()), Set.of(employee.getId()), null, null));

        assertThat(response.costCenter().id()).isEqualTo(costCenter.getId());
        assertThat(response.client().id()).isEqualTo(client.getId());
        assertThat(response.accommodations()).singleElement()
                .satisfies(summary -> assertThat(summary.id()).isEqualTo(accommodation.getId()));
        assertThat(response.employees()).singleElement()
                .satisfies(summary -> assertThat(summary.id()).isEqualTo(employee.getId()));
        assertThat(response.lunch()).isNull();
        assertThat(response.totalPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldRejectMealIdOnCreateBecauseNewProjectHasNoMeals() {
        CostCenter costCenter = costCenter();
        Client client = client();
        Meal lunch = meal(MealType.LUNCH);
        when(costCenterRepository.findById(costCenter.getId())).thenReturn(costCenter);
        when(clientRepository.findById(client.getId())).thenReturn(client);
        when(mealRepository.findById(lunch.getId())).thenReturn(lunch);

        assertThatThrownBy(() -> useCase.create(projectRequest(costCenter.getId(), client.getId(), null, null,
                lunch.getId(), null)))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.meal");
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateShouldAcceptOwnMealAndRejectMealFromAnotherProject() {
        CostCenter costCenter = costCenter();
        Client client = client();
        Project existing = project();
        Meal ownLunch = meal(MealType.LUNCH, existing);
        Meal foreignLunch = meal(MealType.LUNCH, project());
        existing.setLunch(ownLunch);
        existing.markCreated(CLOCK_NOW);
        when(projectRepository.findById(existing.getId())).thenReturn(existing);
        when(costCenterRepository.findById(costCenter.getId())).thenReturn(costCenter);
        when(clientRepository.findById(client.getId())).thenReturn(client);
        when(mealRepository.findById(ownLunch.getId())).thenReturn(ownLunch);
        when(mealRepository.findById(foreignLunch.getId())).thenReturn(foreignLunch);

        assertThat(useCase.update(existing.getId(), projectRequest(costCenter.getId(), client.getId(), null, null,
                ownLunch.getId(), null)).lunch().id()).isEqualTo(ownLunch.getId());
        assertThatThrownBy(() -> useCase.update(existing.getId(), projectRequest(costCenter.getId(),
                client.getId(), null, null, foreignLunch.getId(), null)))
                .isInstanceOf(InvalidProjectException.class)
                .extracting("errorCode").isEqualTo("project.invalid.meal");
    }

    @Test
    void updateWithoutMealIdsShouldKeepCurrentMeals() {
        CostCenter costCenter = costCenter();
        Client client = client();
        Project existing = project();
        Meal lunch = meal(MealType.LUNCH, existing);
        existing.setLunch(lunch);
        existing.markCreated(CLOCK_NOW);
        when(projectRepository.findById(existing.getId())).thenReturn(existing);
        when(costCenterRepository.findById(costCenter.getId())).thenReturn(costCenter);
        when(clientRepository.findById(client.getId())).thenReturn(client);

        ProjectResponseDTO response = useCase.update(existing.getId(), projectRequest(costCenter.getId(),
                client.getId(), null, null, null, null));

        assertThat(response.lunch().id()).isEqualTo(lunch.getId());
    }

    @Test
    void shouldRejectCreateWithUnknownClient() {
        CostCenter costCenter = costCenter();
        when(costCenterRepository.findById(costCenter.getId())).thenReturn(costCenter);

        assertThatThrownBy(() -> useCase.create(projectRequest(costCenter.getId(), UUID.randomUUID(), null, null,
                null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("client.not_found");
        verify(projectRepository, never()).save(any());
    }

    @Test
    void shouldUpdateKeepingId() {
        Project existing = project();
        CostCenter costCenter = costCenter();
        Client client = client();
        when(projectRepository.findById(existing.getId())).thenReturn(existing);
        when(costCenterRepository.findById(costCenter.getId())).thenReturn(costCenter);
        when(clientRepository.findById(client.getId())).thenReturn(client);

        ProjectResponseDTO response = useCase.update(existing.getId(),
                projectRequest(costCenter.getId(), client.getId(), null, null, null, null));

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(projectRepository).update(any(Project.class));
    }

    @Test
    void shouldRejectFindByUnknownId() {
        assertThatThrownBy(() -> useCase.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("project.not_found");
    }
}
