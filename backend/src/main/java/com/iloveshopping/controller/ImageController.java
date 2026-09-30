package com.iloveshopping.controller;

import com.iloveshopping.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileStorageService.storeImage(file));
    }

    /**
     * Serves a stored image in a specific size variant (thumb/medium/full)
     * so different views request appropriately sized files.
     */
    @GetMapping("/{size}/{filename}")
    public ResponseEntity<FileSystemResource> serveImage(
            @PathVariable String size,
            @PathVariable String filename) {
        FileStorageService.ImageSize variant;
        try {
            variant = FileStorageService.ImageSize.valueOf(size.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        try {
            FileSystemResource resource = new FileSystemResource(fileStorageService.resolve(variant, filename));
            MediaType type = MediaType.APPLICATION_OCTET_STREAM;
            String name = filename.toLowerCase();
            if (name.endsWith(".png")) {
                type = MediaType.IMAGE_PNG;
            } else if (name.endsWith(".gif")) {
                type = MediaType.IMAGE_GIF;
            } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                type = MediaType.IMAGE_JPEG;
            }
            return ResponseEntity.ok()
                    .contentType(type)
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
