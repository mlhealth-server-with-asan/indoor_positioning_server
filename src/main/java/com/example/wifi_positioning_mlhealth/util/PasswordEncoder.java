package com.example.wifi_positioning_mlhealth.util;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Component
public class PasswordEncoder {

    public String encrypt(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 85319, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            // Return the salt and hash, concatenated with a separator
            return Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean matches(String rawPassword, String storedHashWithSalt) {
        try {
            String[] parts = storedHashWithSalt.split("\\$");
            if (parts.length != 2) {
                return false; // The stored hash should have two parts: salt and hash
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            KeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, 85319, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = factory.generateSecret(spec).getEncoded();

            // Compare the newly generated hash with the stored hash
            return Base64.getEncoder().encodeToString(hash).equals(parts[1]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

