package com.iloveshopping.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    /** Image size variants served for different views (thumbnails, cards, full-size). */
    public enum ImageSize {
        THUMB(256), MEDIUM(512), FULL(1024);

        final int maxEdge;
        ImageSize(int maxEdge) { this.maxEdge = maxEdge; }
        public int maxEdge() { return maxEdge; }
    }

    public FileStorageService() {
        try {
            for (ImageSize size : ImageSize.values()) {
                Files.createDirectories(this.fileStorageLocation.resolve(size.name().toLowerCase()));
            }
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files are stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        return storeImage(file).get("url");
    }

    /**
     * Stores an uploaded image in multiple sizes (thumb/medium/full) so every view
     * can be served an appropriately sized file. Non-image files are stored once
     * under full/. Returns the public URLs for each generated size.
     */
    public Map<String, String> storeImage(MultipartFile file) {
        if (file.getOriginalFilename() == null) {
            throw new RuntimeException("Sorry! Filename contains invalid path sequence");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).toLowerCase();
        }
        String baseName = UUID.randomUUID().toString();
        String fileName = baseName + ext;
        if (fileName.contains("..")) {
            throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
        }

        Map<String, String> urls = new LinkedHashMap<>();
        try {
            BufferedImage source = ImageIO.read(file.getInputStream());
            if (source == null) {
                // Not a readable image — store as-is, serve same bytes for every size
                Path target = this.fileStorageLocation.resolve(ImageSize.FULL.name().toLowerCase()).resolve(fileName);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                for (ImageSize size : ImageSize.values()) {
                    urls.put(sizeKey(size), publicUrl(ImageSize.FULL, fileName));
                }
                return urls;
            }
            for (ImageSize size : ImageSize.values()) {
                Path target = this.fileStorageLocation.resolve(size.name().toLowerCase()).resolve(fileName);
                if (size == ImageSize.FULL || Math.max(source.getWidth(), source.getHeight()) > size.maxEdge()) {
                    BufferedImage scaled = scale(source, size.maxEdge());
                    ImageIO.write(scaled, formatName(ext), target.toFile());
                } else {
                    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                }
                urls.put(sizeKey(size), publicUrl(size, fileName));
            }
            return urls;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    /** Streams a previously stored file for the given size variant. */
    public Path resolve(ImageSize size, String fileName) {
        if (fileName == null || fileName.contains("..") || fileName.contains("/")) {
            throw new RuntimeException("Invalid file name");
        }
        Path base = this.fileStorageLocation.resolve(size.name().toLowerCase());
        Path resolved = base.resolve(fileName).normalize();
        if (!resolved.startsWith(this.fileStorageLocation)) {
            throw new RuntimeException("Invalid file name");
        }
        if (!Files.exists(resolved) && size != ImageSize.FULL) {
            resolved = this.fileStorageLocation.resolve(ImageSize.FULL.name().toLowerCase()).resolve(fileName);
        }
        if (!Files.exists(resolved)) {
            throw new RuntimeException("File not found");
        }
        return resolved;
    }

    private BufferedImage scale(BufferedImage source, int maxEdge) {
        int w = source.getWidth();
        int h = source.getHeight();
        if (Math.max(w, h) <= maxEdge) {
            return source;
        }
        double factor = (double) maxEdge / Math.max(w, h);
        int nw = Math.max(1, (int) Math.round(w * factor));
        int nh = Math.max(1, (int) Math.round(h * factor));
        BufferedImage target = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, nw, nh, null);
        g.dispose();
        return target;
    }

    private String formatName(String ext) {
        String e = ext.startsWith(".") ? ext.substring(1) : ext;
        return switch (e) {
            case "png" -> "png";
            case "gif" -> "gif";
            default -> "jpg";
        };
    }

    private String sizeKey(ImageSize size) {
        return switch (size) {
            case THUMB -> "thumbnailUrl";
            case MEDIUM -> "mediumUrl";
            case FULL -> "url";
        };
    }

    private String publicUrl(ImageSize size, String fileName) {
        return "/api/v1/images/" + size.name().toLowerCase() + "/" + fileName;
    }
}
