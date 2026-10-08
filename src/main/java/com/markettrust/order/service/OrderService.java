package com.markettrust.order.service;

import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.order.dto.*;
import com.markettrust.order.entity.Order;
import com.markettrust.order.entity.OrderStatus;
import com.markettrust.order.entity.OrderStatusHistory;
import com.markettrust.order.exception.InvalidOrderTransitionException;
import com.markettrust.order.exception.OrderAccessDeniedException;
import com.markettrust.order.repository.OrderRepository;
import com.markettrust.order.repository.OrderStatusHistoryRepository;
import com.markettrust.product.dto.ProductSummaryDto;
import com.markettrust.product.entity.Product;
import com.markettrust.product.entity.ProductImage;
import com.markettrust.product.entity.ProductStatus;
import com.markettrust.product.repository.ProductRepository;
import com.markettrust.review.repository.ReviewRepository;
import com.markettrust.seller.entity.SellerProfile;
import com.markettrust.seller.repository.SellerProfileRepository;
import com.markettrust.user.entity.RoleName;
import com.markettrust.user.entity.User;
import com.markettrust.user.entity.UserStatus;
import com.markettrust.user.repository.UserRepository;
import com.markettrust.wallet.exception.WalletFrozenException;
import com.markettrust.wallet.repository.WalletRepository;
import com.markettrust.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Core order lifecycle service.
 *
 * <p>Status transition rules:
 * <pre>
 * PENDING → CONFIRMED (seller) | CANCELLED (buyer / seller / admin)
 * CONFIRMED → PROCESSING (seller) | CANCELLED
 * PROCESSING → READY_FOR_SHIPPING (seller)
 * READY_FOR_SHIPPING → SHIPPED (seller)
 * SHIPPED → OUT_FOR_DELIVERY (seller) | DISPUTED (buyer)
 * OUT_FOR_DELIVERY → DELIVERED (seller) | DISPUTED (buyer)
 * DELIVERED → BUYER_CONFIRMED (buyer) | DISPUTED (buyer)
 * BUYER_CONFIRMED → COMPLETED (seller)
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);

    /** Platform takes 10 % of transaction value. */
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10");

    private final OrderRepository             orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ProductRepository           productRepository;
    private final UserRepository              userRepository;
    private final SellerProfileRepository     sellerProfileRepository;
    private final WalletRepository            walletRepository;
    private final WalletService               walletService;
    private final ReviewRepository            reviewRepository;

    // =========================================================================
    // Create order
    // =========================================================================

    @Transactional
    public OrderDto createOrder(Long buyerId, CreateOrderRequest req) {
        // 1. Idempotency
        return orderRepository.findByIdempotencyKey(req.idempotencyKey())
                .map(existing -> buildDto(existing, buyerId))
                .orElseGet(() -> doCreateOrder(buyerId, req));
    }

    private OrderDto doCreateOrder(Long buyerId, CreateOrderRequest req) {
        // 2. Load product
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + req.productId()));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new IllegalStateException("Product is not available for purchase");
        }
        if (product.getQuantity() < req.quantity()) {
            throw new IllegalStateException("Insufficient product quantity");
        }

        // 3. Seller not banned
        User sellerUser = userRepository.findById(product.getSellerId())
                .orElseThrow(() -> new IllegalStateException("Seller user not found"));
        if (sellerUser.getStatus() == UserStatus.BANNED || sellerUser.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("Seller account is not active");
        }

        // 4. Buyer != seller
        if (buyerId.equals(product.getSellerId())) {
            throw new IllegalStateException("You cannot purchase your own product");
        }

        // 5. Buyer wallet not frozen (checked inside holdCreditsForPurchase)
        walletRepository.findByUserId(buyerId)
                .filter(w -> Boolean.TRUE.equals(w.getIsFrozen()))
                .ifPresent(w -> { throw new WalletFrozenException("Your wallet is frozen"); });

        // 6. Calculate prices
        BigDecimal unitPrice    = BigDecimal.valueOf(product.getPriceInCredits());
        BigDecimal subtotal     = unitPrice.multiply(BigDecimal.valueOf(req.quantity()));
        BigDecimal platformFee  = subtotal.multiply(PLATFORM_FEE_RATE).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalPrice   = subtotal.add(platformFee);
        BigDecimal sellerAmount = subtotal.subtract(platformFee);

        // 7. Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setBuyerId(buyerId);
        order.setSellerId(sellerUser.getId());
        order.setProductId(product.getId());
        order.setQuantity(req.quantity());
        order.setUnitPrice(unitPrice);
        order.setTotalPrice(totalPrice);
        order.setPlatformFee(platformFee);
        order.setSellerAmount(sellerAmount);
        order.setDeliveryAddress(req.deliveryAddress());
        order.setNotes(req.notes());
        order.setIdempotencyKey(req.idempotencyKey());
        order.setStatus(OrderStatus.PENDING);
        Order saved = orderRepository.save(order);

        // 8. Hold credits (validates sufficient balance inside)
        String holdIdem = "HOLD-" + buyerId + "-" + req.idempotencyKey();
        walletService.holdCreditsForPurchase(buyerId, totalPrice, saved.getId(), holdIdem);

        // 9. Record initial status history
        recordHistory(saved.getId(), OrderStatus.PENDING, "Order placed", buyerId);

        // 10. Reduce product quantity / mark RESERVED
        int newQty = product.getQuantity() - req.quantity();
        product.setQuantity(newQty);
        if (newQty == 0) {
            product.setStatus(ProductStatus.RESERVED);
        }
        productRepository.save(product);

        log.info("Order {} created by buyer {} for product {}", saved.getOrderNumber(), buyerId, product.getId());
        return buildDto(saved, buyerId);
    }

    // =========================================================================
    // Confirm order (seller shortcut)
    // =========================================================================

    @Transactional
    public OrderDto confirmOrder(Long sellerId, Long orderId) {
        Order order = requireOrder(orderId);
        requireSeller(sellerId, order);
        transitionStatus(order, OrderStatus.CONFIRMED, sellerId, null, null);
        return buildDto(order, sellerId);
    }

    // =========================================================================
    // Update order status
    // =========================================================================

    @Transactional
    public OrderDto updateOrderStatus(Long userId, Long orderId, UpdateOrderStatusRequest req) {
        Order order = requireOrder(orderId);
        OrderStatus newStatus = req.newStatus();

        boolean isBuyer  = userId.equals(order.getBuyerId());
        boolean isSeller = userId.equals(order.getSellerId());
        boolean isAdmin  = isAdmin(userId);

        if (!isBuyer && !isSeller && !isAdmin) {
            throw new OrderAccessDeniedException("You are not a party to this order");
        }

        // Buyer-only transitions
        Set<OrderStatus> buyerTransitions = EnumSet.of(OrderStatus.BUYER_CONFIRMED, OrderStatus.DISPUTED);
        if (buyerTransitions.contains(newStatus) && !isBuyer && !isAdmin) {
            throw new OrderAccessDeniedException("Only the buyer can perform this action");
        }

        // Seller-only transitions
        Set<OrderStatus> sellerTransitions = EnumSet.of(
                OrderStatus.CONFIRMED, OrderStatus.PROCESSING,
                OrderStatus.READY_FOR_SHIPPING, OrderStatus.SHIPPED,
                OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.COMPLETED);
        if (sellerTransitions.contains(newStatus) && !isSeller && !isAdmin) {
            throw new OrderAccessDeniedException("Only the seller can perform this action");
        }

        transitionStatus(order, newStatus, userId, req.notes(), req.trackingNumber());

        // Post-transition side effects
        if (newStatus == OrderStatus.BUYER_CONFIRMED) {
            walletService.releaseHeldCreditsToSeller(
                    order.getId(), order.getSellerId(), order.getSellerAmount(), order.getPlatformFee());
            updateSellerStatsOnCompletion(order.getSellerId(), order.getSellerAmount());
        }

        return buildDto(order, userId);
    }

    // =========================================================================
    // Cancel order
    // =========================================================================

    @Transactional
    public OrderDto cancelOrder(Long userId, Long orderId, String reason) {
        Order order = requireOrder(orderId);
        boolean isBuyer  = userId.equals(order.getBuyerId());
        boolean isSeller = userId.equals(order.getSellerId());
        boolean isAdmin  = isAdmin(userId);

        if (!isBuyer && !isSeller && !isAdmin) {
            throw new OrderAccessDeniedException("You are not a party to this order");
        }

        Set<OrderStatus> cancellable = EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        if (!cancellable.contains(order.getStatus()) && !isAdmin) {
            throw new InvalidOrderTransitionException(
                    "Cannot cancel order in status: " + order.getStatus());
        }
        if (isFinalStatus(order.getStatus()) && !isAdmin) {
            throw new InvalidOrderTransitionException("Order is already in a final state");
        }

        transitionStatus(order, OrderStatus.CANCELLED, userId,
                reason != null ? reason : "Cancelled by user", null);

        // Refund held credits
        walletService.refundHeldCredits(order.getId());

        // Restore product
        restoreProductQuantity(order);

        // Update seller cancellation rate if seller cancelled
        if (isSeller) {
            updateSellerCancellationRate(order.getSellerId());
        }

        return buildDto(order, userId);
    }

    // =========================================================================
    // Dispute
    // =========================================================================

    @Transactional
    public OrderDto openDispute(Long buyerId, Long orderId, DisputeRequest req) {
        Order order = requireOrder(orderId);
        if (!buyerId.equals(order.getBuyerId())) {
            throw new OrderAccessDeniedException("Only the buyer can open a dispute");
        }

        Set<OrderStatus> disputable = EnumSet.of(
                OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);
        if (!disputable.contains(order.getStatus())) {
            throw new InvalidOrderTransitionException(
                    "Cannot open dispute in status: " + order.getStatus());
        }

        transitionStatus(order, OrderStatus.DISPUTED, buyerId,
                "Dispute: " + req.reason(), null);

        return buildDto(order, buyerId);
    }

    // =========================================================================
    // Queries
    // =========================================================================

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long userId, Long orderId) {
        Order order = requireOrder(orderId);
        boolean allowed = userId.equals(order.getBuyerId())
                || userId.equals(order.getSellerId())
                || isAdmin(userId);
        if (!allowed) {
            throw new OrderAccessDeniedException("Access denied to order " + orderId);
        }
        return buildDto(order, userId);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getMyOrdersAsBuyer(Long buyerId, Pageable pageable) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable)
                .map(o -> buildDto(o, buyerId));
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getMyOrdersAsSeller(Long sellerId, Pageable pageable) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable)
                .map(o -> buildDto(o, sellerId));
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getAllOrders(Long adminId, Pageable pageable) {
        requireAdminUser(adminId);
        return orderRepository.findAll(pageable).map(o -> buildDto(o, adminId));
    }

    @Transactional
    public OrderDto adminUpdateOrderStatus(Long adminId, Long orderId, UpdateOrderStatusRequest req) {
        requireAdminUser(adminId);
        Order order = requireOrder(orderId);
        transitionStatus(order, req.newStatus(), adminId, req.notes(), req.trackingNumber());
        return buildDto(order, adminId);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void transitionStatus(Order order, OrderStatus newStatus, Long changedBy,
                                  String notes, String trackingNumber) {
        validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        if (trackingNumber != null) {
            order.setTrackingNumber(trackingNumber);
        }
        orderRepository.save(order);
        recordHistory(order.getId(), newStatus, notes, changedBy);
        log.info("Order {} transitioned to {} by userId={}", order.getOrderNumber(), newStatus, changedBy);
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING          -> EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED).contains(next);
            case CONFIRMED        -> EnumSet.of(OrderStatus.PROCESSING, OrderStatus.READY_FOR_SHIPPING, OrderStatus.SHIPPED, OrderStatus.CANCELLED).contains(next);
            case PROCESSING       -> EnumSet.of(OrderStatus.READY_FOR_SHIPPING, OrderStatus.SHIPPED, OrderStatus.CANCELLED).contains(next);
            case READY_FOR_SHIPPING -> EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED).contains(next);
            case SHIPPED          -> EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.DISPUTED).contains(next);
            case OUT_FOR_DELIVERY -> EnumSet.of(OrderStatus.DELIVERED, OrderStatus.DISPUTED).contains(next);
            case DELIVERED        -> EnumSet.of(OrderStatus.BUYER_CONFIRMED, OrderStatus.DISPUTED).contains(next);
            case BUYER_CONFIRMED  -> EnumSet.of(OrderStatus.COMPLETED).contains(next);
            default               -> false;
        };
        if (!valid) {
            throw new InvalidOrderTransitionException(
                    "Cannot transition from " + current + " to " + next);
        }
    }

    private boolean isFinalStatus(OrderStatus status) {
        return EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED,
                OrderStatus.REFUNDED).contains(status);
    }

    private void recordHistory(Long orderId, OrderStatus status, String notes, Long changedBy) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrderId(orderId);
        h.setStatus(status);
        h.setNotes(notes);
        h.setChangedBy(changedBy);
        historyRepository.save(h);
    }

    private void restoreProductQuantity(Order order) {
        productRepository.findById(order.getProductId()).ifPresent(p -> {
            p.setQuantity(p.getQuantity() + order.getQuantity());
            if (p.getStatus() == ProductStatus.RESERVED) {
                p.setStatus(ProductStatus.ACTIVE);
            }
            productRepository.save(p);
        });
    }

    private void updateSellerStatsOnCompletion(Long sellerId, BigDecimal sellerAmount) {
        sellerProfileRepository.findByUserId(sellerId).ifPresent(sp -> {
            sp.setCompletedOrders(sp.getCompletedOrders() + 1);
            sp.setTotalSales(sp.getTotalSales().add(sellerAmount));
            sellerProfileRepository.save(sp);
        });
    }

    private void updateSellerCancellationRate(Long sellerId) {
        sellerProfileRepository.findByUserId(sellerId).ifPresent(sp -> {
            int total      = sp.getCompletedOrders() + 1; // approximate
            double rate    = sp.getCancellationRate() == null ? 0.0 : sp.getCancellationRate();
            sp.setCancellationRate(Math.min(1.0, rate + (1.0 / Math.max(total, 1))));
            sellerProfileRepository.save(sp);
        });
    }

    private Order requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private void requireSeller(Long sellerId, Order order) {
        if (!sellerId.equals(order.getSellerId())) {
            throw new OrderAccessDeniedException("Only the seller can perform this action");
        }
    }

    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN || r.getName() == RoleName.ROLE_ADMIN))
                .orElse(false);
    }

    private void requireAdminUser(Long adminId) {
        if (!isAdmin(adminId)) {
            throw new OrderAccessDeniedException("Admin access required");
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    // =========================================================================
    // DTO builder
    // =========================================================================

    private OrderDto buildDto(Order o, Long currentUserId) {
        User buyer  = userRepository.findById(o.getBuyerId()).orElse(null);
        User seller = userRepository.findById(o.getSellerId()).orElse(null);
        Product product = productRepository.findById(o.getProductId()).orElse(null);
        List<OrderStatusHistoryDto> history = historyRepository
                .findByOrderIdOrderByCreatedAtAsc(o.getId()).stream()
                .map(h -> new OrderStatusHistoryDto(h.getId(), h.getStatus().name(),
                        h.getNotes(), h.getChangedBy(), h.getCreatedAt()))
                .toList();

        boolean reviewable = (o.getStatus() == OrderStatus.BUYER_CONFIRMED
                || o.getStatus() == OrderStatus.COMPLETED)
                && currentUserId.equals(o.getBuyerId())
                && !reviewRepository.existsByOrderIdAndBuyerId(o.getId(), o.getBuyerId());

        return new OrderDto(
                o.getId(),
                o.getOrderNumber(),
                toUserSummary(buyer),
                toUserSummary(seller),
                toProductSummary(product),
                o.getQuantity(),
                o.getUnitPrice(),
                o.getTotalPrice(),
                o.getPlatformFee(),
                o.getSellerAmount(),
                o.getStatus(),
                o.getDeliveryAddress(),
                o.getTrackingNumber(),
                o.getNotes(),
                history,
                o.getCreatedAt(),
                o.getUpdatedAt(),
                reviewable
        );
    }

    private UserSummaryDto toUserSummary(User u) {
        if (u == null) return null;
        return new UserSummaryDto(u.getId(), u.getName(), u.getEmail(), u.getProfileImageUrl());
    }

    private ProductSummaryDto toProductSummary(Product p) {
        if (p == null) return null;
        String img = p.getImages().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                .findFirst().map(ProductImage::getUrl).orElse(null);
        return ProductSummaryDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .priceInCredits(p.getPriceInCredits())
                .primaryImageUrl(img)
                .status(p.getStatus())
                .build();
    }
}
