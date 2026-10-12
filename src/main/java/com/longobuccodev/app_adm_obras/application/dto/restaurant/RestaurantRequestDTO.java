package com.longobuccodev.app_adm_obras.application.dto.restaurant;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code deliveryFee}: taxa por entrega, opcional (null = sem taxa). {@code sodaPrice}: preco unitario do
 * refrigerante, 1 por entrega em todas as refeicoes, opcional (null = nao fornece).
 */
public record RestaurantRequestDTO(
        String name,
        String phone,
        AddressRequestDTO address,
        UUID projectId,
        BigDecimal deliveryFee,
        BigDecimal sodaPrice
) {
}
