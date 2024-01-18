/*
 *  LowerCaseClassNameResolver.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms.apierror;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;


/**
 * Custom Jackson TypeIdResolver that generates type identifiers using lowercase simple class names.
 * This resolver is typically used with the {@code @JsonTypeIdResolver} annotation.
 *
 * <p>This resolver converts the simple class name of an object to lowercase to be used as a type identifier
 * during JSON serialization and deserialization.
 */
public class LowerCaseClassNameResolver extends TypeIdResolverBase {

    /**
     * Generates a lowercase type identifier from the simple class name of the specified object.
     *
     * @param value the object for which to generate the type identifier
     * @return a lowercase type identifier
     */
    @Override
    public String idFromValue(final Object value) {
        return value.getClass().getSimpleName().toLowerCase();
    }

    /**
     * Generates a lowercase type identifier from the simple class name of the specified object and suggested type.
     *
     * @param value         the object for which to generate the type identifier
     * @param suggestedType the suggested type (unused in this implementation)
     * @return a lowercase type identifier
     */
    @Override
    public String idFromValueAndType(final Object value, final Class<?> suggestedType) {
        return idFromValue(value);
    }

    /**
     * Returns the mechanism by which type identifiers are embedded in JSON.
     *
     * @return the type identifier mechanism (CUSTOM in this case)
     */
    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
}
