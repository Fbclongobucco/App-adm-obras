package com.longobuccodev.app_adm_obras.application.dto.restaurant;

import com.longobuccodev.app_adm_obras.application.dto.address.AddressRequestDTO;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code lunchDeliveryFee} e {@code dinnerDeliveryFee}: taxa por entrega do almoco e do jantar, opcionais
 * (null = entrega da refeicao sem taxa). {@code sodaPrice}: preco unitario do
 * refrigerante, 1 por entrega em todas as refeicoes, opcional (null = nao fornece).
 */
public record RestaurantRequestDTO(
        String name,
        String phone,
        AddressRequestDTO address,
        UUID projectId,
        BigDecimal lunchDeliveryFee,
        BigDecimal dinnerDeliveryFee,
        BigDecimal sodaPrice
) {
}
