package core;

import interpreter.Interpreter;
import interpreter.QueryResult;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.StreamObserver;
import model.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;

import static java.lang.Thread.sleep;

class DatabaseService extends DatabaseServiceGrpc.DatabaseServiceImplBase {
    private PathName current;
    private NetworkGrpc.NetworkStub networkStub;

    private Map<PathName, ZMI> zones = new HashMap<>();
    private Set<ValueContact> fallbackContacts = new HashSet<>();
    // TODO remove it
    private Map<Attribute, List<Attribute>> queryAttributes = new HashMap<>();
    private Map<PathName, Map<String, Long>> freshness = new HashMap<>();

    static private int GOSSIPING_DELAY = 4000;

    DatabaseService(PathName current,
                    NetworkGrpc.NetworkStub networkStub) throws ParseException, UnknownHostException {
        this.current = current;
        this.setRoot(ZMIConfig.getZMIConfiguration());
        this.networkStub = networkStub;
    }

    public void startQueryRunner() {
        RunQueries queryRunner = new RunQueries();
        Thread t = new Thread(queryRunner);
        t.start();
    }

    public static class NoOpResponseObserver implements StreamObserver<Model.Empty> {
        @Override
        public void onNext(Model.Empty empty) {
        }
        @Override
        public void onError(Throwable throwable) {
            System.err.println("Error from NoOpResponseObserver");
        }
        @Override
        public void onCompleted() {
        }
    }


    public void startGossiping() {
        System.err.println("Attempting gossiping");
        StreamObserver<core.TimerOuterClass.TimerResponse> responseObserver = new StreamObserver<TimerOuterClass.TimerResponse>() {
            @Override
            public void onNext(TimerOuterClass.TimerResponse timerResponse) {

                System.err.println("gossiping onNext");
                try {
                    NoOpResponseObserver observer = new NoOpResponseObserver();
                    ZoneChoiceStrategy zoneChoiceStrategy = new ZoneChoiceStrategy();
                    zoneChoiceStrategy.chooseZone(zones, current);

                    // TODO dupa
                    ValueContact contact = new ValueContact(new PathName("/dupa"), InetAddress.getLocalHost());
                    networkStub.requestGossip(
                            Gossip.GossipingRequestFromDB.newBuilder().setContact(contact.serialize()).build(), observer);

                } catch (Exception ex) {
                    System.err.println("gossiping onNext error");
                    System.err.println(ex);
                    throw new RuntimeException(ex.getMessage());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("Gossiping error: " + throwable);
            }

            @Override
            public void onCompleted() {
                System.err.println("Gossiping completed");
                try {
                    startGossiping();
                } catch (Exception e) {
                    System.err.println("error in onCompleted");
                    System.err.println(e);
                    throw e;
                }

            }
        };
        ManagedChannel timerChannel = InProcessChannelBuilder.forName("timer_module").directExecutor().build();
        //ManagedChannel timerChannel = ManagedChannelBuilder.forAddress("127.0.0.1", 9999).usePlaintext(true).build();
        TimerGrpc.TimerStub timerStub = TimerGrpc.newStub(timerChannel);
        Context fork = Context.current().fork();
        Context previous = fork.attach();
        try {
            timerStub.set(TimerOuterClass.TimerRequest.newBuilder().setDelay(GOSSIPING_DELAY).build(), responseObserver);
        } finally {
            fork.detach(previous);
        }
        System.err.println("DB: request to timer sent");
    }

    @Override
    public void getZones(Model.Empty request, StreamObserver<Model.Zone> responseObserver) {
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
    public void installQuery(Model.Query request, StreamObserver<Model.Empty> responseObserver) {
        try {
            System.err.println("db: install query");
            installQuery(request.getName().getS(), request.getCode());
            responseObserver.onNext(Model.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception r) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(r.getMessage())
                    .augmentDescription("customException()")
                    .withCause(r) // This can be attached to the Status locally, but NOT transmitted to the client! ???
                    .asRuntimeException());
        }
    }

    @Override
    public void uninstallQuery(Model.QueryName request, StreamObserver<Model.Empty> responseObserver) {
        try {
            uninstallQuery(request.getS());
            responseObserver.onNext(Model.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception r) {
            responseObserver.onError(r);
        }
    }

    @Override
    public void setFallbackContacts(Database.ValueContacts request, StreamObserver<Model.Empty> responseObserver) {
        try {
            HashSet<ValueContact> contacts = new HashSet<>();
            for (Model.ValueContact c: request.getContactsList()) {
                contacts.add(ValueContact.fromProtobuf(c));
            }
            setFallbackContacts(contacts);
            responseObserver.onNext(Model.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception r) {
            System.err.println(r);
            // TODO nicer message
            responseObserver.onError(Status.INTERNAL
                    .withDescription(r.getMessage())
                    .augmentDescription("customException()")
                    .withCause(r) // This can be attached to the Status locally, but NOT transmitted to the client!
                    .asRuntimeException());
        }
    }

    @Override
    public void getFallbackContacts(Model.Empty request, StreamObserver<Model.ValueContact> responseObserver) {
        try {
            Set<ValueContact> contacts = getFallbackContacts();
            for (ValueContact c: contacts) {
                responseObserver.onNext(c.serialize());
            }
            responseObserver.onCompleted();
        } catch (Exception r) {
            responseObserver.onError(r);
        }
    }

    @Override
    public void getQueries(Model.Empty request, StreamObserver<Model.AttributesMap> responseObserver) {
        try {
            AttributesMap queries = getQueries();
            responseObserver.onNext(queries.serialize());
            responseObserver.onCompleted();
        } catch (Exception r) {
            responseObserver.onError(r);
        }
    }


    @Override
    public void setZoneValue(Database.SetZoneValueData request, StreamObserver<Model.Empty> responseObserver) {
        try {
            setZoneValue(
                    PathName.fromProtobuf(request.getPath()),
                    new Attribute(request.getAttribute()),
                    Value.fromProtobuf(request.getValue()));
            responseObserver.onNext(Model.Empty.newBuilder().build());
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

    @Override
    public void receiveGossip(Database.UpdateDatabase request, StreamObserver<Model.Empty> responseObserver) {
        Map<String, Long> map = request.getFreshnessMap();
        AttributesMap attrs = AttributesMap.fromProtobuf(request.getAttributesMap());
        for (Map.Entry<Attribute, Value> e: attrs) {
            // TODO freshness comparison
            zones.get(new PathName("/uw/violet07" /* TODO */)).getAttributes().addOrChange(e);
        }
        responseObserver.onNext(Model.Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void getCurrentDatabase(Database.CurrentDatabaseRequest request, StreamObserver<Database.UpdateDatabase> responseObserver) {
        // TODO choose pathname
        PathName pathName = new PathName("/uw/violet07");
        Model.AttributesMap attrMap = zones.get(pathName).getAttributes().serialize();
        Map<String, Long> zoneFreshness = freshness.getOrDefault(pathName, new HashMap<>());
        Database.UpdateDatabase db = Database.UpdateDatabase.newBuilder()
                .setAttributesMap(attrMap)
                .putAllFreshness(zoneFreshness)
                .build();
        responseObserver.onNext(db);
        responseObserver.onCompleted();
    }

    @Override
    public void startGossiping(Model.Empty request,
                               io.grpc.stub.StreamObserver<Model.Empty> responseObserver) {
        responseObserver.onNext(request);
        responseObserver.onCompleted();
        startGossiping();
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
    public Map<PathName, ZMI> zones() {
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

        freshness.putIfAbsent(zoneName, new HashMap<>());
        freshness.get(zoneName).put(valueName.getName(), System.currentTimeMillis());
    }

    public synchronized void setFallbackContacts(Set<ValueContact> fallbackContacts) {
        this.fallbackContacts = fallbackContacts;
    }

    public synchronized Set<ValueContact> getFallbackContacts() {
        return fallbackContacts;
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

    public class RunQueries implements Runnable {
        public void run() {
            System.out.println("Updater running...");
            while (true) {
                System.out.println("Updating");
                for (Map.Entry<PathName, ZMI> zone: zones.entrySet()) {
                    ZMI zmi = zone.getValue();
                    for (Map.Entry<Attribute, Value >attribute : zmi.getAttributes()) {
                        if (!Attribute.isQuery(attribute.getKey()))
                            continue;
                        Attribute queryName = attribute.getKey();
                        ValueString query = (ValueString)attribute.getValue();
                        try {
                            List<QueryResult> results = runQueryInZone(zmi, query.getValue());
                            applyQueryRunChanges(zmi, results);
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
