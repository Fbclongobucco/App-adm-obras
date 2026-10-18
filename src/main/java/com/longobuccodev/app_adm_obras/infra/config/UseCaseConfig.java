package com.longobuccodev.app_adm_obras.infra.config;

import com.longobuccodev.app_adm_obras.application.security.RefreshTokenFactory;
import com.longobuccodev.app_adm_obras.application.usecase.AccommodationUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.AuditQueryUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.AuthUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.AddressUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.ClientUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.CostCenterUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.EmployeeUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.MealUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.ProjectUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.RestaurantUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.UserUseCase;
import com.longobuccodev.app_adm_obras.core.repository.AccommodationRepository;
import com.longobuccodev.app_adm_obras.core.repository.AddressRepository;
import com.longobuccodev.app_adm_obras.core.repository.ChangeAuditRepository;
import com.longobuccodev.app_adm_obras.core.repository.ClientRepository;
import com.longobuccodev.app_adm_obras.core.repository.CostCenterRepository;
import com.longobuccodev.app_adm_obras.core.repository.EmployeeRepository;
import com.longobuccodev.app_adm_obras.core.repository.LoginAuditRepository;
import com.longobuccodev.app_adm_obras.core.repository.MealRepository;
import com.longobuccodev.app_adm_obras.core.repository.ProjectRepository;
import com.longobuccodev.app_adm_obras.core.repository.RefreshTokenRepository;
import com.longobuccodev.app_adm_obras.core.repository.RestaurantRepository;
import com.longobuccodev.app_adm_obras.core.repository.UserRepository;
import com.longobuccodev.app_adm_obras.core.security.AccessTokenIssuer;
import com.longobuccodev.app_adm_obras.core.security.AuthenticationGateway;
import com.longobuccodev.app_adm_obras.core.security.PasswordHasher;
import com.longobuccodev.app_adm_obras.infra.security.JwtProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    public AddressUseCase addressUseCase(AddressRepository addressRepository) {
        return new AddressUseCase(addressRepository);
    }

    @Bean
    public CostCenterUseCase costCenterUseCase(CostCenterRepository costCenterRepository,
                                               EmployeeRepository employeeRepository,
                                               ProjectRepository projectRepository, Clock clock) {
        return new CostCenterUseCase(costCenterRepository, employeeRepository, projectRepository, clock);
    }

    @Bean
    public ClientUseCase clientUseCase(ClientRepository clientRepository, ProjectRepository projectRepository,
                                       Clock clock) {
        return new ClientUseCase(clientRepository, projectRepository, clock);
    }

    @Bean
    public EmployeeUseCase employeeUseCase(EmployeeRepository employeeRepository,
                                           CostCenterRepository costCenterRepository,
                                           ProjectRepository projectRepository,
                                           AccommodationRepository accommodationRepository, Clock clock) {
        return new EmployeeUseCase(employeeRepository, costCenterRepository, projectRepository,
                accommodationRepository, clock);
    }

    @Bean
    public ProjectUseCase projectUseCase(ProjectRepository projectRepository,
                                         CostCenterRepository costCenterRepository,
                                         ClientRepository clientRepository,
                                         AccommodationRepository accommodationRepository,
                                         EmployeeRepository employeeRepository,
                                         MealRepository mealRepository,
                                         Clock clock) {
        return new ProjectUseCase(projectRepository, costCenterRepository, clientRepository,
                accommodationRepository, employeeRepository, mealRepository, clock);
    }

    @Bean
    public MealUseCase mealUseCase(MealRepository mealRepository, ProjectRepository projectRepository,
                                   RestaurantRepository restaurantRepository, Clock clock) {
        return new MealUseCase(mealRepository, projectRepository, restaurantRepository, clock);
    }

    @Bean
    public RestaurantUseCase restaurantUseCase(RestaurantRepository restaurantRepository,
                                               ProjectRepository projectRepository,
                                               MealRepository mealRepository, Clock clock) {
        return new RestaurantUseCase(restaurantRepository, projectRepository, mealRepository, clock);
    }

    @Bean
    public AccommodationUseCase accommodationUseCase(AccommodationRepository accommodationRepository,
                                                     ProjectRepository projectRepository,
                                                     EmployeeRepository employeeRepository,
                                                     Clock clock) {
        return new AccommodationUseCase(accommodationRepository, projectRepository, employeeRepository, clock);
    }

    @Bean
    public UserUseCase userUseCase(UserRepository userRepository, PasswordHasher passwordHasher,
                                   RefreshTokenRepository refreshTokenRepository, Clock clock) {
        return new UserUseCase(userRepository, passwordHasher, refreshTokenRepository, clock);
    }

    @Bean
    public RefreshTokenFactory refreshTokenFactory() {
        return new RefreshTokenFactory();
    }

    @Bean
    public AuthUseCase authUseCase(AuthenticationGateway authenticationGateway, UserRepository userRepository,
                                   RefreshTokenRepository refreshTokenRepository,
                                   LoginAuditRepository loginAuditRepository, AccessTokenIssuer accessTokenIssuer,
                                   RefreshTokenFactory refreshTokenFactory, JwtProperties jwtProperties, Clock clock) {
        return new AuthUseCase(authenticationGateway, userRepository, refreshTokenRepository, loginAuditRepository,
                accessTokenIssuer, refreshTokenFactory, jwtProperties.refreshTokenTtl(),
                jwtProperties.refreshReuseGrace(), clock);
    }

    @Bean
    public AuditQueryUseCase auditQueryUseCase(ChangeAuditRepository changeAuditRepository,
                                               LoginAuditRepository loginAuditRepository) {
        return new AuditQueryUseCase(changeAuditRepository, loginAuditRepository);
    }
}
