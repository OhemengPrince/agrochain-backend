package com.agrochain.backend.service;

import com.agrochain.backend.exception.BadRequestException;
import com.agrochain.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "m4a", "mp4", "mp3", "wav");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final Path uploadDir;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + this.uploadDir, e);
        }
    }

    public String store(MultipartFile file) {
        return store(file, ALLOWED_EXTENSIONS, "Only image files (jpg, jpeg, png, webp) or audio files (m4a, mp4, mp3, wav) are allowed");
    }

    // Stricter variant for profile photos — audio isn't a valid profile picture,
    // even though the general upload endpoint accepts it for chat voice notes.
    public String storeImage(MultipartFile file) {
        return store(file, IMAGE_EXTENSIONS, "Only image files (jpg, jpeg, png, webp) are allowed");
    }

    private String store(MultipartFile file, Set<String> allowedExtensions, String rejectionMessage) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File exceeds maximum size of 5MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (extension.isEmpty() || !allowedExtensions.contains(extension)) {
            throw new BadRequestException(rejectionMessage);
        }

        String filename = UUID.randomUUID() + "." + extension;
        Path target = uploadDir.resolve(filename).normalize();

        try {
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file " + filename, e);
        }

        return filename;
    }

    public Resource load(String filename) {
        try {
            Path file = uploadDir.resolve(filename).normalize();
            if (!file.startsWith(uploadDir)) {
                throw new ResourceNotFoundException("File not found: " + filename);
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found: " + filename);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
