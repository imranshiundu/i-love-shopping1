package com.iloveshopping.entity;

import com.iloveshopping.service.DataEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Encrypts sensitive string columns at rest (AES-256-GCM via
 * {@link DataEncryptionService}). Null and blank values pass through
 * untouched; legacy plaintext rows decrypt transparently (the service
 * detects the {@code enc:v1:} prefix).
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        return DataEncryptionService.encryptStatic(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        return DataEncryptionService.decryptStatic(dbData);
    }
}
