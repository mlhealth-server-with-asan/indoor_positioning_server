package com.example.wifi_positioning_mlhealth.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

public class PasswordGenerator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter new password: ");
        String rawPassword = scanner.nextLine();

        PasswordEncoder encoder = new PasswordEncoder();
        String encodedPassword = encoder.encrypt(rawPassword);

        String jsonContent = "{\"key\": \"" + encodedPassword + "\"}";

        Path filePath = Paths.get("C:\\Users\\qkrwo\\OneDrive\\바탕 화면\\wifi_positioning_mlhealth\\src\\main\\resources\\static\\password.json");
        try {
            Files.write(filePath, jsonContent.getBytes());
            System.out.println("Password file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to create password file.");
        }
    }
}