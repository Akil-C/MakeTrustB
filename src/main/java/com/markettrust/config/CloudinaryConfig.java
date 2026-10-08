package com.markettrust.config;

import com.cloudinary.Cloudinary;
import com.markettrust.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CloudinaryConfig.class);

    private final AppProperties appProperties;

    /**
     * Cloudinary bean is only created when {@code app.cloudinary.enabled=true}.
     * In local/test environments it is simply absent and any service injecting it
     * must declare the dependency as {@code @Autowired(required = false)}.
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.cloudinary", name = "enabled", havingValue = "true")
    public Cloudinary cloudinary() {
        AppProperties.Cloudinary cfg = appProperties.getCloudinary();
        Cloudinary cloudinary = new Cloudinary(Map.of(
                "cloud_name", cfg.getCloudName(),
                "api_key",    cfg.getApiKey(),
                "api_secret", cfg.getApiSecret(),
                "secure",     true
        ));
        log.info("Cloudinary configured for cloud: {}", cfg.getCloudName());
        return cloudinary;
    }
}
