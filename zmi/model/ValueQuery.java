package model;

public class ValueQuery extends Value {
    @Override
    public Type getType() {
        return null;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public Value convertTo(Type to) {
        throw new UnsupportedConversionException(getType(), to);
    }

    @Override
    public Value getDefaultValue() {
        return null;
    }
}
