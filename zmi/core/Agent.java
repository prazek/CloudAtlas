package core;


import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import model.*;

import java.io.IOException;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.text.ParseException;
import java.util.concurrent.Executors;

import static java.lang.System.exit;
public class Agent {

    private PathName pathName;
    // This map stores which attributes are created by running one query.
    private Signature verifier;
    private PublicKey publicKey;

    Agent(PathName pathName, PublicKey publicKey) throws NoSuchAlgorithmException {
        this.pathName = pathName;
        this.verifier = Signature.getInstance("SHA256withRSA");
        this.publicKey = publicKey;
    }


    private class AgentService extends AgentGrpc.AgentImplBase {
        DatabaseServiceGrpc.DatabaseServiceStub dbStub;

        public AgentService(DatabaseServiceGrpc.DatabaseServiceStub dbStub) {
            this.dbStub = dbStub;
        }

        @Override
        public void getZones(Model.Empty request, StreamObserver<Model.Zone> responseObserver) {
            dbStub.getZones(request, responseObserver);
        }

        boolean verify(byte[] queryBytes, byte[] signature) {
            try {
                verifier.initVerify(publicKey);
                verifier.update(queryBytes);

                System.out.println(signature);
                System.out.println(queryBytes);
                return verifier.verify(signature);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }

        @Override
        public void installQuery(SignerOuterClass.SignedQuery request, StreamObserver<Model.Empty> responseObserver) {
                if (verify(request.getQuery().toByteArray(), request.getSignedQueryBytes().toByteArray())) {
                    System.out.println("Verification successfull; installing query");
                    dbStub.installQuery(request.getQuery(), responseObserver);
                } else {
                    System.err.println("Verification failed; Can't install query");
                }
        }

        @Override
        public void uninstallQuery(SignerOuterClass.SignedUninstallQuery request, StreamObserver<Model.Empty> responseObserver) {
            if (verify(request.getName().toByteArray(), request.getSignedNameBytes().toByteArray()))
                dbStub.uninstallQuery(request.getName(), responseObserver);
            else
                System.err.println("Uninstall verification failed");
        }


        @Override
        public void setZoneValue(Database.SetZoneValueData request, StreamObserver<Model.Empty> responseObserver) {
            dbStub.setZoneValue(request, responseObserver);
        }


        @Override
        public void setFallbackContacts(Database.ValueContacts request, StreamObserver<Model.Empty> responseObserver) {
            dbStub.setFallbackContacts(request, responseObserver);
        }

        @Override
        public void getFallbackContacts(Model.Empty request, StreamObserver<Model.ValueContact> responseObserver) {
            dbStub.getFallbackContacts(request, responseObserver);
        }

        @Override
        public void getQueries(Model.Empty request, StreamObserver<Model.AttributesMap> responseObserver) {
            dbStub.getQueries(request, responseObserver);
        }

        @Override
        public void getZone(Model.PathName request,
                            io.grpc.stub.StreamObserver<Model.Zone> responseObserver) {
            dbStub.getZone(request, responseObserver);
        }


    }

    private void startServer() throws IOException, ParseException {
        TimerService timerService = new TimerService();
        timerService.startQueue();
        Server timerServer = InProcessServerBuilder.forName("timer_module").executor(Executors.newFixedThreadPool(1)).addService(timerService).build();
        //Server timerServer = ServerBuilder.forPort(9999).addService(timerService).build();
        timerServer.start();


        Network network = new Network();
        Network.NetworkService networkService = network.new NetworkService();
        //networkService.startQueryRunner();
        Server networkServer = InProcessServerBuilder.forName("network_module").executor(Executors.newFixedThreadPool(1)).addService(networkService).build();
        networkServer.start();
        ManagedChannel networkChannel = InProcessChannelBuilder.forName("network_module").directExecutor().build();
        NetworkGrpc.NetworkStub networkStub = NetworkGrpc.newStub(networkChannel);


        DatabaseService dbService = new DatabaseService(pathName, networkStub);
        dbService.startQueryRunner();
        Server dbServer = InProcessServerBuilder.forName("db_module").executor(Executors.newFixedThreadPool(1)).addService(dbService).build();
        dbServer.start();

        ManagedChannel dbChannel = InProcessChannelBuilder.forName("db_module").directExecutor().build();
        DatabaseServiceGrpc.DatabaseServiceStub dbStub = DatabaseServiceGrpc.newStub(dbChannel);

        network.setDatabaseStub(dbStub);

        int port = 4321;
        ServerBuilder serverBuilder = ServerBuilder.forPort(port).executor(Executors.newFixedThreadPool(1));
        serverBuilder.addService(new AgentService(dbStub));
        Server server = serverBuilder.build();
        server.start();
        System.err.println("Server started, listening on " + port);

        dbStub.startGossiping(Model.Empty.newBuilder().build(), new DatabaseService.NoOpResponseObserver());
    }

    static public void main(String args[]) {
        if (args.length != 2) {
            System.err.println("Usage: ./agent zone_name signer_public_key");
            exit(1);
        }

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            String zoneName = args[0];
            PublicKey publicKey = QuerySignerService.PublicKeyReader.get(args[1]);
            Agent agent = new Agent(new PathName(zoneName), publicKey);
            agent.startServer();
        } catch (Exception e) {
            System.err.println("Agent exception:");
            e.printStackTrace();
        }
    }




}
