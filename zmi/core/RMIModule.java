package core;

public class RMIModule extends Executor {

    private DatabaseUpdater databaseUpdater;

    RMIModule(DatabaseUpdater databaseUpdater) {
        this.databaseUpdater = databaseUpdater;
    }

    @Override
    void execute(ExecuteContext context) {

    }
}
