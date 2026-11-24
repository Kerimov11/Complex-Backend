package az.arena.icearena.service;

import az.arena.icearena.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Vəsiqə və təsdiq şəkillərini bazada base64 TEXT kimi deyil,
 * fayl/obyekt saxlanışında saxlayır və bazaya yalnız fayl yolunu qaytarır.
 * Maksimum fayl ölçüsü: 2 MB.
 */
@Service
public class DocumentStorageService {

    public static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024; // 2 MB
    public static final String FILE_PREFIX = "file:";

    private final Path storageRoot;

    public DocumentStorageService(
            @Value("${app.storage.documents-dir:./data/uploads/documents}") String storageDir) {
        this.storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    /**
     * Base64 data URL-i yoxlayır (format və <= 2 MB ölçü limiti), fayl sisteminə yazır
     * və bazada saxlanılacaq nisbi fayl yolunu ("file:...") qaytarır.
     */
    public String storePhoto(String rawDataUrl, String categoryPrefix) {
        String cleaned = rawDataUrl == null ? "" : rawDataUrl.trim();
        if (cleaned.isEmpty()) {
            throw new BadRequestException("Şəkil tələb olunur.");
        }
        if (cleaned.startsWith(FILE_PREFIX)) {
            return cleaned;
        }
        if (!cleaned.startsWith("data:image/") || cleaned.length() < 100) {
            throw new BadRequestException("Şəxsiyyət vəsiqəsi və ya sənəd ilə foto təsdiqi tələb olunur.");
        }

        int base64Index = cleaned.indexOf(";base64,");
        if (base64Index < 0) {
            throw new BadRequestException("Şəkil formatı düzgün deyil (base64 tələb olunur).");
        }

        String header = cleaned.substring("data:image/".length(), base64Index).toLowerCase(Locale.ROOT);
        String ext = resolveExtension(header);
        String base64Payload = cleaned.substring(base64Index + ";base64,".length()).trim();

        // Base64 uzunluğuna görə ilkin sürətli limit yoxlaması (~2.8 MB base64 > 2 MB binary)
        if (base64Payload.length() > (MAX_IMAGE_BYTES * 4 / 3) + 1024) {
            throw new BadRequestException("Yüklənən şəkil 2 MB limitini aşır (maksimum 2 MB).");
        }

        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(base64Payload);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Şəkil base64 məlumatı oxuna bilmədi.");
        }

        if (bytes.length == 0) {
            throw new BadRequestException("Yüklənən şəkil boşdur.");
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Yüklənən şəkil 2 MB limitini aşır (maksimum 2 MB).");
        }

        String safePrefix = (categoryPrefix == null || categoryPrefix.isBlank())
                ? "doc"
                : categoryPrefix.replaceAll("[^a-zA-Z0-9_-]", "");
        String dateFolder = LocalDate.now().toString();
        String fileName = safePrefix + "-" + UUID.randomUUID() + "." + ext;
        String relativePath = dateFolder + "/" + fileName;

        Path target = storageRoot.resolve(relativePath).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new BadRequestException("Yanlış fayl yolu.");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException ex) {
            throw new IllegalStateException("Şəkil fayl saxlanışına yazıla bilmədi", ex);
        }

        return FILE_PREFIX + relativePath;
    }

    /**
     * Opsional şəkil sahəsi üçün: əgər data:image/ göndərilibsə 2 MB limitini yoxlayıb fayla yazır,
     * boşdursa null qaytarır.
     */
    public String storeOptionalPhoto(String rawDataUrl, String categoryPrefix) {
        String cleaned = rawDataUrl == null ? "" : rawDataUrl.trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        return storePhoto(cleaned, categoryPrefix);
    }

    /**
     * Bazada saxlanan fayl yolunu ("file:...") kassir/admin baxışı üçün oxuyur.
     * Köhnə ("data:image/...") yazılarla da geriyə uyğun işləyir.
     */
    public String resolvePhotoForReview(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }
        String trimmed = storedValue.trim();
        if (trimmed.startsWith("data:image/")) {
            return trimmed;
        }
        if (!trimmed.startsWith(FILE_PREFIX)) {
            return null;
        }
        String relativePath = trimmed.substring(FILE_PREFIX.length());
        Path target = storageRoot.resolve(relativePath).normalize();
        if (!target.startsWith(storageRoot) || !Files.exists(target)) {
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(target);
            String mime = resolveMimeType(target.getFileName().toString());
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException ex) {
            return null;
        }
    }

    public static String maskFin(String finCode) {
        if (finCode == null || finCode.isBlank()) {
            return null;
        }
        String trimmed = finCode.trim();
        if (trimmed.length() <= 3) {
            return "***";
        }
        return "*".repeat(trimmed.length() - 3) + trimmed.substring(trimmed.length() - 3);
    }

    private String resolveExtension(String header) {
        if (header.contains("png")) {
            return "png";
        }
        if (header.contains("webp")) {
            return "webp";
        }
        return "jpg";
    }

    private String resolveMimeType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
