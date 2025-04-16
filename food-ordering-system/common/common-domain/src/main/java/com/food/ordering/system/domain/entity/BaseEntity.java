package com.food.ordering.system.domain.entity;

import java.util.Objects;

// a boilerplate for other classes - they will use their own ID type
public abstract class BaseEntity<ID> {

    private ID id;

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

    // to support the uniqueness of an entity based on ID
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof BaseEntity<?> that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
