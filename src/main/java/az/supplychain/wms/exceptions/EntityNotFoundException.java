/*
 *  EntityNotFoundException.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms.exceptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import org.springframework.util.StringUtils;


/**
 * Custom exception class representing the scenario where an entity is not found in the system.
 *
 * <p>This exception extends {@code RuntimeException} and is typically thrown when attempting to retrieve
 * an entity that does not exist.
 * <p>The exception provides information about the entity class and the search parameters that were used in
 * the unsuccessful attempt to find the entity.
 */
public class EntityNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 4351639179569687589L;


    /**
     * Constructs a new {@code EntityNotFoundException} with the specified entity class and search parameters.
     *
     * @param clazz           the entity class for which an instance was not found
     * @param searchParamsMap the search parameters used in the unsuccessful attempt
     */
    public EntityNotFoundException(final Class clazz, final String... searchParamsMap) {
        super(
                EntityNotFoundException.generateMessage(
                        clazz.getSimpleName(), toMap(String.class, String.class, searchParamsMap)));
    }

    /**
     * Generates a detailed message for the exception, including the entity name and search parameters.
     *
     * @param entity       the name of the entity
     * @param searchParams the search parameters used in the unsuccessful attempt
     * @return a detailed error message
     */
    private static String generateMessage(
            final String entity, final Map<String, String> searchParams) {
        return StringUtils.capitalize(entity) + " was not found for parameters " + searchParams;
    }

    /**
     * Converts an array of entries into a map with alternating key-value pairs.
     *
     * @param keyType   the class type of the keys
     * @param valueType the class type of the values
     * @param entries   an array of key-value pairs
     * @param <K>       the type of keys
     * @param <V>       the type of values
     * @return a map created from the input array
     * @throws IllegalArgumentException if the number of entries is not even
     */
    private static <K, V> Map<K, V> toMap(
            final Class<K> keyType, final Class<V> valueType, final Object... entries) {
        if (Objects.nonNull(entries) && entries.length % 2 == 1) {
            throw new IllegalArgumentException("Invalid entries");
        }
        return IntStream.range(0, entries.length / 2)
                .map(i -> i * 2)
                .collect(
                        HashMap::new,
                        (m, i) -> m.put(keyType.cast(entries[i]), valueType.cast(entries[i + 1])),
                        Map::putAll);
    }
}
