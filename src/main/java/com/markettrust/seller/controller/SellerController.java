package com.markettrust.seller.controller;

import com.markettrust.security.SecurityUtils;
import com.markettrust.seller.dto.*;
import com.markettrust.seller.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller Profile & KYC", description = "Public profile, seller setup, and KYC workflow")
public class SellerController {

    private final SellerService sellerService;

    @GetMapping("/{userId}/public")
    @Operation(summary = "Get public seller profile (no private KYC or document data exposed)")
    public ResponseEntity<SellerProfileDto> getPublicProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(sellerService.getPublicSellerProfile(userId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get own seller profile")
    public ResponseEntity<SellerProfileDto> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(sellerService.getMySellerProfile(userId));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update own seller profile")
    public ResponseEntity<SellerProfileDto> updateProfile(@Valid @RequestBody UpdateSellerProfileRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(sellerService.updateSellerProfile(userId, req));
    }

    @GetMapping("/me/kyc")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get own KYC submission status")
    public ResponseEntity<KycStatusDto> getKycStatus() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(sellerService.getKycStatus(userId));
    }

    @PostMapping(value = "/me/kyc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Submit KYC verification documents")
    public ResponseEntity<KycStatusDto> submitKyc(
            @Valid @RequestPart("data") SubmitKycRequest req,
            @RequestPart(value = "idDocument", required = false) MultipartFile idDoc,
            @RequestPart(value = "selfie", required = false) MultipartFile selfie) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(sellerService.submitKyc(userId, req, idDoc, selfie));
    }

    @GetMapping("/trusted")
    @Operation(summary = "Get top trusted sellers list (public)")
    public ResponseEntity<Page<SellerProfileDto>> getTrustedSellers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(sellerService.getTrustedSellers(PageRequest.of(page, size)));
    }
}
