package com.iloveshopping.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpesaCallbackRequest {

    private Body body;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body {
        private StkCallback stkCallback;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StkCallback {
        private String merchantRequestId;
        private String checkoutRequestId;
        private int resultCode;
        private String resultDesc;
        private CallbackMetadata callbackMetadata;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CallbackMetadata {
            private Item[] item;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Item {
            private String name;
            private Object value;
        }
    }
}