package interpreter;

import model.ValueList;
import model.ValueNull;

public class TableEnvironment implements Environment {
    private final Table table;

    public TableEnvironment(Table table) {
        this.table = table;
    }

    public ResultColumn getIdent(String ident) {
        try {
            return new ResultColumn(table.getColumn(ident));
        } catch (NoSuchAttributeException ex) {
            return new ResultColumn(null);
            //return new ResultColumn(new ValueList(null));
        }
    }


}
