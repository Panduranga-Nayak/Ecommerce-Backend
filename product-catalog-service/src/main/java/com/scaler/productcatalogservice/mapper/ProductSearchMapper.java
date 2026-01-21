package com.scaler.productcatalogservice.mapper;

import com.scaler.productcatalogservice.dto.ProductSearchResponseDto;
import com.scaler.productcatalogservice.model.Product;
import com.scaler.productcatalogservice.model.ProductImage;
import com.scaler.productcatalogservice.model.ProductSpecification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductSearchMapper {
    public static ProductSearchResponseDto toResponse(Product product) {
        if (product == null) {
            return null;
        }

        ProductSearchResponseDto dto = new ProductSearchResponseDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        dto.setPrice(product.getPrice());
        dto.setCurrency(product.getCurrency());
        dto.setImageUrls(toImageUrls(product.getImages()));
        dto.setSpecifications(toSpecifications(product.getSpecifications()));
        return dto;
    }

    private static List<String> toImageUrls(List<ProductImage> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .map(ProductImage::getUrl)
                .collect(Collectors.toList());
    }

    private static Map<String, String> toSpecifications(List<ProductSpecification> specifications) {
        if (specifications == null) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (ProductSpecification spec : specifications) {
            result.put(spec.getSpecKey(), spec.getSpecValue());
        }
        return result;
    }
}