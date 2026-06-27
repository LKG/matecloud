package vip.mate.starter.security.encrypt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for field-level AES encryption.
 *
 * @author mateaix
 */
@Data
@ConfigurationProperties(prefix = "mate.security.encrypt")
public class EncryptProperties {

    /**
     * AES key (must be 16, 24 or 32 bytes for AES-128/192/256).
     */
    private String key;

    /**
     * Whether field-level encryption is enabled.
     */
    private boolean enabled = false;
}
