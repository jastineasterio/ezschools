package com.edumanage.finance.controller;

import com.edumanage.common.dto.ApiResponse;
import com.edumanage.common.dto.PageResponse;
import com.edumanage.finance.dto.CreateFinancialCategoryRequest;
import com.edumanage.finance.dto.FinancialCategoryDto;
import com.edumanage.finance.service.FinancialCategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/school/finance/categories")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class FinancialCategoryController {

    private final FinancialCategoryService financialCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FinancialCategoryDto>>> list(Pageable pageable) {
        var categories = financialCategoryService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Financial categories fetched", categories, null, LocalDateTime.now(), "/api/school/finance/categories"));
    }

    @GetMapping("/type/{categoryType}")
    public ResponseEntity<ApiResponse<List<FinancialCategoryDto>>> listByType(@PathVariable String categoryType) {
        var categories = financialCategoryService.listByType(categoryType);
        return ResponseEntity.ok(new ApiResponse<>(true, "Categories fetched", categories, null, LocalDateTime.now(), "/api/school/finance/categories/type/" + categoryType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FinancialCategoryDto>> getById(@PathVariable Long id) {
        var category = financialCategoryService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category fetched", category, null, LocalDateTime.now(), "/api/school/finance/categories/" + id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FinancialCategoryDto>> create(@Valid @RequestBody CreateFinancialCategoryRequest request) {
        var category = financialCategoryService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category created", category, null, LocalDateTime.now(), "/api/school/finance/categories"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FinancialCategoryDto>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody CreateFinancialCategoryRequest request) {
        var category = financialCategoryService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category updated", category, null, LocalDateTime.now(), "/api/school/finance/categories/" + id));
    }
}
