package com.markettrust.product.controller;

import com.markettrust.product.dto.*;
import com.markettrust.product.service.ProductService;
import com.markettrust.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog", description = "Product listing, search, nearby, and image endpoints")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create a product (Draft status)")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest req) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(sellerId, req));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Upload product images (max 8)")
    public ResponseEntity<List<ProductImageDto>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(productService.uploadProductImages(sellerId, id, files));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Publish draft product to ACTIVE")
    public ResponseEntity<ProductDto> publishProduct(@PathVariable Long id) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(productService.publishProduct(sellerId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update product details")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest req) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(productService.updateProduct(sellerId, id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Soft delete product")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        productService.deleteProduct(sellerId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/pause")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Pause an active product")
    public ResponseEntity<ProductDto> pauseProduct(@PathVariable Long id) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(productService.pauseProduct(sellerId, id));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Reactivate a paused product")
    public ResponseEntity<ProductDto> reactivateProduct(@PathVariable Long id) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(productService.reactivateProduct(sellerId, id));
    }

    @PutMapping("/{id}/sold")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Mark product as sold")
    public ResponseEntity<ProductDto> markAsSold(@PathVariable Long id) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(productService.markAsSold(sellerId, id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed product view by ID (public)")
    public ResponseEntity<ProductDto> getProduct(
            @PathVariable Long id,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress) {
        Long viewerId = SecurityUtils.getCurrentUserIdSafely();
        return ResponseEntity.ok(productService.getProductById(id, viewerId, ipAddress != null ? ipAddress : "127.0.0.1"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products with filters (public)")
    public ResponseEntity<Page<ProductSummaryDto>> searchProducts(SearchRequest req) {
        return ResponseEntity.ok(productService.searchProducts(req));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get products near a location using Haversine calculation (public)")
    public ResponseEntity<List<ProductSummaryDto>> getNearbyProducts(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10.0") double radius,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(productService.getNearbyProducts(lat, lng, radius, page));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recently added products (public)")
    public ResponseEntity<Page<ProductSummaryDto>> getRecentProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(productService.getRecentProducts(PageRequest.of(page, size)));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending products by view count (public)")
    public ResponseEntity<Page<ProductSummaryDto>> getTrendingProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(productService.getTrendingProducts(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "Get similar products in the same category (public)")
    public ResponseEntity<List<ProductSummaryDto>> getSimilarProducts(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getSimilarProducts(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller's own products")
    public ResponseEntity<Page<ProductSummaryDto>> getMyProducts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(productService.getSellerProducts(sellerId, status, PageRequest.of(page, size)));
    }

    @PutMapping("/admin/{id}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin hide product")
    public ResponseEntity<Void> adminHideProduct(@PathVariable Long id, @RequestParam String reason) {
        Long adminId = SecurityUtils.getCurrentUserId();
        productService.adminHideProduct(adminId, id, reason);
        return ResponseEntity.noContent().build();
    }
}
