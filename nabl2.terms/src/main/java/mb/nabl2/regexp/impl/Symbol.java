package mb.nabl2.regexp.impl;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpBuilder;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class Symbol<S> implements IRegExp<S> {

    @Value.Parameter public abstract S getSymbol();

    @Override
    @Value.Parameter public abstract IRegExpBuilder<S> getBuilder();

    @Override public boolean isNullable() {
        return false;
    }

    @Override public <T> T match(IRegExp.ICases<S,T> visitor) {
        return visitor.symbol(getSymbol());
    }

    @Override public String toString() {
        return getSymbol().toString();
    }

}
