package org.example.service;

import java.lang.StringTemplate;
import java.lang.StringTemplate.Processor;
import java.util.List;

// Custom processor for SQL injection prevention
public class SafeSQLProcessor implements Processor<String, RuntimeException> {

    // Reusable singleton instance
    public static final SafeSQLProcessor SAFE = new SafeSQLProcessor();

    @Override
    public String process(StringTemplate st) {
        // Get the string fragments and values from the template
        List<String> fragments = st.fragments();
        List<Object> values = st.values();

        StringBuilder result = new StringBuilder();

        // Merge fragments with sanitized values
        for (int i = 0; i < fragments.size(); i++) {
            result.append(fragments.get(i));

            if (i < values.size()) {
                Object value = values.get(i);
                String sanitized = sanitizeValue(value);
                result.append(sanitized);
            }
        }

        return result.toString();
    }

    /**
     * Very simple SQL sanitization for demo purposes.
     * In production, always use PreparedStatement instead of building SQL strings.
     */
    private String sanitizeValue(Object value) {
        String str = String.valueOf(value);

        // Escape / strip potentially dangerous patterns
        str = str.replace("'", "''");        // escape single quotes
        str = str.replace("--", "");         // remove line comments
        str = str.replace(";", "");          // remove statement terminators
        str = str.replace("/*", "");         // remove block comment start
        str = str.replace("*/", "");         // remove block comment end

        // Crude keyword blocking (case-insensitive) just for demo
        str = str.replaceAll("(?i)DROP", "");
        str = str.replaceAll("(?i)DELETE", "");
        str = str.replaceAll("(?i)UPDATE", "");
        str = str.replaceAll("(?i)INSERT", "");

        return str.trim();
    }
}
