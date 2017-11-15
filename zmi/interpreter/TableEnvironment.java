package interpreter;

import model.ValueNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.ValueNull;


public class TableEnvironment {
    private final Table table;

    public TableEnvironment(Table table) {
        this.table = table;
    }

    public ResultColumn getIdent(String ident) {
        return new ResultColumn(table.getColumn(ident));
    }


}
