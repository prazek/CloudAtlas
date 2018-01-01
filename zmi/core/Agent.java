package core;

import interpreter.Interpreter;
import interpreter.QueryResult;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import model.*;

import javax.xml.crypto.Data;
import java.io.IOException;

import java.util.*;

import static java.lang.System.exit;
import static java.lang.Thread.sleep;

public class Agent {
    private HashMap<PathName, ZMI> zones = new HashMap<>();
    private PathName pathName;
    // This map stores which attributes are created by running one query.
    private HashMap<Attribute, List<Attribute>> queryAttributes = new HashMap<>();

    private Set<ValueContact> fallbackContacts = new HashSet<>();

    Agent(PathName pathName) throws Exception {
        this.pathName = pathName;
        this.setRoot(ZMIConfig.getZMIConfiguration());

    }

    private void setRoot(ZMI zmi) {
        zones.clear();
        addZMI(zmi, null);
    }

    private synchronized void addZMI(ZMI zmi, PathName parentName) {
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


    public HashMap<PathName, ZMI> zones() {
        return this.zones;
    }

    public synchronized ZMI zone(PathName zoneName) {
        ZMI zmi = zones().get(zoneName);
        return zmi;
    }

    public synchronized void installQuery(String queryCertificate, String query) throws Exception {
        if (!queryCertificate.startsWith("&"))
            throw new RuntimeException("name must start with &");
        for (Map.Entry<PathName, ZMI> zone: this.zones.entrySet()) {
            // Dont install query in leaf node.
            if (!zone.getValue().getSons().isEmpty())
                installQueryInZone(zone.getValue(), new Attribute(queryCertificate), query);
        }
    }

    public synchronized void uninstallQuery(String queryCertificate)  {
        if (!queryCertificate.startsWith("&"))
            throw new RuntimeException("name must start with &");
        for (Map.Entry<PathName, ZMI> zone: this.zones.entrySet()) {
            if (!zone.getValue().getSons().isEmpty())
                uninstallQueryInZone(zone.getValue(), queryCertificate);
        }
        queryAttributes.remove(queryCertificate);
    }

    public synchronized AttributesMap getQueries()  {
        for (Map.Entry<PathName, ZMI> zone : this.zones.entrySet()) {
            if (!zone.getValue().getSons().isEmpty())
                return getQueriesForZone(zone.getValue());
        }
        return new AttributesMap();
    }

    public synchronized void setZoneValue(PathName zoneName, Attribute valueName, Value value) {
        System.out.println(zoneName + " " + valueName + " " + value.toString());

        if (!zone(zoneName).getSons().isEmpty())
            throw new RuntimeException("Can't set up attribute for non leaf node");
        zone(zoneName).getAttributes().addOrChange(valueName, value);
    }

    public synchronized void setFallbackContacts(Set<ValueContact> fallbackContacts) {
        this.fallbackContacts = fallbackContacts;
    }

    public synchronized Set<ValueContact> getFallbackContacts() {
        return fallbackContacts;
    }

    private class TimerService extends TimerGrpc.TimerImplBase {
        private class TimerQueue extends Thread {
            SortedMap<Long, Callback> waiting;

            public void run() {
                long timestamp = System.currentTimeMillis();
                try {
                    if (waiting.isEmpty()) {
                        wait();
                    } else {
                        long delay = waiting.firstKey() - timestamp;
                        if (delay < 0) {
                            delay = 0;
                        }
                        wait(delay);
                    }
                } catch (InterruptedException e) {
                    // Do nothing
                }
                while (waiting.firstKey() <= timestamp) {
                    Callback callback = waiting.get(waiting.firstKey());
                    waiting.remove(waiting.firstKey());
                    callback.responseObserver.onNext(TimerOuterClass.TimerResponse.newBuilder().setId(callback.id).build());
                }
            }
        }

        // TODO(sbarzowski) better name
        private class Callback {
            public int id;
            StreamObserver<TimerOuterClass.TimerResponse> responseObserver;
        }

        @Override
        public void set(TimerOuterClass.TimerRequest request, StreamObserver<TimerOuterClass.TimerResponse> responseObserver) {

        }
    }

    private class DatabaseService extends DatabaseServiceGrpc.DatabaseServiceImplBase {
        @Override
        public void getZones(AgentOuterClass.Empty request, StreamObserver<Model.Zone> responseObserver) {
            try {
                for (Map.Entry<PathName, ZMI> i: zones().entrySet()) {
                    // mild schizophrenia
                    Model.ZMI zmi = i.getValue().serialize();
                    // TODO(sbarzowski) set sons
                    Model.Zone z = Model.Zone.newBuilder().setPath(i.getKey().serialize()).setZmi(zmi).build();
                    System.err.println(z.toString());
                    responseObserver.onNext(z);
                }
                responseObserver.onCompleted();
            } catch (Exception r) {
                System.err.println(r);
                responseObserver.onError(r);
            }

        }

        @Override
        public void setZoneValue(AgentOuterClass.SetZoneValueData request, StreamObserver<AgentOuterClass.Empty> responseObserver) {
            try {
                Agent.this.setZoneValue(
                        PathName.fromProtobuf(request.getPath()),
                        new Attribute(request.getAttribute()),
                        Value.fromProtobuf(request.getValue()));
                responseObserver.onNext(AgentOuterClass.Empty.newBuilder().build());
                responseObserver.onCompleted();
            } catch (Exception r) {
                System.err.println(r);
                responseObserver.onError(r);
            }
        }

        @Override
        public void getZone(Model.PathName request,
                            io.grpc.stub.StreamObserver<Model.Zone> responseObserver) {
            try {
                responseObserver.onNext(Model.Zone.newBuilder().build());
                responseObserver.onCompleted();
            } catch (Exception r) {
                responseObserver.onError(r);
            }
        }
    }

    private class AgentService extends AgentGrpc.AgentImplBase {
        DatabaseServiceGrpc.DatabaseServiceStub dbStub;

        public AgentService(DatabaseServiceGrpc.DatabaseServiceStub dbStub) {
            this.dbStub = dbStub;
        }

        @Override
        public void getZones(AgentOuterClass.Empty request, StreamObserver<Model.Zone> responseObserver) {
            dbStub.getZones(request, responseObserver);
        }

        @Override
        public void installQuery(Model.Query request, StreamObserver<AgentOuterClass.Empty> responseObserver) {
            try {
                Agent.this.installQuery(request.getName().getS(), request.getCode());
                responseObserver.onCompleted();
            } catch (Exception r) {
                responseObserver.onError(r);
            }
        }

        @Override
        public void uninstallQuery(Model.QueryName request, StreamObserver<AgentOuterClass.Empty> responseObserver) {
            try {
                Agent.this.uninstallQuery(request.getS());
                responseObserver.onCompleted();
            } catch (Exception r) {
                responseObserver.onError(r);
            }
        }

        @Override
        public void setZoneValue(AgentOuterClass.SetZoneValueData request, StreamObserver<AgentOuterClass.Empty> responseObserver) {
            dbStub.setZoneValue(request, responseObserver);
        }


        @Override
        public void setFallbackContacts(AgentOuterClass.ValueContacts request, StreamObserver<AgentOuterClass.Empty> responseObserver) {
            try {
                HashSet<ValueContact> contacts = new HashSet<>();
                for (Model.ValueContact c: request.getContactsList()) {
                    contacts.add(ValueContact.fromProtobuf(c));
                }
                Agent.this.setFallbackContacts(contacts);
                responseObserver.onCompleted();
            } catch (Exception r) {
                responseObserver.onError(r);
            }
        }

        @Override
        public void getFallbackContacts(AgentOuterClass.Empty request, StreamObserver<Model.ValueContact> responseObserver) {
            try {
                Set<ValueContact> contacts = Agent.this.getFallbackContacts();
                for (ValueContact c: contacts) {
                    responseObserver.onNext(c.serialize());
                }
                responseObserver.onCompleted();
            } catch (Exception r) {
                responseObserver.onError(r);
            }
        }

        @Override
        public void getQueries(AgentOuterClass.Empty request, StreamObserver<Model.AttributesMap> responseObserver) {
            try {
                AttributesMap contacts = Agent.this.getQueries();
                responseObserver.onNext(contacts.serialize());
            } catch (Exception r) {
                responseObserver.onError(r);
            }
        }

        @Override
        public void getZone(Model.PathName request,
                            io.grpc.stub.StreamObserver<Model.Zone> responseObserver) {
            dbStub.getZone(request, responseObserver);
        }
    }

    private void startServer() throws IOException {
        Server dbServer = InProcessServerBuilder.forName("db_module").addService(new DatabaseService()).build();
        dbServer.start();
        ManagedChannel dbChannel = InProcessChannelBuilder.forName("db_module").build();
        DatabaseServiceGrpc.DatabaseServiceStub dbStub = DatabaseServiceGrpc.newStub(dbChannel);

        int port = 4321;
        ServerBuilder serverBuilder = ServerBuilder.forPort(port);
        serverBuilder.addService(new AgentService(dbStub));
        Server server = serverBuilder.build();
        server.start();
        System.err.println("Server started, listening on " + port);
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
            agent.startServer();
            RunQueries queryRunner = new RunQueries(agent);
            Thread t = new Thread(queryRunner);
            t.run();
        } catch (Exception e) {
            System.err.println("Agent exception:");
            e.printStackTrace();
        }
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
    
    private AttributesMap getQueriesForZone(ZMI zone) {
        AttributesMap result = new AttributesMap();
        for (Map.Entry<Attribute, Value> entry :  zone.getAttributes()) {
            if (Attribute.isQuery(entry.getKey()))
                result.add(entry);
        }
        System.err.println(result);
        return result;
    }

    private synchronized void installQueryInZone(ZMI zmi, Attribute queryCertificate, String query) throws Exception {
        System.err.println("Installing query " );
        if (zmi.getSons().isEmpty()) {
            throw new RuntimeException("Installing query on leaf node");
        }
        Value q = new ValueString(query);

        if (zmi.getAttributes().getOrNull(queryCertificate) != null) {
            throw new RuntimeException("Duplicated query of name [" + queryCertificate + "]");
        }

        List<QueryResult> results = runQueryInZone(zmi, query);

        // Put attributes if first run of this query
        if (!queryAttributes.containsKey(queryCertificate)) {
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
            queryAttributes.put(queryCertificate, createdAttributes);
        }
        applyQueryRunChanges(zmi, results);
        zmi.getAttributes().add(queryCertificate, q);
    }

    private synchronized void uninstallQueryInZone(ZMI z, String queryName) {
        z.getAttributes().remove(queryName);
        for (Attribute attr :  queryAttributes.get(new Attribute(queryName))) {
            z.getAttributes().remove(attr);
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
                            List<QueryResult> results = agent.runQueryInZone(zmi, query.getValue());
                            agent.applyQueryRunChanges(zmi, results);
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
