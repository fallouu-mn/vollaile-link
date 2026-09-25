package com.vollailelink.backend.repository;

import com.vollailelink.backend.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findByIsActiveTrue();
    List<ProductCategory> findByParentId(Long parentId);
}
