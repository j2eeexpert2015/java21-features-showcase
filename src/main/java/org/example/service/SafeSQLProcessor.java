package org.example.service;

import java.lang.StringTemplate;
import java.lang.StringTemplate.Processor;
import java.util.List;

// Custom processor for SQL injection prevention
public class SafeSQLProcessor implements Processor<String, RuntimeException> {

    public static final SafeSQLProcessor SAFE = new SafeSQLProcessor();

    @Override
    public String process(StringTemplate st) {
        // Get the string fragments and values
        List<String> fragments = st.fragments();
        List<Object> values = st.values();

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < fragments.size(); i++) {
            result.append(fragments.get(i));

            if (i < values.size()) {
                Object value = values.get(i);
                // Sanitize by removing dangerous SQL characters
                String sanitized = value.toString()
                        .replaceAll("[';\"\\-\\-]", "")
                        .trim();
                result.append(sanitized);
            }
        }

        return result.toString();
    }
}