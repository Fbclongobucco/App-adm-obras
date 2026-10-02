package com.longobuccodev.app_adm_obras.infra.security;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.address.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.common.PageResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.user.UserRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.user.UserResponseDTO;
import com.longobuccodev.app_adm_obras.application.usecase.AddressUseCase;
import com.longobuccodev.app_adm_obras.application.usecase.UserUseCase;
import com.longobuccodev.app_adm_obras.core.domain.User.Role;
import com.longobuccodev.app_adm_obras.core.repository.PageRequest;
import com.longobuccodev.app_adm_obras.infra.controllers.AddressController;
import com.longobuccodev.app_adm_obras.infra.controllers.UserController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, AddressController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {


    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserUseCase userUseCase;

    @MockitoBean
    private AddressUseCase addressUseCase;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN deve conseguir ler e criar usuarios")
    void adminShouldReadAndCreateUsers() throws Exception {
        when(userUseCase.findAll(any(PageRequest.class))).thenReturn(paginatedUsers());
        when(userUseCase.create(any(UserRequestDTO.class))).thenReturn(userResponse());

        mockMvc.perform(get("/api/users")).andExpect(status().isOk());
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("OPERADOR deve conseguir ler mas nao criar usuarios")
    void operadorShouldReadButNotCreateUsers() throws Exception {
        when(userUseCase.findAll(any(PageRequest.class))).thenReturn(paginatedUsers());

        mockMvc.perform(get("/api/users")).andExpect(status().isOk());
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("OPERADOR nao deve conseguir escrever em nenhum outro recurso")
    void operadorShouldNotWriteOnOtherResources() throws Exception {
        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN deve conseguir escrever nos demais recursos")
    void adminShouldWriteOnOtherResources() throws Exception {
        when(addressUseCase.create(any(AddressRequestDTO.class))).thenReturn(addressResponse());

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("credenciais ausentes devem ser rejeitadas")
    void missingCredentialsShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users").header("Authorization", "Basic credenciais-invalidas"))
                .andExpect(status().isUnauthorized());
    }

    private PageResponseDTO<UserResponseDTO> paginatedUsers() {
        return new PageResponseDTO<>(List.of(userResponse()), 0, 20, 1, 1, true, true);
    }

    private UserResponseDTO userResponse() {
        return new UserResponseDTO(UUID.randomUUID(), "Maria Souza", "maria@email.com", true, Set.of(Role.ADMIN));
    }

    private AddressResponseDTO addressResponse() {
        return new AddressResponseDTO(UUID.randomUUID(), "Rua das Flores", "120", "Sao Paulo", "SP", "Brasil",
                "Centro", "01310100");
    }
}
