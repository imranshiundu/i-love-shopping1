package com.iloveshopping.db.migration;

import com.iloveshopping.service.DataEncryptionService;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Backfills the encrypted PII columns added in V19:
 * - users.email / users.name -> AES-256-GCM ciphertext (was plaintext)
 * - users.email_lookup -> HMAC-SHA256 of the lowercased email
 * - orders.guest_email -> ciphertext, orders.guest_email_lookup -> HMAC
 *
 * Runs as a Spring-managed Java migration so it can use the configured
 * DATA_ENCRYPTION_KEY (a SQL migration cannot compute the keyed hashes).
 * Re-runnable by design: rows are skipped when already encrypted.
 */
public class V20__Backfill_encrypted_pii extends BaseJavaMigration {

    private final DataEncryptionService encryption;

    public V20__Backfill_encrypted_pii(DataEncryptionService encryption) {
        this.encryption = encryption;
    }

    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion("20");
    }

    @Override
    public String getDescription() {
        return "backfill encrypted PII and lookup hashes";
    }

    @Override
    public Integer getChecksum() {
        return 20; // stable checksum: content depends on data, not on SQL text
    }

    @Override
    public void migrate(Context context) throws Exception {
        backfillUsers(context);
        backfillGuestOrders(context);
    }

    private void backfillUsers(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement();
             ResultSet rs = select.executeQuery("SELECT id, email, name FROM users")) {
            PreparedStatement update = context.getConnection().prepareStatement(
                    "UPDATE users SET email = ?, email_lookup = ?, name = ? WHERE id = ?");
            while (rs.next()) {
                String email = rs.getString("email");
                String name = rs.getString("name");
                String storedEmail = alreadyEncrypted(email) ? email : encryption.encrypt(email.trim().toLowerCase());
                String storedName = (name == null || name.isBlank() || alreadyEncrypted(name))
                        ? name : encryption.encrypt(name);
                update.setString(1, storedEmail);
                update.setString(2, encryption.lookupHash(email));
                update.setString(3, storedName);
                update.setString(4, rs.getString("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private void backfillGuestOrders(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement();
             ResultSet rs = select.executeQuery(
                     "SELECT id, guest_email FROM orders WHERE guest_email IS NOT NULL")) {
            PreparedStatement update = context.getConnection().prepareStatement(
                    "UPDATE orders SET guest_email = ?, guest_email_lookup = ? WHERE id = ?");
            while (rs.next()) {
                String guestEmail = rs.getString("guest_email");
                String stored = alreadyEncrypted(guestEmail) ? guestEmail : encryption.encrypt(guestEmail.trim().toLowerCase());
                update.setString(1, stored);
                update.setString(2, encryption.lookupHash(guestEmail));
                update.setString(3, rs.getString("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private boolean alreadyEncrypted(String value) {
        return value != null && value.startsWith("enc:v1:");
    }
}
