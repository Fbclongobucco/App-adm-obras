package com.longobuccodev.app_adm_obras.infra.security;

import com.longobuccodev.app_adm_obras.application.usecase.AuthUseCase;
import com.longobuccodev.app_adm_obras.core.exception.AuthenticationFailedException;
import com.longobuccodev.app_adm_obras.infra.config.ClockConfig;
import com.longobuccodev.app_adm_obras.infra.controllers.AuthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, ClockConfig.class})
class RefreshOriginFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthUseCase authUseCase;

    @Test
    @DisplayName("sem Origin (cliente que nao e navegador) a requisicao passa pelo filtro e chega ao controller")
    void missingOriginShouldPass() throws Exception {
        // Refresh sem Origin e coberto ponta a ponta em AuthControllerIntegrationTest
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));

        verify(authUseCase).logout(any());
    }

    @Test
    @DisplayName("refresh e logout com Origin desconhecida devem responder 403")
    void unknownOriginShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").header("Origin", "https://site-malicioso.example.com"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/logout").header("Origin", "https://site-malicioso.example.com"))
                .andExpect(status().isForbidden());

        verify(authUseCase, never()).refresh(any());
        verify(authUseCase, never()).logout(any());
    }

    @Test
    @DisplayName("logout com a origem do frontend deve chegar ao controller")
    void allowedOriginShouldPass() throws Exception {
        mockMvc.perform(post("/api/auth/logout").header("Origin", "http://localhost:3000"))
                .andExpect(status().isNoContent());

        verify(authUseCase).logout(any());
    }

    @Test
    @DisplayName("login nao exige Origin")
    void loginIsNotFiltered() throws Exception {
        when(authUseCase.login(any(), any())).thenThrow(AuthenticationFailedException.invalidCredentials());

        mockMvc.perform(post("/api/auth/login")).andExpect(status().isUnauthorized());
    }
}
