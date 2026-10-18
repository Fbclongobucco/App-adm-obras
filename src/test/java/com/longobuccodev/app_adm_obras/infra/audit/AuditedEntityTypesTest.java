package com.longobuccodev.app_adm_obras.infra.audit;

import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry;
import com.longobuccodev.app_adm_obras.core.domain.ChangeAuditEntry.EntityType;
import com.longobuccodev.app_adm_obras.infra.entities.AccommodationEntity;
import com.longobuccodev.app_adm_obras.infra.entities.AccommodationPeriodEntity;
import com.longobuccodev.app_adm_obras.infra.entities.AddressEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ClientEntity;
import com.longobuccodev.app_adm_obras.infra.entities.CostCenterEntity;
import com.longobuccodev.app_adm_obras.infra.entities.EmployeeEntity;
import com.longobuccodev.app_adm_obras.infra.entities.MealAdjustmentEntity;
import com.longobuccodev.app_adm_obras.infra.entities.MealEntity;
import com.longobuccodev.app_adm_obras.infra.entities.ProjectEntity;
import com.longobuccodev.app_adm_obras.infra.entities.RefreshTokenEntity;
import com.longobuccodev.app_adm_obras.infra.entities.RestaurantEntity;
import com.longobuccodev.app_adm_obras.infra.entities.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditedEntityTypesTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Cada entidade de negocio deve ter o seu tipo de registro, e os 10 tipos devem estar cobertos")
    void shouldMapBusinessEntities() throws Exception {
        Map<Class<?>, EntityType> expected = Map.of(
                ProjectEntity.class, EntityType.PROJECT,
                EmployeeEntity.class, EntityType.EMPLOYEE,
                CostCenterEntity.class, EntityType.COST_CENTER,
                ClientEntity.class, EntityType.CLIENT,
                AccommodationEntity.class, EntityType.ACCOMMODATION,
                AccommodationPeriodEntity.class, EntityType.ACCOMMODATION_PERIOD,
                MealEntity.class, EntityType.MEAL,
                MealAdjustmentEntity.class, EntityType.MEAL_ADJUSTMENT,
                RestaurantEntity.class, EntityType.RESTAURANT,
                UserEntity.class, EntityType.USER);
        for (var entry : expected.entrySet()) {
            assertThat(AuditedEntityTypes.of(instance(entry.getKey()))).as(entry.getKey().getSimpleName())
                    .contains(entry.getValue());
        }
        assertThat(expected.values()).containsExactlyInAnyOrder(EntityType.values());
    }

    @Test
    @DisplayName("Endereco e refresh token nao devem ser auditados")
    void shouldIgnoreOtherEntities() throws Exception {
        for (Class<?> type : List.of(AddressEntity.class, RefreshTokenEntity.class)) {
            assertThat(AuditedEntityTypes.of(instance(type))).as(type.getSimpleName()).isEmpty();
        }
    }

    @Test
    @DisplayName("Sem autenticacao, o autor deve ser SYSTEM sem id")
    void shouldResolveSystemWithoutAuthentication() throws Exception {
        AuditActorResolver.Actor actor = AuditActorResolver.resolve(null);

        assertThat(actor.id()).isNull();
        assertThat(actor.name()).isEqualTo(ChangeAuditEntry.SYSTEM);
        assertThat(actor.email()).isNull();
    }

    @Test
    @DisplayName("Autenticacao anonima tambem deve resultar em SYSTEM")
    void shouldResolveSystemForAnonymous() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(AuditActorResolver.resolve(null)).isEqualTo(AuditActorResolver.SYSTEM);
    }

    @Test
    @DisplayName("Autenticacao sem id de usuario deve registrar o nome que ela traz, sem consultar o banco")
    void shouldKeepNameWhenNotAUserId() throws Exception {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("rotina", null, "ROLE_ADMIN");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AuditActorResolver.Actor actor = AuditActorResolver.resolve(null);

        assertThat(actor.id()).isNull();
        assertThat(actor.name()).isEqualTo("rotina");
    }

    private static Object instance(Class<?> type) throws Exception {
        Constructor<?> constructor = Arrays.stream(type.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == 0).findFirst().orElseThrow();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
