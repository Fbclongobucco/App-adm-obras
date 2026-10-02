package com.longobuccodev.app_adm_obras.infra.config;

import com.longobuccodev.app_adm_obras.application.usecase.AccommodationUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.AddressUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.ClientUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.CostCenterUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.EmployeeUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.MealUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.ProjectUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.UserUseCase;
import com.longobuccodev.app_adm_obras.core.repository.AccommodationRepository;
import com.longobuccodev.app_adm_obras.core.repository.AddressRepository;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public AddressUseCase addressUseCase(AddressRepository addressRepository) {
        return new AddressUseCase(addressRepository);
    }

    @Bean
    public CostCenterUseCase costCenterUseCase(CostCenterRepository costCenterRepository) {
        return new CostCenterUseCase(costCenterRepository);
    }

    @Bean
    public ClientUseCase clientUseCase(ClientRepository clientRepository) {
        return new ClientUseCase(clientRepository);
    }

    @Bean
    public EmployeeUseCase employeeUseCase(EmployeeRepository employeeRepository,
                                           CostCenterRepository costCenterRepository) {
        return new EmployeeUseCase(employeeRepository, costCenterRepository);
    }

    @Bean
    public ProjectUseCase projectUseCase(ProjectRepository projectRepository,
                                         CostCenterRepository costCenterRepository,
                                         ClientRepository clientRepository,
                                         AccommodationRepository accommodationRepository,
                                         EmployeeRepository employeeRepository,
                                         MealRepository mealRepository) {
        return new ProjectUseCase(projectRepository, costCenterRepository, clientRepository,
                accommodationRepository, employeeRepository, mealRepository);
    }

    @Bean
    public MealUseCase mealUseCase(MealRepository mealRepository, ProjectRepository projectRepository) {
        return new MealUseCase(mealRepository, projectRepository);
    }

    @Bean
    public AccommodationUseCase accommodationUseCase(AccommodationRepository accommodationRepository,
                                                     ProjectRepository projectRepository,
                                                     EmployeeRepository employeeRepository) {
        return new AccommodationUseCase(accommodationRepository, projectRepository, employeeRepository);
    }

    @Bean
    public UserUseCase userUseCase(UserRepository userRepository) {
        return new UserUseCase(userRepository);
    }
}
