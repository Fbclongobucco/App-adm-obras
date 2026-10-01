package com.longobuccodev.app_adm_obras.application.usecase;

import com.longobuccodev.app_adm_obras.application.dto.AddressResponseDTO;
import com.longobuccodev.app_adm_obras.application.exception.ResourceNotFoundException;
import com.longobuccodev.app_adm_obras.core.domain.Address;
import com.longobuccodev.app_adm_obras.core.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.address;
import static com.longobuccodev.app_adm_obras.application.ApplicationFixtures.addressRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressUseCaseTest {

    private AddressRepository addressRepository;
    private AddressUseCase useCase;

    @BeforeEach
    void setUp() {
        addressRepository = mock(AddressRepository.class);
        useCase = new AddressUseCase(addressRepository);
    }

    @Test
    void shouldCreateAddress() {
        when(addressRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AddressResponseDTO response = useCase.create(addressRequest());

        assertThat(response.state()).isEqualTo("SP");
        assertThat(response.zipCode()).isEqualTo("01310100");
    }

    @Test
    void shouldUpdateKeepingId() {
        Address existing = address();
        when(addressRepository.findById(existing.getId())).thenReturn(existing);

        AddressResponseDTO response = useCase.update(existing.getId(), addressRequest());

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(addressRepository).update(any(Address.class));
    }

    @Test
    void shouldRejectFindByUnknownId() {
        assertThatThrownBy(() -> useCase.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo("address.not_found");
    }
}
