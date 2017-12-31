package core;

import interpreter.Interpreter;
import interpreter.QueryResult;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import model.*;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static java.lang.System.exit;
import static java.lang.Thread.sleep;




import interpreter.Interpreter;
import interpreter.QueryResult;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import model.*;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.*;

import static java.lang.System.exit;
import static java.lang.Thread.sleep;

public class RMIModule extends Executor implements AgentIface{

    private DatabaseUpdater databaseUpdater;

    RMIModule(DatabaseUpdater databaseUpdater) {
        this.databaseUpdater = databaseUpdater;
    }

    @Override
    void execute(ExecuteContext context) {
        if (context.data.hasZonesResponse()) {

        }

    }





    public Map<PathName, ZMI> zones() throws RemoteException {
        int id = currentId++;
        ExecuteContext context = new ExecuteContext(this,
                MessageOuterClass.Message.newBuilder()
                        .setZones(Database.Zones.newBuilder().setMsgID(id).build()).build());
        
        databaseUpdater.pushToExecute(context);

        // WAIT somehow
        Map<PathName, ZMI> result = new HashMap<>();
        for (Map.Entry<String, Model.ZMI> entry : responses.get(id).getZonesResponse().getZonesMap().entrySet()) {

            result.put(new PathName(entry.getKey()), ZMI.fromProtobuf(entry.getValue()));
        }
        return result;
    }

    public synchronized ZMI zone(PathName zoneName) throws RemoteException {
        ZMI zmi = zones().get(zoneName);
        return zmi;
    }

    public synchronized void installQuery(String queryCertificate, String query) throws RemoteException, Exception {

    }

    public synchronized void uninstallQuery(String queryCertificate) throws RemoteException {

    }

    public synchronized AttributesMap getQueries() throws RemoteException {
        return null;
    }

    public synchronized void setZoneValue(PathName zoneName, Attribute valueName, Value value) throws RemoteException {

    }

    public synchronized void setFallbackContacts(Set<ValueContact> fallbackContacts) {

    }

    public synchronized Set<ValueContact> getFallbackContacts() {
        return null;
    }


}
