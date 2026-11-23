package com.erp.controller;

import com.erp.util.BrandingStorage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Serves branding files from persistent storage outside the redeployed web application. */
public class BrandingAssetServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        String filename = pathInfo == null ? "" : pathInfo.substring(1);
        if (filename.isEmpty() || filename.contains("/") || filename.contains("\\")
                || !filename.matches("[0-9a-fA-F-]{36}\\.(png|jpg|gif)")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path file = BrandingStorage.directory().resolve(filename).normalize();
        if (!file.getParent().equals(BrandingStorage.directory()) || !Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        resp.setContentType(switch (extension) {
            case "png" -> "image/png";
            case "jpg" -> "image/jpeg";
            default -> "image/gif";
        });
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "public, max-age=3600");
        resp.setContentLengthLong(Files.size(file));
        Files.copy(file, resp.getOutputStream());
    }
}
