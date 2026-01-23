package com.scaler.orderservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class DeliveryAddress {
    @Column(name = "address_line1", length = 255)
    private String line1;

    @Column(name = "address_line2", length = 255)
    private String line2;

    @Column(name = "address_city", length = 100)
    private String city;

    @Column(name = "address_state", length = 100)
    private String state;

    @Column(name = "address_postal_code", length = 20)
    private String postalCode;

    @Column(name = "address_country", length = 100)
    private String country;
}