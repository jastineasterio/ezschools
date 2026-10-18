package com.edumanage.sms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SmsEncryptionService {

    @Value("${app.encryption.secret}")
    private String encryptionSecret;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    public String encrypt(String plainText) {
        try {
            byte[] key = deriveKey(encryptionSecret);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt value", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] key = deriveKey(encryptionSecret);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt value", e);
        }
    }

    private byte[] deriveKey(String secret) {
        byte[] key = new byte[32];
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, key, 0, Math.min(secretBytes.length, key.length));
        return key;
    }
}
