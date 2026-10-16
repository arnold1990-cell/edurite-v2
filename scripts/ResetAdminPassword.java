import java.io.Console;
import java.sql.DriverManager;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Offline operator recovery. Passwords are accepted only through a hidden interactive console. */
class ResetAdminPassword {
    public static void main(String[] args) throws Exception {
        Console console = System.console();
        if (console == null || args.length != 1) {
            throw new IllegalArgumentException("Run in an interactive terminal with the administrator email as the only argument.");
        }
        String url = System.getenv("SPRING_DATASOURCE_URL");
        if (url == null || !url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1|\\[::1\\]):[0-9]+/[^?;]+")) {
            throw new IllegalArgumentException("Use an explicitly configured loopback PostgreSQL connection. Remote recovery requires an authorized SSH tunnel.");
        }
        char[] password = console.readPassword("New administrator password: ");
        char[] confirmation = console.readPassword("Confirm new password: ");
        try {
            if (password == null || confirmation == null || !Arrays.equals(password, confirmation)
                    || password.length < 12 || new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("Passwords must match and contain at least 12 characters and at most 72 UTF-8 bytes.");
            }
            String encoded = new BCryptPasswordEncoder(12).encode(new String(password));
            try (var connection = DriverManager.getConnection(url, System.getenv("SPRING_DATASOURCE_USERNAME"),
                    System.getenv("SPRING_DATASOURCE_PASSWORD"))) {
                connection.setAutoCommit(false);
                try (var statement = connection.prepareStatement("""
                    UPDATE users SET password_hash=?, must_change_password=true, updated_at=CURRENT_TIMESTAMP
                    WHERE lower(email)=lower(?) AND status='ACTIVE' AND deleted_at IS NULL
                      AND EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id=ur.role_id
                                  WHERE ur.user_id=users.id AND r.name IN ('ROLE_ADMIN','ADMIN','ROLE_SUPER_ADMIN','SUPER_ADMIN'))
                    """)) {
                    statement.setString(1, encoded);
                    statement.setString(2, args[0].trim());
                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        throw new IllegalStateException("Recovery refused: expected exactly one active administrator.");
                    }
                    connection.commit();
                }
            }
            console.printf("Password reset completed. The administrator must change it after login.%n");
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}
