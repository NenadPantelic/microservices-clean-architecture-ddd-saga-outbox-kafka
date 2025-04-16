package com.food.ordering.system.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) {

    public boolean isGreaterThanZero() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isGreater(Money money) {
        return amount != null && amount.compareTo(money.amount) > 0;
    }

    public Money add(Money money) {
        return new Money(setScale(amount.add(money.amount)));
    }

    public Money subtract(Money money) {
        return new Money(setScale(amount.subtract(money.amount)));
    }

    public Money multiply(int multiplier) {
        return new Money(setScale(amount.multiply(new BigDecimal(multiplier))));
    }

    private BigDecimal setScale(BigDecimal input) {
        // two decimals
        // after each BigDecimal operation, the result will be rounded to that scale
        // Java uses available bits to represent the repeating fractional numbers
        // this RoundingMode minimizes the error that is specific to floating-point numbers.
        // HALF_EVEN means round towards the nearest neighbour - if both neighbours are equidistant, round towards the
        // even neighbour.
        return input.setScale(2, RoundingMode.HALF_EVEN);
    }
}
