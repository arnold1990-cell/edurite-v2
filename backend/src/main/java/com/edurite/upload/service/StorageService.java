package com.edurite.upload.service;

import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named StorageService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StorageService {

    /**
     * this method handles the "putObject" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String putObject(String bucket, String objectName, byte[] bytes) {
        return "s3://" + bucket + "/" + objectName;
    }
}

