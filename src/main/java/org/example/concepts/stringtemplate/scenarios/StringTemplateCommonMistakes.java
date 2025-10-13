package org.example.concepts.stringtemplate.scenarios;

/**
 * Common Mistakes with String Templates & Security Considerations
 *
 * This class demonstrates:
 * 1. SQL Injection Prevention
 * 2. XSS Prevention
 * 3. Common syntax errors
 * 4. Performance pitfalls
 * 5. Proper escaping
 */
public class StringTemplateCommonMistakes {

    public static void main(String[] args) {
        System.out.println("=== String Template Common Mistakes & Security ===\n");

        demonstrateSQLInjectionRisk();
        demonstrateXSSRisk();
        demonstrateSyntaxMistakes();
        demonstrateProperUsage();
    }

    /**
     * MISTAKE #1: SQL Injection Risk
     *
     * ⚠️ WARNING: String Templates do NOT protect against SQL injection!
     * You still need parameterized queries.
     */
    private static void demonstrateSQLInjectionRisk() {
        System.out.println("━".repeat(70));
        System.out.println("❌ MISTAKE #1: SQL Injection Risk");
        System.out.println("━".repeat(70));

        String userInput = "admin' OR '1'='1"; // Malicious input

        // ❌ WRONG: Direct embedding in SQL (UNSAFE!)
        String unsafeSQL = STR."""
            SELECT * FROM users
            WHERE username = '\{userInput}'
            AND status = 'active'
            """;

        System.out.println("❌ UNSAFE SQL (String Template):");
        System.out.println(unsafeSQL);
        System.out.println("\n⚠️  Result: SQL INJECTION! This query returns ALL users!");
        System.out.println();

        // ✅ CORRECT: Use parameterized queries
        System.out.println("✅ SAFE APPROACH - Parameterized Query:");
        System.out.println("String sql = \"SELECT * FROM users WHERE username = ? AND status = 'active'\";");
        System.out.println("PreparedStatement ps = conn.prepareStatement(sql);");
        System.out.println("ps.setString(1, userInput);  // ← Safely escaped by JDBC");
        System.out.println();

        // ✅ ALTERNATIVE: Sanitize input first
        String sanitizedInput = userInput.replaceAll("[';\"\\-\\-]", "");
        String sanitizedSQL = STR."""
            -- Safe version (after sanitization)
            SELECT * FROM users
            WHERE username = '\{sanitizedInput}'
            AND status = 'active'
            """;

        System.out.println("✅ SAFER (with sanitization):");
        System.out.println(sanitizedSQL);
        System.out.println();
    }

    /**
     * MISTAKE #2: XSS (Cross-Site Scripting) Risk
     *
     * When generating HTML, you must escape user input!
     */
    private static void demonstrateXSSRisk() {
        System.out.println("━".repeat(70));
        System.out.println("❌ MISTAKE #2: XSS (Cross-Site Scripting) Risk");
        System.out.println("━".repeat(70));

        String userName = "<script>alert('XSS Attack!')</script>";
        String userComment = "This is <b>bold</b> and <script>malicious()</script>";

        // ❌ WRONG: Direct HTML generation without escaping
        String unsafeHTML = STR."""
            <div class="user-profile">
                <h1>Welcome, \{userName}!</h1>
                <div class="comment">\{userComment}</div>
            </div>
            """;

        System.out.println("❌ UNSAFE HTML:");
        System.out.println(unsafeHTML);
        System.out.println("\n⚠️  Result: XSS ATTACK! JavaScript will execute in browser!");
        System.out.println();

        // ✅ CORRECT: Escape HTML entities
        String safeUserName = escapeHtml(userName);
        String safeComment = escapeHtml(userComment);

        String safeHTML = STR."""
            <div class="user-profile">
                <h1>Welcome, \{safeUserName}!</h1>
                <div class="comment">\{safeComment}</div>
            </div>
            """;

        System.out.println("✅ SAFE HTML (after escaping):");
        System.out.println(safeHTML);
        System.out.println("\n✅ Result: Script tags displayed as text, not executed");
        System.out.println();
    }

    /**
     * MISTAKE #3: Common Syntax Errors
     */
    private static void demonstrateSyntaxMistakes() {
        System.out.println("━".repeat(70));
        System.out.println("❌ MISTAKE #3: Common Syntax Errors");
        System.out.println("━".repeat(70));

        String name = "John";
        int age = 30;

        // ❌ WRONG: Forgetting the backslash before curly brace
        // String wrong1 = STR."Name: {name}";  // Won't compile! Need \{name}
        System.out.println("❌ Wrong: STR.\"Name: {name}\"");
        System.out.println("   Error: { is not escaped. Should be \\{name}");
        System.out.println();

        // ❌ WRONG: Using wrong processor
        // String wrong2 = "Name: \{name}";  // Won't compile! Need STR."..."
        System.out.println("❌ Wrong: \"Name: \\{name}\"");
        System.out.println("   Error: Missing STR processor. Should be STR.\"Name: \\{name}\"");
        System.out.println();

        // ❌ WRONG: Null values can cause issues
        String nullValue = null;
        String withNull = STR."Value: \{nullValue}";
        System.out.println("⚠️  With null: " + withNull);
        System.out.println("   Result: 'Value: null' - handle nulls properly!");
        System.out.println();

        // ✅ CORRECT: Handle nulls
        String safeValue = nullValue != null ? nullValue : "N/A";
        String safe = STR."Value: \{safeValue}";
        System.out.println("✅ Correct: " + safe);
        System.out.println();
    }

    /**
     * ✅ PROPER USAGE PATTERNS
     */
    private static void demonstrateProperUsage() {
        System.out.println("━".repeat(70));
        System.out.println("✅ PROPER USAGE PATTERNS");
        System.out.println("━".repeat(70));

        System.out.println("\n1. Safe User Display:");
        String rawName = "<b>Admin</b>";
        String safeName = escapeHtml(rawName);
        String display = STR."Welcome, \{safeName}!";
        System.out.println(display);

        System.out.println("\n2. Safe SQL Generation:");
        String searchTerm = sanitizeInput("test' OR '1'='1");
        String sql = STR."SELECT * FROM products WHERE name LIKE '%\{searchTerm}%'";
        System.out.println(sql);
        System.out.println("(But better: use PreparedStatement!)");

        System.out.println("\n3. Null-Safe Templates:");
        String optionalField = null;
        String message = STR."Status: \{optionalField != null ? optionalField : "Not Set"}";
        System.out.println(message);

        System.out.println("\n4. Multi-Line with Proper Indentation:");
        String email = STR."""
            Dear Customer,

            \{indent("Thank you for your purchase!")}
            \{indent("Your order will arrive soon.")}

            Best regards
            """;
        System.out.println(email);
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER METHODS - Security Utilities
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Escape HTML to prevent XSS attacks
     */
    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * Sanitize SQL input (basic - still use PreparedStatement!)
     */
    private static String sanitizeInput(String input) {
        if (input == null) return "";
        return input.replaceAll("[';\"\\-\\-]", "");
    }

    /**
     * Add indentation to text
     */
    private static String indent(String text) {
        return "    " + text;
    }
}