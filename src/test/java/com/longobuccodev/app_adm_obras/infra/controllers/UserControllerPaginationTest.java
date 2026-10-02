package com.longobuccodev.app_adm_obras.infra.controllers;

import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.user.UserResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.UserUseCase;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.repository.Page;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(com.longobuccodev.app_adm_obras.infra.security.SecurityConfig.class)
class UserControllerPaginationTest {


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserUseCase userUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("sem parametros a paginacao deve usar a primeira pagina com 20 itens")
    void shouldUseFirstPageWithDefaultSize() throws Exception {
        when(userUseCase.findAll(any(PageRequest.class))).thenReturn(pageOf(0, 20, 3));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.first").value(true));

        assertThat(capturedRequest().page()).isZero();
        assertThat(capturedRequest().size()).isEqualTo(PageRequest.DEFAULT_SIZE);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("os parametros page e size devem ser repassados ao caso de uso")
    void shouldForwardPageAndSizeQueryParams() throws Exception {
        when(userUseCase.findAll(any(PageRequest.class))).thenReturn(pageOf(2, 5, 3));

        mockMvc.perform(get("/api/users").param("page", "2").param("size", "5")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.content").isArray());

        assertThat(capturedRequest().page()).isEqualTo(2);
        assertThat(capturedRequest().size()).isEqualTo(5);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("pagina fora do limite deve ser rejeitada com 400")
    void shouldRejectInvalidPage() throws Exception {
        mockMvc.perform(get("/api/users").param("page", "-1")
                        )
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/users").param("size", "500")
                        )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("pagina nao numerica deve ser rejeitada com 400")
    void shouldRejectNonNumericPage() throws Exception {
        mockMvc.perform(get("/api/users").param("page", "abc")
                        )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("a listagem paginada deve continuar acessivel ao OPERADOR")
    void operadorShouldStillReadPaginatedList() throws Exception {
        when(userUseCase.findAll(any(PageRequest.class))).thenReturn(pageOf(0, 20, 3));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    private PageRequest capturedRequest() {
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(userUseCase).findAll(captor.capture());
        return captor.getValue();
    }

    private PageResponseDTO<UserResponseDTO> pageOf(int page, int size, int total) {
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResponseDTO<>(
                List.of(new UserResponseDTO(UUID.randomUUID(), "Maria Souza", "maria@email.com", true,
                        Set.of(Role.ADMIN))),
                page, size, total, totalPages, page == 0, page >= totalPages - 1);
    }
}
