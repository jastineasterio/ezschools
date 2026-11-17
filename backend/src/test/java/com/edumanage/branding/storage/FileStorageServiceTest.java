package com.edumanage.branding.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService newService() {
        FileStorageService service = new FileStorageService(tempDir.toString());
        service.init();
        return service;
    }

    private MultipartFile png(String name, byte[] content) {
        return new MockMultipartFile("file", name, "image/png", content);
    }

    @Test
    void storesPngAndReturnsSafeFileName() {
        FileStorageService service = newService();
        MultipartFile file = png("logo.png", new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47});

        String fileName = service.storeLogo(file);

        assertNotNull(fileName);
        assertEquals("png", fileName.substring(fileName.lastIndexOf('.') + 1));
        assertTrue(Files.exists(service.resolveLogoPath(fileName)));
        assertFalse(fileName.contains("logo"));
    }

    @Test
    void acceptsJpegAndWebp() {
        FileStorageService service = newService();
        assertDoesNotThrow(() -> service.storeLogo(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8})));
        assertDoesNotThrow(() -> service.storeLogo(new MockMultipartFile("file", "a.webp", "image/webp",
                new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50})));
    }

    @Test
    void rejectsEmptyFile() {
        FileStorageService service = newService();
        assertThrows(IllegalArgumentException.class,
                () -> service.storeLogo(new MockMultipartFile("file", "empty.png", "image/png", new byte[0])));
    }

    @Test
    void rejectsUnsupportedType() {
        FileStorageService service = newService();
        assertThrows(IllegalArgumentException.class,
                () -> service.storeLogo(new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3})));
    }

    @Test
    void doesNotTrustOriginalFilenameForType() {
        FileStorageService service = newService();
        // .png extension but PDF content type and content.
        assertThrows(IllegalArgumentException.class,
                () -> service.storeLogo(new MockMultipartFile("file", "logo.png", "application/pdf", new byte[]{1, 2, 3})));
    }

    @Test
    void blocksPathTraversalInDelete() {
        FileStorageService service = newService();
        assertDoesNotThrow(() -> service.deleteLogo("../../../etc/passwd"));
        assertFalse(Files.exists(tempDir.resolve("etc/passwd")));
    }

    @Test
    void blocksPathTraversalInResolve() {
        FileStorageService service = newService();
        assertThrows(IllegalArgumentException.class,
                () -> service.resolveLogoPath("../../../etc/passwd"));
    }
}
