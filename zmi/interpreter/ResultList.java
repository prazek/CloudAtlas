package interpreter;

import model.Type;
import model.Value;
import model.ValueBoolean;
import model.ValueList;

import java.util.ArrayList;
import java.util.List;

public class ResultList extends Result {
    private ValueList values;

    public ResultList(ValueList values) {
        this.values = values;
    }

    @Override
    protected Result binaryOperationTyped(BinaryOperation operation, ResultSingle right) {
        return null;
    }

    @Override
    public Result unaryOperation(UnaryOperation operation) {
        return new ValueList(operation.perform(value));
    }

    interface Op {
        Value perform(Value x);
    }

    protected ResultList apply(Op op) {
        List<Value> vs = new ArrayList<>();
        for (int i = 0; i < values.size(); ++i) {

        }
        //return new ValueList(vs);
    }

    @Override
    protected Result callMe(BinaryOperation operation, Result left) {
        return null;
    }

    @Override
    public Value getValue()  {
        throw new UnsupportedOperationException("Not a ResultSingle.");
    }

    @Override
    public ValueList getList() {
        return values;
    }

    @Override
    public ValueList getColumn()    {
        throw new UnsupportedOperationException("Not a ResultColumn.");
    }

    @Override
    public Result filterNulls() {
        return new ResultSingle(filterNullsList(getList()));
    }

    @Override
    public ResultList first(int size) {
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
        return new ResultSingle(new ValueBoolean(values.isNull()));
    }

    @Override
    public Type getType() {
        return values.getType();
    }
}
