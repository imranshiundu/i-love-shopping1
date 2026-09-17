package com.iloveshopping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iloveshopping.dto.catalog.BulkUploadResult;
import com.iloveshopping.entity.Brand;
import com.iloveshopping.entity.Category;
import com.iloveshopping.entity.Product;
import com.iloveshopping.repository.BrandRepository;
import com.iloveshopping.repository.CategoryRepository;
import com.iloveshopping.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bulk product import from JSON or CSV (admin only).
 *
 * <p>Each row creates a product when its SKU is new, or updates the existing
 * product with that SKU. Category and brand resolve by id, then slug, then
 * case-insensitive name. Invalid rows are skipped and reported — valid rows
 * still import.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ObjectMapper objectMapper;
    private final CatalogService catalogService;

    @Transactional
    public BulkUploadResult importProducts(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        BulkUploadResult.BulkUploadResultBuilder builder = BulkUploadResult.builder().fileName(name);
        List<BulkUploadResult.RowError> errors = new ArrayList<>();

        try {
            if (name.endsWith(".json")) {
                return importJson(file, errors);
            } else if (name.endsWith(".csv")) {
                return importCsv(file, errors);
            }
            throw new IllegalArgumentException("Unsupported file type — upload a .json or .csv file");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Bulk import failed", e);
            throw new IllegalArgumentException("Could not read the uploaded file: " + e.getMessage());
        }
    }

    private BulkUploadResult importJson(MultipartFile file, List<BulkUploadResult.RowError> errors) throws Exception {
        JsonNode root = objectMapper.readTree(file.getInputStream());
        if (!root.isArray()) {
            throw new IllegalArgumentException("JSON must be an array of product objects");
        }

        Map<String, Category> categories = categoryMap();
        Map<String, Brand> brands = brandMap();
        int created = 0, updated = 0, skipped = 0;

        for (int i = 0; i < root.size(); i++) {
            JsonNode node = root.get(i);
            int row = i + 1;
            try {
                Map<String, String> map = new HashMap<>();
                map.put("name", text(node, "name"));
                map.put("description", text(node, "description"));
                map.put("price", text(node, "price"));
                map.put("compareAtPrice", text(node, "compareAtPrice"));
                map.put("sku", text(node, "sku"));
                map.put("stock", text(node, "stock"));
                map.put("weight", text(node, "weight"));
                map.put("weightUnit", text(node, "weightUnit"));
                map.put("dimensions", text(node, "dimensions"));
                map.put("category", textOr(node, "categoryId", textOr(node, "categorySlug", text(node, "category"))));
                map.put("brand", textOr(node, "brandId", textOr(node, "brandSlug", text(node, "brand"))));
                map.put("isActive", text(node, "isActive"));
                String sku = map.get("sku");
                Result r = upsert(map, categories, brands, sku);
                if (r == Result.CREATED) created++; else if (r == Result.UPDATED) updated++; else skipped++;
            } catch (Exception e) {
                skipped++;
                errors.add(BulkUploadResult.RowError.builder().row(row)
                        .identifier(text(node, "sku")).reason(e.getMessage()).build());
            }
        }
        return BulkUploadResult.builder()
                .fileName(fileNameOf(file)).totalRows(root.size())
                .created(created).updated(updated).skipped(skipped).errors(errors).build();
    }

    private BulkUploadResult importCsv(MultipartFile file, List<BulkUploadResult.RowError> errors) throws Exception {
        Map<String, Category> categories = categoryMap();
        Map<String, Brand> brands = brandMap();
        int created = 0, updated = 0, skipped = 0, row = 0;
        String identifier = "";

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            List<String> headers = parseCsvLine(headerLine);
            for (int i = 0; i < headers.size(); i++) {
                headers.set(i, headers.get(i).trim());
            }

            String line;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                try {
                    List<String> cells = parseCsvLine(line);
                    Map<String, String> map = new HashMap<>();
                    for (int i = 0; i < headers.size() && i < cells.size(); i++) {
                        map.put(headers.get(i), cells.get(i).trim());
                    }
                    String sku = map.get("sku");
                    identifier = sku;
                    Result r = upsert(map, categories, brands, sku);
                    if (r == Result.CREATED) created++; else if (r == Result.UPDATED) updated++; else skipped++;
                } catch (Exception e) {
                    skipped++;
                    errors.add(BulkUploadResult.RowError.builder().row(row).identifier(identifier).reason(e.getMessage()).build());
                }
            }
        }
        return BulkUploadResult.builder()
                .fileName(fileNameOf(file)).totalRows(row)
                .created(created).updated(updated).skipped(skipped).errors(errors).build();
    }

    private Result upsert(Map<String, String> map, Map<String, Category> categories, Map<String, Brand> brands, String sku) {
        String name = required(map, "name");
        String description = required(map, "description");
        String priceRaw = required(map, "price");
        String stockRaw = required(map, "stock");
        String categoryId = required(map, "category");
        String brandId = required(map, "brand");

        BigDecimal price;
        try {
            price = new BigDecimal(priceRaw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("price '" + priceRaw + "' is not a number");
        }
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("price must be positive");
        }
        Integer stock;
        try {
            stock = Integer.valueOf(stockRaw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("stock '" + stockRaw + "' is not a whole number");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }

        Category category = resolve(categories, categoryId, "category");
        Brand brand = resolve(brands, brandId, "brand");

        BigDecimal compareAtPrice = null;
        if (map.get("compareAtPrice") != null && !map.get("compareAtPrice").isBlank()) {
            try {
                compareAtPrice = new BigDecimal(map.get("compareAtPrice"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("compareAtPrice '" + map.get("compareAtPrice") + "' is not a number");
            }
        }

        boolean isActive = map.get("isActive") == null || map.get("isActive").isBlank()
                || map.get("isActive").equalsIgnoreCase("true") || map.get("isActive").equalsIgnoreCase("yes");

        var existing = productRepository.findBySkuIgnoreCase(sku);
        if (existing.isPresent()) {
            Product p = existing.get();
            p.setName(name);
            p.setDescription(description);
            p.setPrice(price);
            p.setCompareAtPrice(compareAtPrice);
            p.setStock(stock);
            p.setWeight(parseDecimal(map.get("weight")));
            p.setWeightUnit(blankToNull(map.get("weightUnit")));
            p.setDimensions(blankToNull(map.get("dimensions")));
            p.setCategory(category);
            p.setBrand(brand);
            p.setIsActive(isActive);
            productRepository.save(p);
            return Result.UPDATED;
        }

        Product p = Product.builder()
                .name(name)
                .slug(catalogService.generateSlugPublic(name))
                .description(description)
                .price(price)
                .compareAtPrice(compareAtPrice)
                .sku(sku)
                .stock(stock)
                .weight(parseDecimal(map.get("weight")))
                .weightUnit(blankToNull(map.get("weightUnit")))
                .dimensions(blankToNull(map.get("dimensions")))
                .isActive(isActive)
                .category(category)
                .brand(brand)
                .build();
        productRepository.save(p);
        return Result.CREATED;
    }

    private Map<String, Category> categoryMap() {
        Map<String, Category> map = new HashMap<>();
        for (Category c : categoryRepository.findAll()) {
            map.put(c.getId(), c);
            if (c.getSlug() != null) map.put(c.getSlug().toLowerCase(), c);
            if (c.getName() != null) map.put(c.getName().toLowerCase(), c);
        }
        return map;
    }

    private Map<String, Brand> brandMap() {
        Map<String, Brand> map = new HashMap<>();
        for (Brand b : brandRepository.findAll()) {
            map.put(b.getId(), b);
            if (b.getSlug() != null) map.put(b.getSlug().toLowerCase(), b);
            if (b.getName() != null) map.put(b.getName().toLowerCase(), b);
        }
        return map;
    }

    private <T> T resolve(Map<String, T> map, String key, String kind) {
        T value = map.get(key) != null ? map.get(key) : map.get(key.toLowerCase());
        if (value == null) {
            throw new IllegalArgumentException(kind + " '" + key + "' not found (match by id, slug or name)");
        }
        return value;
    }

    private String required(Map<String, String> map, String field) {
        String value = map.get(field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }

    private String textOr(JsonNode node, String field, String fallback) {
        String value = text(node, field);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String fileNameOf(MultipartFile file) {
        return file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
    }

    /**
     * Minimal CSV line parser: handles quoted cells with embedded commas
     * and escaped double quotes.
     */
    static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }

    private enum Result {
        CREATED, UPDATED, SKIPPED
    }
}
