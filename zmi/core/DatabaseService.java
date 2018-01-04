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
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;

import static java.lang.Thread.sleep;

class DatabaseService extends DatabaseServiceGrpc.DatabaseServiceImplBase {
    private PathName current;
    private NetworkGrpc.NetworkStub networkStub;

    private Map<PathName, ZMI> zones = new HashMap<>();
    private Set<ValueContact> fallbackContacts = new TreeSet<>();
    // TODO remove it
    private Map<Attribute, List<Attribute>> queryAttributes = new HashMap<>();
    private Map<PathName, Map<String, Long>> freshness;
    private Random randomGenerator = new Random();

    ZMI root;

    static private int GOSSIPING_DELAY = 4000;

    DatabaseService(PathName current,
                    NetworkGrpc.NetworkStub networkStub) throws ParseException, UnknownHostException {
        this.current = current;
        this.setRoot(initialTree(current));

        freshness = startupFreshness();
        this.networkStub = networkStub;
        fallbackContacts.add(new ValueContact(new PathName(System.getenv("fallback_contact_path")),
                InetAddress.getByName(System.getenv("fallback_contact"))));
    }

    static ZMI getSonByName(ZMI node, String name) {
        for (ZMI son: node.getSons()) {
            if (son.getAttributes().get("name").equals(name)) {
                return son;
            }
        }
        return null;
    }

    static void attachTreeFromPath(ZMI node, PathName pathName, long level) {
        for (String name: pathName.getComponents()) {
            ZMI son = getSonByName(node, name);
            if (son == null) {
                ZMI newNode = new ZMI(node);
                newNode.getAttributes().add("level", new ValueInt(level));
                newNode.getAttributes().add("name", new ValueString(name));
                node.addSon(newNode);
                node = newNode;
            } else {
                node = son;
            }
            ++level;
        }
    }

    static ZMI initialTree(PathName current) {
        ZMI root = new ZMI();
        ZMI node = root;
        long level = 1;
        root.getAttributes().add("level", new ValueInt(0l));
        root.getAttributes().add("name", new ValueString(null));
        attachTreeFromPath(root, current, level);
        return root;
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
            throwable.printStackTrace();
            System.err.println("Error from NoOpResponseObserver");
        }
        @Override
        public void onCompleted() {
        }
    }

    PathName getPathNameForZone(ZMI zone) {
        for (Map.Entry<PathName, ZMI> entry : zones.entrySet()) {
            if (entry.getValue() == zone)
                return entry.getKey();
        }
        throw new RuntimeException("Unknown zone");
    }


    private PathName chooseSybling(PathName name) {
        PathName parent = name.levelUp();
        List<PathName> validSons = new ArrayList<>();
        for (ZMI son : zones.get(parent).getSons()) {
            if (getPathNameForZone(son).equals(name))
                continue;
            if (son.getAttributes().getOrNull("contacts") != null)
                validSons.add(getPathNameForZone(son));
        }
        if (validSons.isEmpty())
            return null;
        return validSons.get(randomGenerator.nextInt(validSons.size()));
    }

    public void startGossiping() {
        System.err.println("Attempting gossiping");
        StreamObserver<core.TimerOuterClass.TimerResponse> responseObserver = new StreamObserver<TimerOuterClass.TimerResponse>() {
            @Override
            public void onNext(TimerOuterClass.TimerResponse timerResponse) {
                try {
                    NoOpResponseObserver observer = new NoOpResponseObserver();
                    ZoneChoiceStrategy zoneChoiceStrategy = new ZoneChoiceStrategy();

                    PathName choosedZone = chooseSybling(zoneChoiceStrategy.chooseZone(zones, current));

                    Value vcontacts = null;
                    if (choosedZone != null) {
                        vcontacts = zones.get(choosedZone).getAttributes().getOrNull("contacts");
                        System.out.println("Choosing [" + choosedZone + "] for gossiping");
                    }

                    ArrayList<Value> contacts = new ArrayList<>();
                    if (vcontacts != null) {
                        contacts.addAll(((ValueSet)vcontacts).getValue());
                    }

                    if (contacts.isEmpty()) {
                        System.out.println("Trying fallback contact");
                        if (fallbackContacts.isEmpty()) {
                              System.err.println("No available contacts in zone and fallback contacts not set");
                              return;
                        }
                        contacts.addAll(fallbackContacts);
                    }

                    int index = randomGenerator.nextInt(contacts.size());

                    ValueContact contact = (ValueContact)contacts.get(index);
                    //ValueContact contact = new ValueContact(choosedZone, InetAddress.getByName("192.168.1.116"));
                    networkStub.requestGossip(
                            Gossip.GossipingRequestFromDB.newBuilder()
                                    .setContact(contact.serialize()).build(), observer);

                } catch (Exception ex) {
                    System.err.println("Gossiping on next error: " + ex);
                    //throw new RuntimeException(ex.getMessage());
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
        System.out.println("DB: request to timer sent");
    }

    @Override
    public void getZones(Model.Empty request, StreamObserver<Model.Zone> responseObserver) {
        try {
            for (Map.Entry<PathName, ZMI> i: zones().entrySet()) {
                // mild schizophrenia
                Model.ZMI zmi = i.getValue().serialize();
                // TODO(sbarzowski) set sons
                Model.Zone z = Model.Zone.newBuilder().setPath(i.getKey().serialize()).setZmi(zmi).build();
                //System.err.println(z.toString());
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

    private void receiveGossipForZone(Database.DatabaseState dbState) throws Exception {
        Map<String, Long> gossipFreshness = dbState.getFreshnessMap();
        PathName updatingZMIName = new PathName(dbState.getZmiPathName());
        Map<String, Long> databaseFresshness = freshness.getOrDefault(updatingZMIName, new HashMap<>());
        AttributesMap attrs = AttributesMap.fromProtobuf(dbState.getAttributesMap());

        for (Map.Entry<Attribute, Value> e: attrs) {
            Long currentFreshness = databaseFresshness.get(e.getKey().getName());
            Long newFreshness = gossipFreshness.get(e.getKey().getName());
            if (currentFreshness == null || (newFreshness > currentFreshness )) {
                System.out.println("Fresher data from gossip [" + e.getKey() + ":" + e.getValue() + "] in zone " + dbState.getZmiPathName());
                if (e.getKey().getName().startsWith("&")) {
                    if (e.getValue().isNull()) {
                        uninstallQueryInZone(zones.get(updatingZMIName), databaseFresshness, e.getKey().getName());
                    } else if (e.getValue() instanceof ValueString) {
                        installQueryInZone(zones.get(updatingZMIName), databaseFresshness, e.getKey(), ((ValueString) e.getValue()).getValue());
                    } else {
                        throw new IllegalArgumentException("Bad type");
                    }
                } else {
                    zones.get(updatingZMIName).getAttributes().addOrChange(e);
                }
                databaseFresshness.put(e.getKey().getName(), newFreshness);
            }
        }
        freshness.put(updatingZMIName, databaseFresshness);
    }

    @Override
    public void receiveGossip(Database.UpdateDatabase request, StreamObserver<Model.Empty> responseObserver) {
        try {
            System.err.println("db: received Gossip!");
            for (Database.DatabaseState dbState: request.getDatabaseStateList()) {
                attachTreeFromPath(root, new PathName(dbState.getZmiPathName()), 1);
            }
            addZMI(root, null);
            for (Database.DatabaseState dbState: request.getDatabaseStateList()) {
                receiveGossipForZone(dbState);
            }
            System.err.println("db: gossip applied!");
            responseObserver.onNext(Model.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCurrentDatabase(Database.CurrentDatabaseRequest request, StreamObserver<Database.UpdateDatabase> responseObserver) {
        Database.UpdateDatabase.Builder dbBuilder = Database.UpdateDatabase.newBuilder();

        for (Map.Entry<PathName, ZMI> zone: zones.entrySet()) {
            PathName pathName = zone.getKey();
            Model.AttributesMap attrMap = zones.get(pathName).getAttributes().serialize();
            Map<String, Long> zoneFreshness = freshness.getOrDefault(pathName, new HashMap<>());

            Database.DatabaseState state = Database.DatabaseState.newBuilder()
                    .setAttributesMap(attrMap)
                    .putAllFreshness(zoneFreshness)
                    .setZmiPathName(pathName.getName())
                    .build();
            dbBuilder.addDatabaseState(state);
        }
        responseObserver.onNext(dbBuilder.build());
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
        root = zmi;
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
            // Don't install query in a leaf node.
            if (!zone.getValue().getSons().isEmpty())
                installQueryInZone(zone.getValue(), freshness.get(zone.getKey()), new Attribute(queryCertificate), query);
        }
    }

    public synchronized void uninstallQuery(String queryCertificate)  {
        if (!queryCertificate.startsWith("&"))
            throw new RuntimeException("name must start with &");
        for (Map.Entry<PathName, ZMI> zone: this.zones.entrySet()) {
            if (!zone.getValue().getSons().isEmpty())
                uninstallQueryInZone(zone.getValue(), freshness.get(zone.getKey()), queryCertificate);
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

    private void applyQueryRunChanges(ZMI zmi, Map<String, Long> freshness, List<QueryResult> results) {
        for (QueryResult r : results) {
            freshness.put(r.getName().getName(), System.currentTimeMillis());
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

    private synchronized void installQueryInZone(ZMI zmi, Map<String, Long> freshness, Attribute queryCertificate, String query) throws Exception {
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
            freshness.put(queryCertificate.getName(), System.currentTimeMillis());
        }
        applyQueryRunChanges(zmi, freshness, results);
        zmi.getAttributes().add(queryCertificate, q);
    }

    private synchronized void uninstallQueryInZone(ZMI z, Map<String, Long> freshness, String queryName) {
        z.getAttributes().addOrChange(queryName, new ValueNull());
        freshness.put(queryName, System.currentTimeMillis());
        for (Attribute attr :  queryAttributes.get(new Attribute(queryName))) {
            z.getAttributes().addOrChange(attr, new ValueNull());
            freshness.put(attr.getName(), System.currentTimeMillis());
        }
    }

    private Map<PathName, Map<String, Long>> startupFreshness() {
        Map<PathName, Map<String, Long>> result = new HashMap<>();
        Long currentTimestamp = System.currentTimeMillis();
        for (Map.Entry<PathName, ZMI> zone : zones.entrySet()) {
            Map<String, Long> zoneFreshness = new HashMap<>();
            for (Map.Entry<Attribute, Value> attribute : zone.getValue().getAttributes()) {
                zoneFreshness.put(attribute.getKey().getName(), currentTimestamp);
            }
            result.put(zone.getKey(), zoneFreshness);
        }
        return result;
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
                            applyQueryRunChanges(zmi, freshness.get(zone.getKey()), results);
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
