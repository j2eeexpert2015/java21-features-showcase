package org.example.constants;

import java.util.List;

public class Java21Methods {
    // Sequenced Collections methods for cart operations
    public static final String ADD_FIRST = "addFirst";
    public static final String ADD_LAST = "addLast";
    public static final String GET_FIRST = "getFirst";
    public static final String GET_LAST = "getLast";
    public static final String REMOVE_LAST = "removeLast";
    public static final String REMOVE = "remove";
    public static final String CLEAR = "clear";

    // Common combinations for service responses
    public static final List<String> BASIC_OPERATIONS = List.of(GET_FIRST, GET_LAST);
    public static final List<String> ADD_OPERATIONS = List.of(ADD_LAST, GET_FIRST, GET_LAST);
    public static final List<String> PRIORITY_OPERATIONS = List.of(ADD_FIRST, GET_FIRST, GET_LAST);
}