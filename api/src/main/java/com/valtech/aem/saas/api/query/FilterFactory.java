package com.valtech.aem.saas.api.query;

import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A helper factory for convenient instantiation of filter entries.
 */
public final class FilterFactory {

    /**
     * Convenience method for creating simple filter query item.
     *
     * @param key   filter query key.
     * @param value filter query value.
     * @return a filter object representing the filter query.
     */
    public static Filter createFilter(
            @NonNull String key,
            @NonNull String value) {
        return new SimpleFilter(key, value);
    }

    /**
     * Convenience method for creating composite filter with 'OR' joining operator.
     *
     * @param key    filter query key.
     * @param values list of filter query value.
     * @return a composite filter with an 'OR' joining operator
     */
    public static Filter createFilter(
            @NonNull String key,
            @NonNull List<String> values) {
        return createFilter(key, FilterJoinOperator.OR, values);
    }

    /**
     * Convenience method for creating composite filter.
     *
     * @param key          filter query key.
     * @param joinOperator joining operator.
     * @param values       list of filter query value.
     * @return a composite filter with an 'OR' joining operator
     */
    public static Filter createFilter(
            @NonNull String key,
            FilterJoinOperator joinOperator,
            @NonNull List<String> values) {
        if (CollectionUtils.isNotEmpty(values)) {
            if (values.size() > 1) {
                return CompositeFilter.builder()
                                      .joinOperator(joinOperator)
                                      .filters(values.stream()
                                                     .map(v -> createFilter(key, v))
                                                     .collect(Collectors.toSet()))
                                      .build();
            }
            return createFilter(key, values.get(0));
        }
        return new SimpleFilter();
    }

    private FilterFactory() {
        throw new UnsupportedOperationException();
    }
}
