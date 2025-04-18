package com.food.ordering.system.order.service.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record StreetAddress(UUID id, String street, String postalCode, String city) {

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof StreetAddress that)) return false;
        return Objects.equals(street(), that.street()) &&
                Objects.equals(postalCode(), that.postalCode()) &&
                Objects.equals(city(), that.city());
    }

    @Override
    public int hashCode() {
        return Objects.hash(street(), postalCode(), city());
    }
}
