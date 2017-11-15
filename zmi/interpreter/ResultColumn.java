package interpreter;

import model.*;

import java.util.ArrayList;
import java.util.List;


public class ResultColumn extends Result {
    ValueList values;

    ResultColumn(ValueList val) {
        values = val;
    }

    @Override
    protected Result binaryOperationTyped(BinaryOperation operation, ResultSingle right) {
        if (values == null) {
            return this;
        }
        List<Value> result = new ArrayList<>();
        Value r = right.getValue();
        for (int i = 0; i < values.size(); ++i) {
            Value l = values.get(i);
            result.add(operation.perform(l, r));
        }
        if (result.isEmpty()) {
            return this;
        }
        return new ResultColumn(new ValueList(result, result.get(0).getType()));
        //return null;
    }

    @Override
    protected Result binaryOperationTyped(BinaryOperation operation, ResultColumn right) {
        if (values == null) {
            return this;
        }
        if (right.values == null || right.values.size() == 0) {
            return right;
        }
        List<Value> result = new ArrayList<>();
        for (int i = 0; i < values.size(); ++i) {
            Value l = values.get(i);
            // TODO right.values is null
            Value r = right.values.get(i);
            result.add(operation.perform(l, r));
        }
        if (result.isEmpty()) {
            return this;
        }
        return new ResultColumn(new ValueList(result, result.get(0).getType()));
    }

    @Override
    protected Result binaryOperationTyped(BinaryOperation operation, ResultList right) {
        throw new UnsupportedOperationException("Can't perform operation on column and list");
    }

    @Override
    public Result unaryOperation(UnaryOperation operation) {
        if (values == null) {
            return this;
        }
        List<Value> result = new ArrayList<>();
        for (Value v: values) {
            result.add(operation.perform(v));
        }
        if (result.isEmpty()) {
            return this;
        }
        return new ResultColumn(new ValueList(result, result.get(0).getType()));
    }

    @Override
    protected Result callMe(BinaryOperation operation, Result left) {
        return left.binaryOperationTyped(operation, this);
    }

    @Override
    public Value getValue() {
        if (values == null)
            return new ValueNull();
        return values;
        //throw new UnsupportedOperationException("Not a ResultSingle.");
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
        return new ResultSingle(firstList(values, size));
    }

    @Override
    public Result last(int size) {
        return new ResultSingle(lastList(values, size));
    }

    @Override
    public Result random(int size) {
        return new ResultSingle(randomList(values, size));
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
