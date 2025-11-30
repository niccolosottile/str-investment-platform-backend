package com.str.platform.location.domain.model;

import com.str.platform.shared.domain.common.ValueObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Value object representing a physical address.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class Address {
    
    private final String city;
    private final String region;
    private final String country;
    private final String fullAddress;
    
    public Address(String city, String region, String country, String fullAddress) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City cannot be empty");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be empty");
        }
        
        this.city = city;
        this.region = region;
        this.country = country;
        this.fullAddress = fullAddress != null ? fullAddress : buildFullAddress(city, region, country);
    }
    
    private String buildFullAddress(String city, String region, String country) {
        StringBuilder sb = new StringBuilder(city);
        if (region != null && !region.isBlank()) {
            sb.append(", ").append(region);
        }
        sb.append(", ").append(country);
        return sb.toString();
    }
}
