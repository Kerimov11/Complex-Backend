package az.arena.icearena.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * FİN kod və şəxsiyyət vəsiqəsi nömrəsi kimi həssas şəxsi məlumatları
 * verilənlər bazasında AES-256-GCM ilə şifrələnmiş şəkildə saxlayır.
 */
@Component
@Converter
public class SensitiveDataEncryptor implements AttributeConverter<String, String> {

    private static final String PREFIX = "ENC:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static volatile SecretKeySpec secretKey = buildKey("IceArenaDefaultEncryptionSecretKey2026");

    @Value("${app.security.encryption-key:${jwt.secret:IceArenaDefaultEncryptionSecretKey2026}}")
    public void setEncryptionKey(String rawSecret) {
        if (rawSecret != null && !rawSecret.isBlank()) {
            secretKey = buildKey(rawSecret.trim());
        }
    }

    private static SecretKeySpec buildKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Şifrələmə açarı yaradıla bilmədi", ex);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        if (attribute.startsWith(PREFIX)) {
            return attribute;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Həssas məlumat şifrələnə bilmədi", ex);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        if (!dbData.startsWith(PREFIX)) {
            // Köhnə şifrəsiz yazı varsa oxuna bilsin
            return dbData;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData.substring(PREFIX.length()));
            if (decoded.length <= IV_LENGTH_BYTES) {
                return dbData;
            }
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return dbData;
        }
    }
}
