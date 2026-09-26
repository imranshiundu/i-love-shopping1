package com.iloveshopping.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "addresses", indexes = {
    @Index(name = "idx_addresses_user_id", columnList = "user_id"),
    @Index(name = "idx_addresses_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private AddressType type;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "name", nullable = false, length = 400)
    private String name;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "line1", nullable = false, length = 700)
    private String line1;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "line2", length = 700)
    private String line2;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "city", nullable = false, length = 400)
    private String city;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "state", nullable = false, length = 400)
    private String state;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "postal_code", nullable = false, length = 300)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 2)
    @Builder.Default
    private String country = "KE";

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone", length = 300)
    private String phone;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum AddressType {
        SHIPPING, BILLING
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(line1);
        if (line2 != null && !line2.isBlank()) {
            sb.append(", ").append(line2);
        }
        sb.append(", ").append(city);
        sb.append(", ").append(state);
        sb.append(" ").append(postalCode);
        sb.append(", ").append(country);
        return sb.toString();
    }
}