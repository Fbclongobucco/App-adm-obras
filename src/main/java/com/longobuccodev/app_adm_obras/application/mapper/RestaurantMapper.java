package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Meal;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;

import java.util.List;

public final class RestaurantMapper {

    private RestaurantMapper() {
    }

    public static Restaurant toDomain(RestaurantRequestDTO dto, Project project) {
        return new Restaurant(null, dto.name(), dto.phone(), AddressMapper.toDomain(dto.address()), project,
                dto.lunchDeliveryFee(), dto.dinnerDeliveryFee(), dto.sodaPrice());
    }

    public static Restaurant toDomain(Restaurant existing, RestaurantRequestDTO dto, Project project) {
        return new Restaurant(existing.getId(), dto.name(), dto.phone(),
                AddressMapper.toDomain(AddressMapper.idOf(existing.getAddress()), dto.address()), project,
                dto.lunchDeliveryFee(), dto.dinnerDeliveryFee(), dto.sodaPrice());
    }

    public static RestaurantSummaryDTO toSummary(Restaurant restaurant) {
        if (restaurant == null) {
            return null;
        }
        return new RestaurantSummaryDTO(restaurant.getId(), restaurant.getName(), restaurant.getPhone(),
                restaurant.getLunchDeliveryFee(), restaurant.getDinnerDeliveryFee(), restaurant.getSodaPrice(),
                AddressMapper.toResponse(restaurant.getAddress()), restaurant.getCreatedAt(),
                restaurant.getUpdatedAt());
    }

    /** @param meals refeicoes servidas pelo restaurante */
    public static RestaurantResponseDTO toResponse(Restaurant restaurant, List<Meal> meals) {
        return new RestaurantResponseDTO(restaurant.getId(), restaurant.getName(), restaurant.getPhone(),
                restaurant.getLunchDeliveryFee(), restaurant.getDinnerDeliveryFee(), restaurant.getSodaPrice(),
                AddressMapper.toResponse(restaurant.getAddress()), ProjectMapper.toSummary(restaurant.getProject()),
                meals == null ? List.of() : meals.stream().map(MealMapper::toSummary).toList(),
                restaurant.getCreatedAt(), restaurant.getUpdatedAt());
    }
}
