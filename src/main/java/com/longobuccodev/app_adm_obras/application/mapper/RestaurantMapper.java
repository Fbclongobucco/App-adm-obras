package com.longobuccodev.app_adm_obras.application.mapper;

import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantRequestDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantResponseDTO;
import com.longobuccodev.app_adm_obras.application.dto.restaurant.RestaurantSummaryDTO;
import com.longobuccodev.app_adm_obras.core.domain.Project;
import com.longobuccodev.app_adm_obras.core.domain.Restaurant;

public final class RestaurantMapper {

    private RestaurantMapper() {
    }

    public static Restaurant toDomain(RestaurantRequestDTO dto, Project project) {
        return new Restaurant(null, dto.name(), dto.phone(), AddressMapper.toDomain(dto.address()), project);
    }

    public static Restaurant toDomain(Restaurant existing, RestaurantRequestDTO dto, Project project) {
        return new Restaurant(existing.getId(), dto.name(), dto.phone(),
                AddressMapper.toDomain(AddressMapper.idOf(existing.getAddress()), dto.address()), project);
    }

    public static RestaurantSummaryDTO toSummary(Restaurant restaurant) {
        if (restaurant == null) {
            return null;
        }
        return new RestaurantSummaryDTO(restaurant.getId(), restaurant.getName(), restaurant.getPhone(),
                AddressMapper.toResponse(restaurant.getAddress()),
                restaurant.getCreatedAt(), restaurant.getUpdatedAt());
    }

    public static RestaurantResponseDTO toResponse(Restaurant restaurant) {
        return new RestaurantResponseDTO(restaurant.getId(), restaurant.getName(), restaurant.getPhone(),
                AddressMapper.toResponse(restaurant.getAddress()), ProjectMapper.toSummary(restaurant.getProject()),
                restaurant.getCreatedAt(), restaurant.getUpdatedAt());
    }
}
