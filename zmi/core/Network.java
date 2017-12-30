package core;

public class Network extends Executor {

    private DatabaseUpdater databaseUpdater = null;


    void setDatabaseUpdater(DatabaseUpdater databaseUpdater) {
        this.databaseUpdater = databaseUpdater;
    }


    @Override
    void execute(ExecuteContext context) {

    }
}
