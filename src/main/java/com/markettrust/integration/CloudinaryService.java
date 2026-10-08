package com.markettrust.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

/**
 * Cloudinary integration service.
 * Falls back to local disk storage when {@code app.cloudinary.enabled=false}
 * (development mode), returning a local URL instead of a CDN URL.
 */
@Service
public class CloudinaryService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CloudinaryService.class);

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    @Value("${app.cloudinary.enabled:true}")
    private boolean cloudinaryEnabled;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    /** Local uploads directory used in development mode. */
    @Value("${app.local.upload-dir:uploads}")
    private String localUploadDir;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Validates and uploads an image file.
     *
     * @param file   the multipart file
     * @param folder Cloudinary folder (e.g. "products", "kyc")
     * @return map with keys {@code url} and {@code publicId}
     */
    public Map<String, String> uploadImage(MultipartFile file, String folder) {
        validateImageFile(file);

        if (!cloudinaryEnabled) {
            return uploadLocally(file, folder);
        }
        return uploadToCloudinary(file, folder);
    }

    /**
     * Deletes an image by its Cloudinary public ID.
     * No-op in local development mode.
     */
    public void deleteImage(String publicId) {
        if (!cloudinaryEnabled) {
            log.debug("Cloudinary disabled – skipping delete for publicId: {}", publicId);
            return;
        }
        try {
            Cloudinary cloudinary = buildCloudinary();
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Deleted Cloudinary asset: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete Cloudinary asset {}: {}", publicId, e.getMessage());
        }
    }

    /**
     * Validates that a file is an allowed image type and within the size limit.
     *
     * @throws com.markettrust.exception.BadRequestException on validation failure
     */
    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.markettrust.exception.BadRequestException("Image file must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new com.markettrust.exception.BadRequestException(
                    "Image file size must not exceed 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new com.markettrust.exception.BadRequestException(
                    "Only JPEG, PNG, and WebP images are allowed");
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Map<String, String> uploadToCloudinary(MultipartFile file, String folder) {
        try {
            Cloudinary cloudinary = buildCloudinary();
            @SuppressWarnings("unchecked")
            Map<Object, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "markettrust/" + folder,
                            "resource_type", "image",
                            "transformation", "q_auto,f_auto"
                    )
            );
            String url      = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            log.info("Uploaded to Cloudinary: publicId={}", publicId);
            return Map.of("url", url, "publicId", publicId);
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage(), e);
            throw new com.markettrust.exception.BadRequestException(
                    "Image upload failed. Please try again.");
        }
    }

    private Map<String, String> uploadLocally(MultipartFile file, String folder) {
        try {
            String filename  = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
            Path   uploadDir = Paths.get(localUploadDir, folder);
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename);
            file.transferTo(target);
            String localUrl  = "/uploads/" + folder + "/" + filename;
            String publicId  = folder + "/" + filename;
            log.debug("Saved locally: {}", target);
            return Map.of("url", localUrl, "publicId", publicId);
        } catch (IOException e) {
            log.error("Local file upload failed: {}", e.getMessage(), e);
            throw new com.markettrust.exception.BadRequestException(
                    "File upload failed. Please try again.");
        }
    }

    private Cloudinary buildCloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
        ));
    }

    private String sanitize(String originalFilename) {
        if (originalFilename == null) return "image";
        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
