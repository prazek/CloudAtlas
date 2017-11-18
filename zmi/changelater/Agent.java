package changelater;

import interpreter.Interpreter;
import interpreter.QueryResult;
import model.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.*;

import static java.lang.System.exit;

public class Agent implements AgentIface {
    private HashMap<PathName, ZMI> zones = new HashMap<>();
    private PathName pathName;

    Agent(PathName pathName) throws Exception {
        this.pathName = pathName;
        this.setRoot(ZMIConfig.getZMIConfiguration());

    }

    private synchronized void addZMI(ZMI zmi, PathName parentName) {
        // TODO what if name changes?
        // TODO what if some invalid value is saved as name?


        PathName path;
        if (parentName == null) {
            path = PathName.ROOT;
        } else {
            String name = ((model.ValueString) zmi.getAttributes().get("name")).getValue();
            path = parentName.levelDown(name);
        }
        System.out.println(path.getName());
        zones.put(path, zmi);
        for (ZMI son: zmi.getSons()) {
            addZMI(son, path);
        }
    }

    private void setRoot(ZMI zmi) {
        zones.clear();
        addZMI(zmi, null);
    }

    public HashMap<PathName, ZMI> zones() throws RemoteException {
        return this.zones;
    }

    public synchronized ZMI zone(PathName zoneName) throws RemoteException {

        ZMI zmi = zones().get(zoneName);
        System.err.println("ZONE:" + zoneName);
        return zmi;
    }

    private synchronized void runQueryInZone(ZMI zmi, String query) {
        Interpreter interpreter = new Interpreter(zmi);
        List<QueryResult> results = interpreter.run(query);
        for (QueryResult r : results)
            zmi.getAttributes().addOrChange(r.getName(), r.getValue());
    }

    private synchronized void installQueryInZone(ZMI z, String queryName, String query) {
        System.err.println("Installing query " );
        Value q = new ValueString(query); // TODO query certificate
        z.getAttributes().add(queryName, q);

        // TODO czy nie powinnismy odpalac w kazdym zonie? I pytanie czy to api tylko
        // robi to w ZMI
        runQueryInZone(z, query);
    }

    private synchronized void uninstallQueryInZone(ZMI z, String queryName) {
        // TODO(sbarzowski) remove query attributes
        z.getAttributes().remove(queryName);
    }

    public synchronized void installQuery(String name, String query) throws RemoteException {
        if (!name.startsWith("&"))
            throw new RuntimeException("name must starts with &");
        for (Map.Entry<PathName, ZMI> zone: this.zones.entrySet()) {
            installQueryInZone(zone.getValue(), name, query);
        }
    }

    public synchronized void uninstallQuery(String name) throws RemoteException {
        if (!name.startsWith("&"))
            throw new RuntimeException("name must starts with &");
        for (Map.Entry<PathName, ZMI> zone: this.zones.entrySet()) {
            uninstallQueryInZone(zone.getValue(), name);
        }
    }

    public synchronized void setZoneValue(PathName zoneName, Attribute valueName, Value value) throws RemoteException {
        System.out.println(zoneName + " " + valueName + " " + value.toString());

        if (zoneName.equals(pathName)) {
            zone(pathName).getAttributes().addOrChange(valueName, value);

        } else {
            System.err.println("not sure if error?");
        }
    }



    static public void main(String args[]) {
        if (args.length == 0) {
            System.err.println("Usage: ./agent zone_name");
            exit(1);
        }

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            String zoneName = args[0];
            Agent agent = new Agent(new PathName(zoneName));

            AgentIface stub =
                    (AgentIface) UnicastRemoteObject.exportObject(agent, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(zoneName, stub);
            System.out.println("Agent bound");
            RunQueries queryRunner = new RunQueries(agent);
            Thread t = new Thread(queryRunner);
            //t.run();
        } catch (Exception e) {
            System.err.println("Agent exception:");
            e.printStackTrace();
        }
    }

    static public class RunQueries implements Runnable {

        private Agent agent;
        RunQueries(Agent agent) { this.agent = agent; }

        public void run() {
            System.out.println("Updater running...");
            while (true) {

                for (Map.Entry<PathName, ZMI> zone: agent.zones.entrySet()) {
                    ZMI zmi = zone.getValue();
                    for (Map.Entry<Attribute, Value >attribute : zmi.getAttributes()) {
                        if (!Attribute.isQuery(attribute.getKey()))
                            continue;
                        ValueString query = (ValueString)attribute.getValue();
                        agent.runQueryInZone(zmi, query.getValue());
                    }
                }
                try {
                    wait(5 * 100);
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }
    }
}
