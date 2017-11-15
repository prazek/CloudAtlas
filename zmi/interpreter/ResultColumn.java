package interpreter;

import model.*;


public class ResultColumn extends Result {
    ValueList values;

    ResultColumn(ValueList val) {
        values = val;
    }

    @Override
    protected Result binaryOperationTyped(BinaryOperation operation, ResultSingle right) {
        return null;
    }

    @Override
    public Result unaryOperation(UnaryOperation operation) {
        return null;
    }

    @Override
    protected Result callMe(BinaryOperation operation, Result left) {
        return null;
    }

    @Override
    public Value getValue()   {
        throw new UnsupportedOperationException("Not a ResultSingle.");
    }

    @Override
    public ValueList getList() {
        throw new UnsupportedOperationException("Not a ResultList.");
    }

    @Override
    public ValueList getColumn() {
        if (values == null)
            return new ValueList(null, TypePrimitive.NULL);
        return values;
    }

    @Override
    public Result filterNulls() {
        if (values == null)
            return new ResultSingle(new ValueNull());
        return new ResultSingle(filterNullsList(getColumn()));
    }

    @Override
    public Result first(int size) {
        // TODO odfiltrowac null
        if (size > values.size())
            return new ResultSingle(values);
        return new ResultSingle((ValueList)values.subList(0, size - 1));
    }

    @Override
    public Result last(int size) {
        // TODO odfiltrowac null
        if (size > values.size())
            return new ResultSingle(values);
        return new ResultSingle((ValueList)values.subList(size, values.size() - 1));
    }

    @Override
    public Result random(int size) {
        return null;
    }

    @Override
    public Result convertTo(Type to) {
        return null;
    }

    @Override
    public ResultSingle isNull() {
        if (values == null)
            return new ResultSingle(new ValueBoolean(true));
        return new ResultSingle(new ValueBoolean(values.isNull()));
    }

    @Override
    public Type getType() {
        if (values == null)
            return TypePrimitive.NULL;
        return ((TypeCollection) values.getType()).getElementType();
    }
}
