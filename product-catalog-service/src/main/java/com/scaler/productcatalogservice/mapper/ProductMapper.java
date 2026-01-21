package com.scaler.productcatalogservice.mapper;

import com.scaler.productcatalogservice.dto.*;
import com.scaler.productcatalogservice.model.Product;
import com.scaler.productcatalogservice.model.ProductImage;
import com.scaler.productcatalogservice.model.ProductSpecification;

import java.util.List;
import java.util.stream.Collectors;

public class ProductMapper {
    public static ProductResponseDto toResponse(Product product) {
        if (product == null) {
            return null;
        }

        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(product.getId());
        dto.setSku(product.getSku());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCurrency(product.getCurrency());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setStatus(product.getStatus());
        dto.setCategory(CategoryMapper.toResponse(product.getCategory()));
        dto.setImages(product.getImages().stream().map(ProductMapper::toImageDto).collect(Collectors.toList()));
        dto.setSpecifications(product.getSpecifications().stream().map(ProductMapper::toSpecDto).collect(Collectors.toList()));
        return dto;
    }

    public static ProductImageDto toImageDto(ProductImage image) {
        ProductImageDto dto = new ProductImageDto();
        dto.setUrl(image.getUrl());
        dto.setSortOrder(image.getSortOrder());
        return dto;
    }

    public static ProductSpecificationDto toSpecDto(ProductSpecification specification) {
        ProductSpecificationDto dto = new ProductSpecificationDto();
        dto.setKey(specification.getSpecKey());
        dto.setValue(specification.getSpecValue());
        return dto;
    }

    public static List<ProductImage> toImages(List<ProductImageDto> images, Product product) {
        if (images == null) {
            return List.of();
        }

        return images.stream().map(dto -> {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setUrl(dto.getUrl());
            image.setSortOrder(dto.getSortOrder());
            return image;
        }).collect(Collectors.toList());
    }

    public static List<ProductSpecification> toSpecifications(List<ProductSpecificationDto> specifications, Product product) {
        if (specifications == null) {
            return List.of();
        }

        return specifications.stream().map(dto -> {
            ProductSpecification spec = new ProductSpecification();
            spec.setProduct(product);
            spec.setSpecKey(dto.getKey());
            spec.setSpecValue(dto.getValue());
            return spec;
        }).collect(Collectors.toList());
    }
}