package com.scaler.productcatalogservice.service;

import com.scaler.productcatalogservice.dto.CategoryRequestDto;
import com.scaler.productcatalogservice.dto.CreateProductRequestDto;
import com.scaler.productcatalogservice.dto.UpdateProductRequestDto;
import com.scaler.productcatalogservice.mapper.ProductMapper;
import com.scaler.productcatalogservice.exception.CategoryNotFoundException;
import com.scaler.productcatalogservice.exception.DuplicateSkuException;
import com.scaler.productcatalogservice.exception.CategoryAlreadyExistsException;
import com.scaler.productcatalogservice.exception.ProductNotFoundException;
import com.scaler.productcatalogservice.model.Category;
import com.scaler.productcatalogservice.model.Product;
import com.scaler.productcatalogservice.model.ProductImage;
import com.scaler.productcatalogservice.model.ProductSpecification;
import com.scaler.productcatalogservice.repo.CategoryRepository;
import com.scaler.productcatalogservice.repo.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Product createProduct(CreateProductRequestDto request) {
        productRepository.findBySku(request.getSku()).ifPresent(existing -> {
            throw new DuplicateSkuException("SKU already exists");
        });

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        Product product = new Product();
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCurrency(request.getCurrency());
        product.setStockQuantity(request.getStockQuantity());
        product.setStatus(request.getStatus());
        product.setCategory(category);

        List<ProductImage> images = ProductMapper.toImages(request.getImages(), product);
        List<ProductSpecification> specs = ProductMapper.toSpecifications(request.getSpecifications(), product);
        product.getImages().clear();
        product.getImages().addAll(images);
        product.getSpecifications().clear();
        product.getSpecifications().addAll(specs);

        Product saved = productRepository.save(product);
        initializeProduct(saved);
        return saved;
    }

    @Override
    @Transactional
    public Product updateProduct(Long productId, UpdateProductRequestDto request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getCurrency() != null) {
            product.setCurrency(request.getCurrency());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
            product.setCategory(category);
        }

        if (request.getImages() != null) {
            product.getImages().clear();
            product.getImages().addAll(ProductMapper.toImages(request.getImages(), product));
        }

        if (request.getSpecifications() != null) {
            product.getSpecifications().clear();
            product.getSpecifications().addAll(ProductMapper.toSpecifications(request.getSpecifications(), product));
        }

        Product saved = productRepository.save(product);
        initializeProduct(saved);
        return saved;
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        initializeProduct(product);
        return product;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> listProducts(ProductFilter filter, Pageable pageable) {
        Specification<Product> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            }
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> products = productRepository.findAll(specification, pageable);
        products.forEach(this::initializeProduct);
        return products;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String query, Pageable pageable) {
        Page<Product> products = productRepository.search(query, pageable);
        products.forEach(this::initializeProduct);
        return products;
    }

    private void initializeProduct(Product product) {
        if (product.getCategory() != null) {
            product.getCategory().getName();
        }
        product.getImages().size();
        product.getSpecifications().size();
    }

    @Override
    @Transactional
    public Category createCategory(CategoryRequestDto request) {
        categoryRepository.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
            throw new CategoryAlreadyExistsException("Category already exists");
        });

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
    }
}