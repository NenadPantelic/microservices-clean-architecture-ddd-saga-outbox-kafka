package com.food.ordering.system.domain.valueobject;

import java.util.Objects;

public abstract class BaseId<T> {

    private final T value;

    // only for subclasses
    protected BaseId(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof BaseId<?> baseId)) return false;
        return Objects.equals(getValue(), baseId.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }
}
