package com.bikebackend.storage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class BikeImageStorage {
    private BikeImageStorage() {}

    public static final String URL_PREFIX = "/bike-images/";

    private static Path directory() throws IOException {
        String configured = System.getProperty("bike.upload.dir");
        Path dir = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "VRM-uploads", "bikes")
                : Path.of(configured);
        Files.createDirectories(dir);
        return dir.toAbsolutePath().normalize();
    }

    public static String store(HttpServletRequest request, Part image) throws IOException {
        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/"))
            throw new IOException("image must be an image file");

        String submitted = image.getSubmittedFileName();
        String extension = ".jpg";
        if (submitted != null && submitted.contains("."))
            extension = submitted.substring(submitted.lastIndexOf('.')).toLowerCase();
        else if ("image/png".equalsIgnoreCase(contentType)) extension = ".png";
        else if ("image/webp".equalsIgnoreCase(contentType)) extension = ".webp";
        else if ("image/gif".equalsIgnoreCase(contentType)) extension = ".gif";
        else if ("image/svg+xml".equalsIgnoreCase(contentType)) extension = ".svg";

        String filename = UUID.randomUUID() + extension;
        Path destination = directory().resolve(filename).normalize();
        try (InputStream input = image.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return request.getContextPath() + URL_PREFIX + filename;
    }

    public static Path resolve(String filename) throws IOException {
        if (filename == null || filename.isBlank() || filename.contains("..") || filename.contains("/") || filename.contains("\\"))
            throw new IOException("Invalid image filename");
        Path path = directory().resolve(filename).normalize();
        if (!path.getParent().equals(directory())) throw new IOException("Invalid image filename");
        return path;
    }
}
