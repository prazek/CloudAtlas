package core;

import model.PathName;
import model.ZMI;


import java.util.HashMap;
import java.util.Map;

public class DatabaseUpdater extends Executor {
    private HashMap<PathName, ZMI> zones = new HashMap<>();
    private PathName pathName;
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
            Map<String, Model.ZMI> map = new HashMap<>();
            for (Map.Entry<PathName, ZMI> entry : zones.entrySet()) {
                map.put(entry.getKey().getName(), entry.getValue().serialize());
            }
            Database.ZonesResponse zonesResponse =
                    Database.ZonesResponse.newBuilder()
                            .setResponseID(context.data.getZones().getMsgID())
                            .putAllZones(map)
                    .build();

            context.sender.pushToExecute(new ExecuteContext(this,
                    MessageOuterClass.Message.newBuilder().
                            setZonesResponse(zonesResponse)
                            .build())
                    );
        }
        //else if (context.data.)



    }
}
