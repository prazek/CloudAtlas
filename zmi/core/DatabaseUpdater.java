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
        if (context.data.hasZones()) {
            context.sender.pushToExecute(new ExecuteContext(this,
                    MessageOuterClass.Message.newBuilder().
                            setZonesResponse(Database.ZonesResponse.newBuilder().
                                    setResponseID(context.data.getZones().getMsgID()).build()).build())
                    );
        }

        //Database.UpdateDatabase updateDatabase = context.data.getUpdateDatabase();


    }
}
