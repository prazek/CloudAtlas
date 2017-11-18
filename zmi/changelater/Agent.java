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
import static java.lang.Thread.sleep;

public class Agent implements AgentIface {
    private HashMap<PathName, ZMI> zones = new HashMap<>();
    private PathName pathName;
    // This map stores which attributes are created by running one query.
    private HashMap<Attribute, List<Attribute>> queryAttributes = new HashMap<>();

    Agent(PathName pathName) throws Exception {
        this.pathName = pathName;
        this.setRoot(ZMIConfig.getZMIConfiguration());

    }

    private void setRoot(ZMI zmi) {
        zones.clear();
        addZMI(zmi, null);
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


    public HashMap<PathName, ZMI> zones() throws RemoteException {
        return this.zones;
    }

    public synchronized ZMI zone(PathName zoneName) throws RemoteException {
        ZMI zmi = zones().get(zoneName);
        return zmi;
    }

    private synchronized List<QueryResult> runQueryInZone(ZMI zmi, String query) throws Exception {
        Interpreter interpreter = new Interpreter(zmi);
        List<QueryResult> results = interpreter.run(query);
        return results;
    }

    private void applyQueryRunChanges(ZMI zmi, List<QueryResult> results) {
        for (QueryResult r : results) {
            zmi.getAttributes().addOrChange(r.getName(), r.getValue());
            System.out.println("Applying result for [" + r.getName() + "] with value [" + r.getValue() + "]");
        }
    }


    private synchronized void installQueryInZone(ZMI zmi, String queryName, String query) throws Exception {
        System.err.println("Installing query " );
        Value q = new ValueString(query); // TODO query certificate

        if (zmi.getAttributes().getOrNull(queryName) != null) {
            throw new RuntimeException("Duplicated query of name [" + queryName + "]");
        }

        List<QueryResult> results = runQueryInZone(zmi, query);

        // Put attributes if first run of this query
        if (!queryAttributes.containsKey(queryName)) {
            ArrayList<Attribute> createdAttributes = new ArrayList<>();
            for (QueryResult r : results) {
                Attribute producedValueName = r.getName();
                createdAttributes.add(producedValueName);
                if (zmi.getAttributes().getOrNull(producedValueName) != null) {
                    throw new RuntimeException(
                            "Query [" + query + "] is producing value [" + producedValueName +
                                    "] that was added by other query or saved as attribute");
                }
            }
            queryAttributes.put(new Attribute(queryName), createdAttributes);
        }
        applyQueryRunChanges(zmi, results);
        zmi.getAttributes().add(queryName, q);
    }

    private synchronized void uninstallQueryInZone(ZMI z, String queryName) {
        z.getAttributes().remove(queryName);
        for (Attribute attr :  queryAttributes.get(queryName)) {
            z.getAttributes().remove(attr);
        }
        queryAttributes.remove(queryName);
    }

    public synchronized void installQuery(String name, String query) throws RemoteException, Exception {
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
            t.run();
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
                System.out.println("Updating");
                for (Map.Entry<PathName, ZMI> zone: agent.zones.entrySet()) {
                    ZMI zmi = zone.getValue();
                    for (Map.Entry<Attribute, Value >attribute : zmi.getAttributes()) {
                        if (!Attribute.isQuery(attribute.getKey()))
                            continue;
                        Attribute queryName = attribute.getKey();
                        ValueString query = (ValueString)attribute.getValue();
                        try {
                            agent.runQueryInZone(zmi, query.getValue());
                        }
                        catch (Exception ex) {
                            System.err.println("Exception in updater:");
                            System.err.println(ex);
                        }
                    }
                }
                try {
                    sleep(10 * 1000); // 10s sleep
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }
    }
}
