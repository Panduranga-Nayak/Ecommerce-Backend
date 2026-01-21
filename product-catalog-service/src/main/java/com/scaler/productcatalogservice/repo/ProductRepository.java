package com.scaler.productcatalogservice.repo;

import com.scaler.productcatalogservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySku(String sku);

    @Query(value = """
            select distinct p from Product p
            left join p.category c
            left join p.specifications s
            where (:query is null or :query = '' or
                   lower(p.name) like lower(concat('%', :query, '%')) or
                   lower(p.description) like lower(concat('%', :query, '%')) or
                   lower(c.name) like lower(concat('%', :query, '%')) or
                   lower(s.specKey) like lower(concat('%', :query, '%')) or
                   lower(s.specValue) like lower(concat('%', :query, '%')))
            """,
            countQuery = """
                    select count(distinct p) from Product p
                    left join p.category c
                    left join p.specifications s
                    where (:query is null or :query = '' or
                           lower(p.name) like lower(concat('%', :query, '%')) or
                           lower(p.description) like lower(concat('%', :query, '%')) or
                           lower(c.name) like lower(concat('%', :query, '%')) or
                           lower(s.specKey) like lower(concat('%', :query, '%')) or
                           lower(s.specValue) like lower(concat('%', :query, '%')))
                    """)
    Page<Product> search(@Param("query") String query, Pageable pageable);
}