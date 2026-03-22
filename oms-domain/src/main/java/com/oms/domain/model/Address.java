package com.oms.domain.model;

import java.util.Objects;

public record Address(String street, String city, String state, String postalCode, String countryCode) {

    public Address {
        requireNonBlank(street, "street");
        requireNonBlank(city, "city");
        requireNonBlank(state, "state");
        requireNonBlank(postalCode, "postalCode");
        requireNonBlank(countryCode, "countryCode");
    }

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
