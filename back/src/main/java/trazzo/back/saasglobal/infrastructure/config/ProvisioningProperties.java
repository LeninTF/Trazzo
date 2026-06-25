package trazzo.back.saasglobal.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trazzo.provisioning")
public record ProvisioningProperties(
        String adminUrl,
        String adminUsername,
        String adminPassword,
        String dbHost,
        String dbPort
) {}
