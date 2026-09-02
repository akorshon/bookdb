package com.bookdb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /** Stores an uploaded image (book cover, author photo, ...) and returns its public URL path. */
    public String storeImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalStateException("File is empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalStateException("File exceeds the 5MB limit");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalStateException("Only jpg, png, webp or gif images are allowed");
        }

        try {
            Path dir = Path.of(uploadDir);
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + "." + extension.toLowerCase();
            Path target = dir.resolve(filename);
            file.transferTo(target);
            return "/api/files/" + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }
}
