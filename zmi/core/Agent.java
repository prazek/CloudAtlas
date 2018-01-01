package core;

import com.google.common.collect.SortedSetMultimap;
import interpreter.Interpreter;
import interpreter.QueryResult;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import model.*;
import sun.nio.ch.Net;

import javax.xml.crypto.Data;
import java.io.IOException;

import java.text.ParseException;
import java.util.*;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.exit;
import static java.lang.Thread.sleep;

public class Agent {

    private PathName pathName;
    // This map stores which attributes are created by running one query.


    Agent(PathName pathName) {
        this.pathName = pathName;
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

        @Override
        public void installQuery(Model.Query request, StreamObserver<Model.Empty> responseObserver) {
            dbStub.installQuery(request, responseObserver);
        }

        @Override
        public void uninstallQuery(Model.QueryName request, StreamObserver<Model.Empty> responseObserver) {
            dbStub.uninstallQuery(request, responseObserver);
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
        Server timerServer = InProcessServerBuilder.forName("timer_module").addService(timerService).build();
        timerServer.start();
        ManagedChannel timerChannel = InProcessChannelBuilder.forName("timer_module").build();
        TimerGrpc.TimerStub timerStub = TimerGrpc.newStub(timerChannel);

        Network network = new Network();
        Network.NetworkService networkService = network.new NetworkService();
        //networkService.startQueryRunner();
        Server networkServer = InProcessServerBuilder.forName("network_module").addService(networkService).build();
        networkServer.start();
        ManagedChannel networkChannel = InProcessChannelBuilder.forName("network_module").build();
        NetworkGrpc.NetworkStub networkStub = NetworkGrpc.newStub(networkChannel);


        DatabaseService dbService = new DatabaseService(this, timerStub, networkStub);
        dbService.startQueryRunner();
        Server dbServer = InProcessServerBuilder.forName("db_module").addService(dbService).build();
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
        } catch (Exception e) {
            System.err.println("Agent exception:");
            e.printStackTrace();
        }
    }




}
