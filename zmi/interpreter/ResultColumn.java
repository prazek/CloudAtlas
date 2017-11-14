package interpreter;

import model.Type;
import model.Value;
import model.ValueList;

public class ResultColumn extends Result {
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
        return null;
    }

    @Override
    public Result filterNulls() {
        return new ResultSingle(filterNullsList(getColumn()));
    }

    @Override
    public Result first(int size) {
        return null;
    }

    @Override
    public Result last(int size) {
        return null;
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
        return null;
    }

    @Override
    public Type getType() {
        return null;
    }
}
