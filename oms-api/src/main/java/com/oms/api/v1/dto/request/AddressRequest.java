package com.oms.api.v1.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    @NotBlank(message = "State is required") String state,
    @NotBlank(message = "Postal code is required") String postalCode,
    @NotBlank(message = "Country code is required") String countryCode
) {}
