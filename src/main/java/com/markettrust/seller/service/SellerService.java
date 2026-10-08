package com.markettrust.seller.service;

import com.markettrust.exception.BadRequestException;
import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.integration.CloudinaryService;
import com.markettrust.seller.dto.*;
import com.markettrust.seller.entity.*;
import com.markettrust.seller.repository.SellerKycRepository;
import com.markettrust.seller.repository.SellerProfileRepository;
import com.markettrust.seller.repository.SellerTrustScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SellerService.class);

    private final SellerProfileRepository sellerProfileRepository;
    private final SellerKycRepository sellerKycRepository;
    private final SellerTrustScoreRepository sellerTrustScoreRepository;
    private final CloudinaryService cloudinaryService;

    public SellerProfileDto getPublicSellerProfile(Long userId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for user: " + userId));
        return toDto(profile);
    }

    public SellerProfileDto getMySellerProfile(Long userId) {
        return getPublicSellerProfile(userId);
    }

    @Transactional
    public SellerProfileDto updateSellerProfile(Long userId, UpdateSellerProfileRequest req) {
        SellerProfile profile = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for user: " + userId));

        if (req.getDisplayName() != null) profile.setDisplayName(req.getDisplayName());
        if (req.getBio() != null) profile.setBio(req.getBio());
        if (req.getCity() != null) profile.setCity(req.getCity());
        if (req.getState() != null) profile.setState(req.getState());
        if (req.getLatitude() != null) profile.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) profile.setLongitude(req.getLongitude());
        if (req.getProfileImageUrl() != null) profile.setProfileImageUrl(req.getProfileImageUrl());

        return toDto(sellerProfileRepository.save(profile));
    }

    @Transactional
    public KycStatusDto submitKyc(Long userId, SubmitKycRequest req, MultipartFile idDoc, MultipartFile selfie) {
        SellerProfile profile = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for user: " + userId));

        if (KycStatus.VERIFIED.equals(profile.getKycStatus())) {
            throw new BadRequestException("KYC is already verified.");
        }

        SellerKyc kyc = sellerKycRepository.findBySellerId(userId).orElse(new SellerKyc());
        kyc.setSellerId(userId);
        kyc.setIdType(req.getIdType());
        kyc.setIdNumber(req.getIdNumber());
        kyc.setAddressLine1(req.getAddressLine1());
        kyc.setAddressLine2(req.getAddressLine2());
        kyc.setCity(req.getCity());
        kyc.setState(req.getState());
        kyc.setPincode(req.getPincode());
        kyc.setDeclarationAccepted(req.getDeclarationAccepted());
        kyc.setStatus(KycStatus.PENDING);
        kyc.setSubmittedAt(LocalDateTime.now());

        if (idDoc != null && !idDoc.isEmpty()) {
            cloudinaryService.validateImageFile(idDoc);
            Map<String, String> upload = cloudinaryService.uploadImage(idDoc, "kyc/docs");
            kyc.setIdDocumentUrl(upload.get("url"));
        }

        if (selfie != null && !selfie.isEmpty()) {
            cloudinaryService.validateImageFile(selfie);
            Map<String, String> upload = cloudinaryService.uploadImage(selfie, "kyc/selfies");
            kyc.setSelfieUrl(upload.get("url"));
        }

        sellerKycRepository.save(kyc);

        profile.setKycStatus(KycStatus.PENDING);
        sellerProfileRepository.save(profile);

        log.info("KYC submitted for seller [userId={}]", userId);
        return toKycDto(kyc);
    }

    public KycStatusDto getKycStatus(Long userId) {
        SellerKyc kyc = sellerKycRepository.findBySellerId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No KYC submitted yet."));
        return toKycDto(kyc);
    }

    public Page<SellerProfileDto> getTrustedSellers(Pageable pageable) {
        return sellerProfileRepository.findByIsVerifiedTrueOrderByTrustScoreDesc(pageable)
                .map(this::toDto);
    }

    @Transactional
    public void recalculateTrustScore(Long sellerId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId).orElse(null);
        if (profile == null) return;

        double baseScore = 0;
        if (Boolean.TRUE.equals(profile.getIsVerified())) baseScore += 20;
        baseScore += (profile.getCompletedOrders() * 5);
        baseScore += (profile.getAverageRating() != null ? profile.getAverageRating() * 10 : 0);

        // Deduct for cancellations
        if (profile.getCancellationRate() != null && profile.getCancellationRate() > 10.0) {
            baseScore -= (profile.getCancellationRate() - 10.0) * 2;
        }

        double finalScore = Math.max(0, Math.min(100, baseScore));
        profile.setTrustScore(finalScore);

        // Update level
        if (finalScore >= 80) profile.setSellerLevel(SellerLevel.GOLD);
        else if (finalScore >= 60) profile.setSellerLevel(SellerLevel.SILVER);
        else if (finalScore >= 30) profile.setSellerLevel(SellerLevel.BRONZE);
        else profile.setSellerLevel(SellerLevel.NEW);

        sellerProfileRepository.save(profile);
    }

    public SellerProfileDto toDto(SellerProfile p) {
        SellerProfileDto dto = new SellerProfileDto();
        dto.setUserId(p.getUserId());
        dto.setDisplayName(p.getDisplayName());
        dto.setBio(p.getBio());
        dto.setCity(p.getCity());
        dto.setState(p.getState());
        dto.setSellerLevel(p.getSellerLevel());
        dto.setTrustScore(p.getTrustScore());
        dto.setIsVerified(p.getIsVerified());
        dto.setKycStatus(p.getKycStatus());
        dto.setAverageRating(p.getAverageRating());
        dto.setReviewCount(p.getReviewCount());
        dto.setCompletedOrders(p.getCompletedOrders());
        dto.setTotalSales(p.getTotalSales() != null ? p.getTotalSales().intValue() : 0);
        dto.setResponseRate(p.getResponseRate());
        dto.setCancellationRate(p.getCancellationRate());
        dto.setSellerSince(p.getSellerSince());
        dto.setProfileImageUrl(p.getProfileImageUrl());
        return dto;
    }

    private KycStatusDto toKycDto(SellerKyc k) {
        KycStatusDto dto = new KycStatusDto();
        dto.setId(k.getId());
        dto.setSellerId(k.getSellerId());
        dto.setIdType(k.getIdType());
        dto.setStatus(k.getStatus());
        dto.setRejectionReason(k.getRejectionReason());
        dto.setSubmittedAt(k.getSubmittedAt());
        dto.setReviewedAt(k.getReviewedAt());
        return dto;
    }
}
