package com.kotopai.outing.engine.balance;

import com.kotopai.common.money.Money;
import java.util.Objects;

/** A registered user's final net for debt generation, including any guests they brought. */
public record UserPosition(long userId, Money net) {

    public UserPosition {
        Objects.requireNonNull(net, "net must not be null");
    }
}
