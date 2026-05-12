package com.ecommerce.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment gateway routing and external bank API placeholders (secrets via env only).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "payment")
public class PaymentGatewayProperties {

    /**
     * {@code simulator} (default) or {@code bb} for the HTTP stub client.
     */
    private String gateway = "simulator";

    private final Bb bb = new Bb();

    @Getter
    @Setter
    public static class Bb {

        /**
         * Base URL for bank HTTP APIs (set via environment in real deployments).
         */
        private String baseUrl = "";

        /**
         * OAuth2 token endpoint URL placeholder.
         */
        private String oauthTokenUrl = "";

        /**
         * Optional webhook shared secret name reference (actual value never stored in YAML).
         */
        private String webhookSecretEnv = "BB_WEBHOOK_SECRET";
    }
}
