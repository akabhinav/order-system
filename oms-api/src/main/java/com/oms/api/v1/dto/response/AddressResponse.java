package com.oms.api.v1.dto.response;

public record AddressResponse(
    String street,
    String city,
    String state,
    String postalCode,
    String countryCode
) {}
