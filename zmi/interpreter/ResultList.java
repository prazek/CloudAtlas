package interpreter;

import model.Type;
import model.Value;
import model.ValueBoolean;
import model.ValueList;

import java.util.ArrayList;
import java.util.List;

public class ResultList extends Result {
    // TODO(sbarzowski) - maybe a common parent with ResultColumn?
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
        //return new ValueList(operation.perform());
        return null;
    }

    interface Op {
        Value perform(Value x);
    }

    protected ResultList apply(Op op) {
        List<Value> vs = new ArrayList<>();
        for (int i = 0; i < values.size(); ++i) {
            vs.set(i, op.perform(values.get(i)));
        }
        return null; // TODO
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
        return new ResultSingle(new ValueBoolean(values.isNull()));
    }

    @Override
    public Type getType() {
        return values.getType();
    }
}
