package com.bikebackend.servlet;

import com.bikebackend.storage.BikeImageStorage;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BikeImageServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        String filename = pathInfo == null ? null : pathInfo.substring(1);
        Path image;
        try {
            image = BikeImageStorage.resolve(filename);
        } catch (IOException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Bike image not found");
            return;
        }
        if (!Files.isRegularFile(image)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Bike image not found");
            return;
        }
        String contentType = getServletContext().getMimeType(image.getFileName().toString());
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        response.setContentLengthLong(Files.size(image));
        Files.copy(image, response.getOutputStream());
    }
}
