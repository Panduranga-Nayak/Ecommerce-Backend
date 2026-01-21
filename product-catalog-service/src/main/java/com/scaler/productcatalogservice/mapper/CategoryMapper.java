package com.scaler.productcatalogservice.mapper;

import com.scaler.productcatalogservice.dto.CategoryResponseDto;
import com.scaler.productcatalogservice.model.Category;

public class CategoryMapper {
    public static CategoryResponseDto toResponse(Category category) {
        if (category == null) {
            return null;
        }

        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        return dto;
    }
}