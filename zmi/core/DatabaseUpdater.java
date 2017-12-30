package core;

public class DatabaseUpdater extends Executor {

    //private RMIModule rmiModule;
    private Network network;
    private Timer timer;

    DatabaseUpdater(Network network, Timer timer) {
        //this.rmiModule = rmiModule;
        this.network = network;
        this.timer = timer;
    }

    @Override
    void execute(ExecuteContext context) {
        //Database.UpdateDatabase updateDatabase = context.data.getUpdateDatabase();


    }
}
