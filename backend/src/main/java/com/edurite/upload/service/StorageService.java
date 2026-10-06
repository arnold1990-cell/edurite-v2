package com.edurite.upload.service;

import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named StorageService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StorageService {

    @org.springframework.beans.factory.annotation.Value("${app.storage.directory:./data/uploads}")
    private String directory = "./data/uploads";

    /**
     * this method handles the "putObject" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String putObject(String bucket, String objectName, byte[] bytes) {
        if (!bucket.matches("[a-zA-Z0-9_-]+") || java.util.Arrays.stream(objectName.replace('\\', '/').split("/")).anyMatch(part -> part.equals("..") || part.equals("."))) throw new IllegalArgumentException("Invalid document path");
        String key = bucket + "/" + java.util.UUID.randomUUID() + "/" + objectName;
        java.nio.file.Path target = resolve(key);
        try {
            java.nio.file.Files.createDirectories(target.getParent());
            java.nio.file.Files.write(target, bytes, java.nio.file.StandardOpenOption.CREATE_NEW);
            return "storage://" + key;
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Document storage is unavailable. Please retry.", ex);
        }
    }

    public byte[] getObject(String location) throws java.io.IOException {
        if (location == null || !location.startsWith("storage://")) throw new java.io.FileNotFoundException("Document is not available in storage.");
        return java.nio.file.Files.readAllBytes(resolve(location.substring("storage://".length())));
    }

    private java.nio.file.Path resolve(String key) {
        java.nio.file.Path root = java.nio.file.Path.of(directory).toAbsolutePath().normalize();
        java.nio.file.Path target = root.resolve(key).normalize();
        if (!target.startsWith(root) || target.equals(root)) throw new IllegalArgumentException("Invalid document path");
        return target;
    }
}

