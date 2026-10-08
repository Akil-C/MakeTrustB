package com.markettrust.admin.service;

import com.markettrust.admin.dto.*;
import com.markettrust.seller.dto.KycStatusDto;
import com.markettrust.seller.dto.SellerProfileDto;
import com.markettrust.seller.entity.KycStatus;
import com.markettrust.seller.entity.SellerKyc;
import com.markettrust.seller.entity.SellerProfile;
import com.markettrust.seller.repository.SellerKycRepository;
import com.markettrust.seller.repository.SellerProfileRepository;
import com.markettrust.seller.service.SellerService;
import com.markettrust.user.entity.User;
import com.markettrust.user.entity.UserStatus;
import com.markettrust.user.repository.UserRepository;
import com.markettrust.product.repository.ProductRepository;
import com.markettrust.order.repository.OrderRepository;
import com.markettrust.wallet.service.WalletService;
import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final SellerKycRepository sellerKycRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final WalletService walletService;
    private final SellerService sellerService;

    public AdminDashboardDto getDashboardStats() {
        AdminDashboardDto dto = new AdminDashboardDto();
        dto.setTotalUsers(userRepository.count());
        dto.setTotalSellers(sellerProfileRepository.count());
        dto.setActiveProducts(productRepository.countByStatus(com.markettrust.product.entity.ProductStatus.ACTIVE));
        dto.setTotalOrders(orderRepository.count());
        dto.setCompletedOrders(orderRepository.countByStatus(com.markettrust.order.entity.OrderStatus.COMPLETED));
        dto.setPendingKyc(sellerKycRepository.countByStatus(KycStatus.PENDING));

        try {
            dto.setTotalCreditsInCirculation(walletService.getTotalCreditsInCirculation().longValue());
            dto.setPlatformFeeCollected(walletService.getTotalPlatformRevenue().longValue());
        } catch (Exception e) {
            dto.setTotalCreditsInCirculation(0L);
            dto.setPlatformFeeCollected(0L);
        }
        return dto;
    }

    @Transactional
    public void approveKyc(Long adminId, Long sellerId, String notes) {
        SellerKyc kyc = sellerKycRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC not found for seller: " + sellerId));

        kyc.setStatus(KycStatus.VERIFIED);
        kyc.setReviewedBy(adminId);
        kyc.setAdminNotes(notes);
        kyc.setReviewedAt(LocalDateTime.now());
        sellerKycRepository.save(kyc);

        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found: " + sellerId));

        profile.setKycStatus(KycStatus.VERIFIED);
        profile.setIsVerified(true);
        sellerProfileRepository.save(profile);

        sellerService.recalculateTrustScore(sellerId);
        log.info("Admin [{}] approved KYC for seller [{}]", adminId, sellerId);
    }

    @Transactional
    public void rejectKyc(Long adminId, Long sellerId, String reason) {
        SellerKyc kyc = sellerKycRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC not found for seller: " + sellerId));

        kyc.setStatus(KycStatus.REJECTED);
        kyc.setRejectionReason(reason);
        kyc.setReviewedBy(adminId);
        kyc.setReviewedAt(LocalDateTime.now());
        sellerKycRepository.save(kyc);

        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId).orElse(null);
        if (profile != null) {
            profile.setKycStatus(KycStatus.REJECTED);
            sellerProfileRepository.save(profile);
        }
        log.info("Admin [{}] rejected KYC for seller [{}] with reason: {}", adminId, sellerId, reason);
    }

    @Transactional
    public void suspendUser(Long adminId, Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        log.info("Admin [{}] suspended user [{}]: {}", adminId, userId, reason);
    }

    @Transactional
    public void unsuspendUser(Long adminId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Admin [{}] unsuspended user [{}]", adminId, userId);
    }

    @Transactional
    public void banUser(Long adminId, Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);
        log.info("Admin [{}] banned user [{}]: {}", adminId, userId, reason);
    }

    @Transactional
    public void unbanUser(Long adminId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Admin [{}] unbanned user [{}]", adminId, userId);
    }
}
