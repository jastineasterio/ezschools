package com.edumanage.branding.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Secure local file storage for school branding logos.
 *
 * <p>Files are stored on the local filesystem under a configurable base directory
 * (default {@code uploads/logos} relative to the application working directory).
 * Each stored file receives a random UUID-based name so the original filename is
 * never trusted or used, one school's logo cannot overwrite another's, and path
 * traversal is impossible.</p>
 *
 * <p>Production note: behind a reverse proxy or in a clustered environment, use a
 * shared network volume or object storage, and ensure the public logo URL is
 * constructed from the public-facing host (see {@code app.uploads.base-url}).
 */
@Service
public class FileStorageService {

    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2 MiB

    private final Path rootLocation;
    private final Path logosLocation;

    public FileStorageService(@Value("${app.uploads.location:uploads}") String location) {
        this.rootLocation = Paths.get(location).toAbsolutePath().normalize();
        this.logosLocation = this.rootLocation.resolve("logos");
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(logosLocation);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize logo storage directory: " + logosLocation, ex);
        }
    }

    /**
     * Store an uploaded logo image and return the generated file name.
     *
     * @throws IllegalArgumentException if the file is missing, too large, or not a
     *                                  supported image type.
     */
    public String storeLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo file is required");
        }

        long size = file.getSize();
        if (size > MAX_LOGO_SIZE) {
            throw new IllegalArgumentException("Logo file too large (maximum 2 MiB)");
        }

        String contentType = detectContentType(file);
        if (!isSupportedImage(contentType)) {
            throw new IllegalArgumentException("Unsupported logo format. Accepted: PNG, JPG, JPEG, WebP");
        }

        String extension = extensionFor(contentType);
        String fileName = UUID.randomUUID().toString() + "." + extension;

        try {
            Path target = logosLocation.resolve(fileName).normalize();
            if (!target.startsWith(logosLocation)) {
                throw new IllegalArgumentException("Invalid storage path");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store logo file: " + fileName, ex);
        }

        return fileName;
    }

    public void deleteLogo(String fileName) {
        if (!isSafeFileName(fileName)) {
            return;
        }
        try {
            Path file = logosLocation.resolve(fileName).normalize();
            if (file.startsWith(logosLocation)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ex) {
            // Best-effort cleanup; the database reference is cleared regardless.
        }
    }

    public Path resolveLogoPath(String fileName) {
        if (!isSafeFileName(fileName)) {
            throw new IllegalArgumentException("Invalid logo file name");
        }
        Path file = logosLocation.resolve(fileName).normalize();
        if (!file.startsWith(logosLocation)) {
            throw new IllegalArgumentException("Invalid logo file name");
        }
        return file;
    }

    public boolean logoExists(String fileName) {
        if (!isSafeFileName(fileName)) {
            return false;
        }
        return Files.exists(logosLocation.resolve(fileName));
    }

    private String detectContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)) {
            return contentType.trim().toLowerCase();
        }
        // Fall back to magic-byte detection so the original filename cannot be trusted.
        try {
            byte[] content = file.getBytes();
            byte[] header = new byte[Math.min(16, content.length)];
            System.arraycopy(content, 0, header, 0, header.length);
            if (header.length >= 4) {
                if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50
                        && header[2] == (byte) 0x4e && header[3] == (byte) 0x47) {
                    return "image/png";
                }
                if (header[0] == (byte) 0xff && header[1] == (byte) 0xd8 && header[2] == (byte) 0xff) {
                    return "image/jpeg";
                }
                if (header.length >= 12 && header[0] == (byte) 0x52 && header[1] == (byte) 0x49
                        && header[2] == (byte) 0x46 && header[3] == (byte) 0x46
                        && header[8] == (byte) 0x57 && header[9] == (byte) 0x45
                        && header[10] == (byte) 0x42 && header[11] == (byte) 0x50) {
                    return "image/webp";
                }
            }
        } catch (IOException ignored) {
        }
        return "";
    }

    private boolean isSupportedImage(String contentType) {
        return "image/png".equals(contentType)
                || "image/jpeg".equals(contentType)
                || "image/jpg".equals(contentType)
                || "image/webp".equals(contentType);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }

    private boolean isSafeFileName(String fileName) {
        return fileName != null
                && !fileName.isEmpty()
                && !fileName.contains("..")
                && !fileName.contains("/")
                && !fileName.contains("\\")
                && !fileName.startsWith("-");
    }
}
