package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.MealResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.core.domain.Meal.MealType;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.meal;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.mealRequest;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MealUseCaseTest {

    private MealRepository mealRepository;
    private ProjectRepository projectRepository;
    private MealUseCase useCase;

    @BeforeEach
    void setUp() {
        mealRepository = mock(MealRepository.class);
        projectRepository = mock(ProjectRepository.class);
        useCase = new MealUseCase(mealRepository, projectRepository);
    }

    @Test
    void shouldCreateMealWithProject() {
        Project project = project();
        when(projectRepository.findById(project.getId())).thenReturn(project);
        when(mealRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MealResponseDTO response = useCase.create(mealRequest(project.getId(), MealType.LUNCH));

        assertThat(response.project().id()).isEqualTo(project.getId());
        assertThat(response.project().client().name()).isEqualTo("Construtora Alfa");
    }

    @Test
    void shouldRejectCreateWithUnknownProject() {
        assertThatThrownBy(() -> useCase.create(mealRequest(UUID.randomUUID(), MealType.LUNCH)))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("project.not_found");
        verify(mealRepository, never()).save(any());
    }

    @Test
    void shouldFindByProjectId() {
        Project project = project();
        when(projectRepository.findById(project.getId())).thenReturn(project);
        when(mealRepository.findByProject(project)).thenReturn(List.of(meal(MealType.LUNCH)));

        assertThat(useCase.findByProjectId(project.getId())).hasSize(1);
    }
}
