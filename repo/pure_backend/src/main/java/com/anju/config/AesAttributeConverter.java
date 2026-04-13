package com.anju.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Converter
public class AesAttributeConverter implements AttributeConverter<String, String> {

    private static final String V2_PREFIX = "v2:";
    private static final String V1_PREFIX = "v1:";
    private static final String V1_ALGO = "AES/ECB/PKCS5Padding"; // legacy fallback
    private static final String V2_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        try {
            byte[] key = resolveKeyBytes();
            byte[] iv = new byte[GCM_IV_BYTES];
            RNG.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(V2_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            // store as: v2:base64(iv):base64(ct)
            return V2_PREFIX
                    + Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(ct);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt sensitive field", ex);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        try {
            // v2 format: v2:base64(iv):base64(ct)
            if (dbData.startsWith(V2_PREFIX)) {
                String[] parts = dbData.substring(V2_PREFIX.length()).split(":", 2);
                if (parts.length != 2) {
                    throw new IllegalStateException("Invalid v2 encrypted format");
                }
                byte[] iv = Base64.getDecoder().decode(parts[0]);
                byte[] ct = Base64.getDecoder().decode(parts[1]);
                byte[] key = resolveKeyBytes();
                Cipher cipher = Cipher.getInstance(V2_ALGO);
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
                return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
            }

            // v1 fallback (legacy ECB base64) for backward compatibility.
            // If the db value has no prefix, treat it as v1.
            String raw = dbData.startsWith(V1_PREFIX) ? dbData.substring(V1_PREFIX.length()) : dbData;
            byte[] key = resolveKeyBytesLegacyFallback();
            Cipher cipher = Cipher.getInstance(V1_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
            return new String(cipher.doFinal(Base64.getDecoder().decode(raw)), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt sensitive field", ex);
        }
    }

    private byte[] resolveKeyBytes() {
        String base64 = System.getenv("APP_AES_KEY_BASE64");
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("APP_AES_KEY_BASE64 is required");
        }
        byte[] key = Base64.getDecoder().decode(base64);
        if (!(key.length == 16 || key.length == 24 || key.length == 32)) {
            throw new IllegalStateException("APP_AES_KEY_BASE64 must decode to 16/24/32 bytes");
        }
        return key;
    }

    // Backward compatibility: if APP_AES_KEY_BASE64 is provided, reuse it for v1 decrypt too.
    // If not provided, fall back to legacy hardcoded key to avoid breaking existing databases.
    private byte[] resolveKeyBytesLegacyFallback() {
        String base64 = System.getenv("APP_AES_KEY_BASE64");
        if (base64 != null && !base64.isBlank()) {
            byte[] key = Base64.getDecoder().decode(base64);
            if (key.length >= 16) {
                // ECB uses first 16 bytes; keep compatibility if a longer key is provided.
                byte[] k16 = new byte[16];
                System.arraycopy(key, 0, k16, 0, 16);
                return k16;
            }
        }
        return "AnjuSecureKey123".getBytes(StandardCharsets.UTF_8);
    }
}
