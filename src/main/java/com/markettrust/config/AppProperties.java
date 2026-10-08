package com.markettrust.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Platform platform = new Platform();
    private final Gemini gemini = new Gemini();
    private final GoogleMaps googleMaps = new GoogleMaps();
    private final Cloudinary cloudinary = new Cloudinary();
    private final Turnstile turnstile = new Turnstile();

    public Jwt getJwt() { return jwt; }
    public Platform getPlatform() { return platform; }
    public Gemini getGemini() { return gemini; }
    public GoogleMaps getGoogleMaps() { return googleMaps; }
    public Cloudinary getCloudinary() { return cloudinary; }
    public Turnstile getTurnstile() { return turnstile; }

    public static class Jwt {
        private String secret;
        private long accessTokenExpiration = 900000;
        private long refreshTokenExpiration = 604800000;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getAccessTokenExpiration() { return accessTokenExpiration; }
        public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }
        public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
        public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
    }

    public static class Platform {
        private int feeCredits = 10;
        private int maxProductImages = 10;
        private int maxListingDays = 90;
        private double defaultSearchRadiusKm = 50.0;

        public int getFeeCredits() { return feeCredits; }
        public void setFeeCredits(int feeCredits) { this.feeCredits = feeCredits; }
        public int getMaxProductImages() { return maxProductImages; }
        public void setMaxProductImages(int maxProductImages) { this.maxProductImages = maxProductImages; }
        public int getMaxListingDays() { return maxListingDays; }
        public void setMaxListingDays(int maxListingDays) { this.maxListingDays = maxListingDays; }
        public double getDefaultSearchRadiusKm() { return defaultSearchRadiusKm; }
        public void setDefaultSearchRadiusKm(double defaultSearchRadiusKm) { this.defaultSearchRadiusKm = defaultSearchRadiusKm; }
    }

    public static class Gemini {
        private String apiKey;
        private String apiUrl = "https://generativelanguage.googleapis.com/v1beta";
        private boolean enabled = false;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class GoogleMaps {
        private String apiKey;
        private boolean enabled = false;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
        private boolean enabled = false;

        public String getCloudName() { return cloudName; }
        public void setCloudName(String cloudName) { this.cloudName = cloudName; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Turnstile {
        private String siteKey;
        private String secretKey;
        private String verifyUrl = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
        private boolean enabled = false;

        public String getSiteKey() { return siteKey; }
        public void setSiteKey(String siteKey) { this.siteKey = siteKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getVerifyUrl() { return verifyUrl; }
        public void setVerifyUrl(String verifyUrl) { this.verifyUrl = verifyUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
