package com.iloveshopping.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResult {

    private String fileName;
    private int totalRows;
    @Builder.Default
    private int created = 0;
    @Builder.Default
    private int updated = 0;
    @Builder.Default
    private int skipped = 0;
    @Builder.Default
    private List<RowError> errors = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int row;
        private String identifier;
        private String reason;
    }
}
