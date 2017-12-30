package core;

public class Network extends Executor {

    private DatabaseUpdater databaseUpdater;

    Network(DatabaseUpdater databaseUpdater) {
        this.databaseUpdater = databaseUpdater;
    }


    @Override
    void execute(ExecuteContext context) {

    }
}
