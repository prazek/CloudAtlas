package interpreter;

import model.ValueNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.ValueNull;


public class TableEnvironment implements Environment {
    private final Table table;

    public TableEnvironment(Table table) {
        this.table = table;
    }

    public ResultColumn getIdent(String ident) {
        return new ResultColumn(table.getColumn(ident));
    }


}
