package com.markettrust.product.service;

import com.markettrust.exception.BadRequestException;
import com.markettrust.exception.ForbiddenException;
import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.product.dto.*;
import com.markettrust.product.entity.*;
import com.markettrust.product.repository.ProductImageRepository;
import com.markettrust.product.repository.ProductRepository;
import com.markettrust.product.repository.ProductViewRepository;
import com.markettrust.seller.entity.SellerProfile;
import com.markettrust.seller.repository.SellerProfileRepository;
import com.markettrust.user.entity.User;
import com.markettrust.user.repository.UserRepository;
import com.markettrust.integration.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductViewRepository productViewRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Value("${app.platform.max-product-images:8}")
    private int maxImages;

    @Value("${app.platform.max-listing-days:90}")
    private int maxListingDays;

    // ─────────────────────────────────────────────────────────────────────────
    // SELLER ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ProductDto createProduct(Long sellerId, CreateProductRequest req) {
        SellerProfile seller = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new BadRequestException("Seller profile not found. Please complete your profile."));

        Product product = new Product();
        product.setSellerId(sellerId);
        mapRequestToProduct(req, product);
        product.setStatus(ProductStatus.DRAFT);
        product.setExpiresAt(LocalDateTime.now().plusDays(maxListingDays));

        Product saved = productRepository.save(product);
        log.info("Product created [id={}, sellerId={}]", saved.getId(), sellerId);
        return toProductDto(saved, null);
    }

    @Transactional
    public List<ProductImageDto> uploadProductImages(Long sellerId, Long productId, MultipartFile[] files) {
        Product product = getProductForSeller(sellerId, productId);

        long existingCount = productImageRepository.countByProductId(productId);
        if (existingCount + files.length > maxImages) {
            throw new BadRequestException("Cannot exceed " + maxImages + " images per product. Currently have " + existingCount + ".");
        }

        List<ProductImageDto> dtos = new ArrayList<>();
        boolean firstImage = existingCount == 0;

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            cloudinaryService.validateImageFile(file);
            Map<String, String> result = cloudinaryService.uploadImage(file, "products/" + productId);

            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(result.get("url"));
            image.setPublicId(result.get("publicId"));
            image.setIsPrimary(firstImage && i == 0);
            image.setSortOrder((int) existingCount + i);
            productImageRepository.save(image);

            dtos.add(toImageDto(image));
        }
        return dtos;
    }

    @Transactional
    public ProductDto publishProduct(Long sellerId, Long productId) {
        Product product = getProductForSeller(sellerId, productId);
        if (!ProductStatus.DRAFT.equals(product.getStatus()) && !ProductStatus.HIDDEN.equals(product.getStatus())) {
            throw new BadRequestException("Only DRAFT or HIDDEN products can be published.");
        }
        if (productImageRepository.countByProductId(productId) == 0) {
            throw new BadRequestException("Please upload at least one image before publishing.");
        }
        product.setStatus(ProductStatus.ACTIVE);
        return toProductDto(productRepository.save(product), null);
    }

    @Transactional
    public ProductDto updateProduct(Long sellerId, Long productId, UpdateProductRequest req) {
        Product product = getProductForSeller(sellerId, productId);
        if (req.getTitle() != null) product.setTitle(req.getTitle());
        if (req.getDescription() != null) product.setDescription(req.getDescription());
        if (req.getPriceInCredits() != null) product.setPriceInCredits(req.getPriceInCredits());
        if (req.getCategoryId() != null) product.setCategoryId(req.getCategoryId());
        if (req.getCondition() != null) product.setCondition(req.getCondition());
        if (req.getBrand() != null) product.setBrand(req.getBrand());
        if (req.getCity() != null) product.setCity(req.getCity());
        if (req.getState() != null) product.setState(req.getState());
        if (req.getLatitude() != null) product.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) product.setLongitude(req.getLongitude());
        if (req.getLocationType() != null) product.setLocationType(req.getLocationType());
        if (req.getQuantity() != null) product.setQuantity(req.getQuantity());
        return toProductDto(productRepository.save(product), null);
    }

    @Transactional
    public void deleteProduct(Long sellerId, Long productId) {
        Product product = getProductForSeller(sellerId, productId);
        product.setStatus(ProductStatus.HIDDEN);
        productRepository.save(product);
        log.info("Product soft-deleted [id={}, sellerId={}]", productId, sellerId);
    }

    @Transactional
    public ProductDto pauseProduct(Long sellerId, Long productId) {
        Product product = getProductForSeller(sellerId, productId);
        if (!ProductStatus.ACTIVE.equals(product.getStatus())) {
            throw new BadRequestException("Only ACTIVE products can be paused.");
        }
        product.setStatus(ProductStatus.HIDDEN);
        return toProductDto(productRepository.save(product), null);
    }

    @Transactional
    public ProductDto reactivateProduct(Long sellerId, Long productId) {
        Product product = getProductForSeller(sellerId, productId);
        if (!ProductStatus.HIDDEN.equals(product.getStatus())) {
            throw new BadRequestException("Only HIDDEN products can be reactivated.");
        }
        product.setStatus(ProductStatus.ACTIVE);
        return toProductDto(productRepository.save(product), null);
    }

    @Transactional
    public ProductDto markAsSold(Long sellerId, Long productId) {
        Product product = getProductForSeller(sellerId, productId);
        product.setStatus(ProductStatus.SOLD);
        return toProductDto(productRepository.save(product), null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC READ
    // ─────────────────────────────────────────────────────────────────────────

    public ProductDto getProductById(Long productId, Long viewerId, String ipAddress) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        recordProductView(productId, viewerId, ipAddress);
        return toProductDto(product, null);
    }

    public Page<ProductSummaryDto> searchProducts(SearchRequest req) {
        Sort sort = buildSort(req.getSortBy());
        Pageable pageable = PageRequest.of(
                req.getPage() != null ? req.getPage() : 0,
                req.getSize() != null ? req.getSize() : 20,
                sort);

        ProductCondition condition = null;
        if (req.getCondition() != null) {
            try { condition = ProductCondition.valueOf(req.getCondition().name()); } catch (Exception ignored) {}
        }

        return productRepository.searchProducts(
                req.getKeyword(),
                req.getCategoryId(),
                req.getMinPrice(),
                req.getMaxPrice(),
                condition,
                pageable
        ).map(p -> toProductSummaryDto(p, null));
    }

    public List<ProductSummaryDto> getNearbyProducts(double lat, double lng, double radiusKm, int page) {
        List<Object[]> results = productRepository.findNearbyProducts(lat, lng, radiusKm, 20);
        return results.stream().map(row -> {
            // row[0..n] = product columns, last column = distance
            Long id = ((Number) row[0]).longValue();
            return productRepository.findById(id).map(p -> {
                double distance = row[row.length - 1] != null ? ((Number) row[row.length - 1]).doubleValue() : 0.0;
                return toProductSummaryDto(p, distance);
            }).orElse(null);
        }).filter(dto -> dto != null).collect(Collectors.toList());
    }

    public Page<ProductSummaryDto> getRecentProducts(Pageable pageable) {
        return productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE, pageable)
                .map(p -> toProductSummaryDto(p, null));
    }

    public Page<ProductSummaryDto> getTrendingProducts(Pageable pageable) {
        return productRepository.findByStatusOrderByViewsDesc(ProductStatus.ACTIVE, pageable)
                .map(p -> toProductSummaryDto(p, null));
    }

    public List<ProductSummaryDto> getSimilarProducts(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        Pageable pageable = PageRequest.of(0, 8);
        return productRepository.findByCategoryIdAndStatusAndIdNot(
                product.getCategoryId(), ProductStatus.ACTIVE, productId, pageable)
                .stream().map(p -> toProductSummaryDto(p, null)).collect(Collectors.toList());
    }

    public Page<ProductSummaryDto> getSellerProducts(Long sellerId, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            try {
                ProductStatus ps = ProductStatus.valueOf(status);
                return productRepository.findBySellerIdAndStatus(sellerId, ps, pageable)
                        .map(p -> toProductSummaryDto(p, null));
            } catch (Exception ignored) {}
        }
        return productRepository.findBySellerId(sellerId, pageable)
                .map(p -> toProductSummaryDto(p, null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void adminHideProduct(Long adminId, Long productId, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        product.setStatus(ProductStatus.HIDDEN);
        productRepository.save(product);
        log.info("Admin [{}] hid product [{}]: {}", adminId, productId, reason);
    }

    @Transactional
    public void adminRejectProduct(Long adminId, Long productId, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        product.setStatus(ProductStatus.REJECTED);
        productRepository.save(product);
        log.info("Admin [{}] rejected product [{}]: {}", adminId, productId, reason);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    public void recordProductView(Long productId, Long viewerId, String ipAddress) {
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            boolean alreadyViewed;
            if (viewerId != null) {
                alreadyViewed = productViewRepository.existsByProductIdAndViewerIdAndViewedAtAfter(productId, viewerId, oneHourAgo);
            } else {
                alreadyViewed = productViewRepository.existsByProductIdAndIpAddressAndViewedAtAfter(productId, ipAddress, oneHourAgo);
            }
            if (!alreadyViewed) {
                ProductView view = new ProductView();
                view.setProductId(productId);
                view.setViewerId(viewerId);
                view.setIpAddress(ipAddress != null ? ipAddress : "unknown");
                productViewRepository.save(view);
                productRepository.incrementViews(productId);
            }
        } catch (Exception e) {
            log.warn("Failed to record product view: {}", e.getMessage());
        }
    }

    private Product getProductForSeller(Long sellerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        if (!product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("You don't have permission to modify this product.");
        }
        return product;
    }

    private void mapRequestToProduct(CreateProductRequest req, Product product) {
        product.setTitle(req.getTitle());
        product.setDescription(req.getDescription());
        product.setPriceInCredits(req.getPriceInCredits());
        product.setCategoryId(req.getCategoryId());
        product.setCondition(req.getCondition());
        product.setBrand(req.getBrand());
        product.setCity(req.getCity());
        product.setState(req.getState());
        product.setLatitude(req.getLatitude());
        product.setLongitude(req.getLongitude());
        product.setLocationType(req.getLocationType() != null ? req.getLocationType() : LocationType.APPROXIMATE);
        product.setQuantity(req.getQuantity() != null ? req.getQuantity() : 1);
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sortBy) {
            case "PRICE_LOW_HIGH"  -> Sort.by(Sort.Direction.ASC, "priceInCredits");
            case "PRICE_HIGH_LOW"  -> Sort.by(Sort.Direction.DESC, "priceInCredits");
            case "MOST_VIEWED"     -> Sort.by(Sort.Direction.DESC, "views");
            case "HIGHEST_RATED"   -> Sort.by(Sort.Direction.DESC, "createdAt"); // rating would require join
            default                -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    public ProductDto toProductDto(Product product, Double distance) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setTitle(product.getTitle());
        dto.setDescription(product.getDescription());
        dto.setPriceInCredits(product.getPriceInCredits());
        dto.setCategoryId(product.getCategoryId());
        dto.setCondition(product.getCondition());
        dto.setBrand(product.getBrand());
        dto.setStatus(product.getStatus());
        dto.setQuantity(product.getQuantity());
        dto.setCity(product.getCity());
        dto.setState(product.getState());
        dto.setViews(product.getViews());
        dto.setUniqueViews(product.getUniqueViews());
        dto.setWishlistCount(product.getWishlistCount());
        dto.setInquiryCount(product.getInquiryCount());
        dto.setIsFeatured(product.getIsFeatured());
        dto.setDistance(distance);
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        // Only expose coordinates if EXACT (or for seller/admin)
        if (LocationType.EXACT.equals(product.getLocationType())) {
            dto.setLatitude(product.getLatitude());
            dto.setLongitude(product.getLongitude());
        }

        // Load images
        dto.setImages(productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId())
                .stream().map(this::toImageDto).collect(Collectors.toList()));

        // Load seller summary
        sellerProfileRepository.findByUserId(product.getSellerId()).ifPresent(sp -> {
            SellerSummaryDto sellerDto = new SellerSummaryDto();
            sellerDto.setId(product.getSellerId());
            sellerDto.setDisplayName(sp.getDisplayName());
            sellerDto.setSellerLevel(sp.getSellerLevel());
            sellerDto.setAverageRating(sp.getAverageRating());
            sellerDto.setReviewCount(sp.getReviewCount());
            sellerDto.setIsVerified(sp.getIsVerified());
            sellerDto.setProfileImageUrl(sp.getProfileImageUrl());
            sellerDto.setCompletedOrders(sp.getCompletedOrders());
            dto.setSeller(sellerDto);
        });

        return dto;
    }

    public ProductSummaryDto toProductSummaryDto(Product product, Double distance) {
        ProductSummaryDto dto = ProductSummaryDto.builder()
                .id(product.getId())
                .title(product.getTitle())
                .priceInCredits(product.getPriceInCredits())
                .condition(product.getCondition() != null ? product.getCondition().name() : null)
                .city(product.getCity())
                .status(product.getStatus())
                .distance(distance)
                .createdAt(product.getCreatedAt())
                .build();

        // Primary image
        productImageRepository.findByProductIdAndIsPrimaryTrue(product.getId())
                .ifPresent(img -> dto.setPrimaryImageUrl(img.getUrl()));

        // Seller summary
        sellerProfileRepository.findByUserId(product.getSellerId()).ifPresent(sp -> {
            SellerSummaryDto sellerDto = new SellerSummaryDto();
            sellerDto.setId(product.getSellerId());
            sellerDto.setDisplayName(sp.getDisplayName());
            sellerDto.setSellerLevel(sp.getSellerLevel());
            sellerDto.setAverageRating(sp.getAverageRating());
            sellerDto.setReviewCount(sp.getReviewCount());
            sellerDto.setIsVerified(sp.getIsVerified());
            sellerDto.setProfileImageUrl(sp.getProfileImageUrl());
            sellerDto.setCompletedOrders(sp.getCompletedOrders());
            dto.setSeller(sellerDto);
        });

        return dto;
    }

    private ProductImageDto toImageDto(ProductImage image) {
        ProductImageDto dto = new ProductImageDto();
        dto.setId(image.getId());
        dto.setUrl(image.getUrl());
        dto.setIsPrimary(image.getIsPrimary());
        dto.setSortOrder(image.getSortOrder());
        return dto;
    }
}
