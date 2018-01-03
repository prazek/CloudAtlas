package core;


import io.grpc.stub.StreamObserver;
import model.ValueContact;
import model.ZMI;

import java.io.IOException;
import java.net.*;
import java.security.Timestamp;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class Network {

    private DatagramSocket receivingSocket;
    DatabaseServiceGrpc.DatabaseServiceStub databaseStub;
    int GLOBAL_NETWORK_SERVICE_PORT = 2137;


    Network() {
        try {
            // TODO make it configurable
            receivingSocket = new DatagramSocket(GLOBAL_NETWORK_SERVICE_PORT);
        } catch (SocketException ex) {
            System.err.println("Socket exception: " + ex);
            assert false;
        }

        Receiver receiver = new Receiver();
        Thread t = new Thread(receiver);
        t.start();
    }

    void setDatabaseStub(DatabaseServiceGrpc.DatabaseServiceStub databaseStub) {
        this.databaseStub = databaseStub;

    }

    class Message {
        Message(core.Gossip.GossipMessageUDP messageUDP, SocketAddress senderAddress) {
            this.messageUDP = messageUDP;
            this.senderAddress = senderAddress;
        }
        core.Gossip.GossipMessageUDP messageUDP;
        SocketAddress senderAddress;
    }

    Message receive() throws IOException {
        // TODO fragmentation
        byte buf[] = new byte[2137*2];
        DatagramPacket packet
                = new DatagramPacket(buf, buf.length);
        receivingSocket.receive(packet);

        byte buf2[] = Arrays.copyOf(buf, packet.getLength());

        return new Message(core.Gossip.GossipMessageUDP.parseFrom(buf2), packet.getSocketAddress());
    }


    public void sendMsg(core.Gossip.GossipMessageUDP gossip, SocketAddress address) throws IOException {
        byte bytes[] = gossip.toByteArray();
        DatagramPacket packet
                = new DatagramPacket(bytes, bytes.length, address);
        receivingSocket.send(packet);
    }


    private void handleGossipingResponseFromDB(Database.UpdateDatabase response,
                                               SocketAddress address, long requestTimestamp,
                                               long responseTimestamp, String zmiPathName) {

        Gossip.GossipResponseUDP responseUDP = Gossip.GossipResponseUDP.newBuilder()
                .setResponseTimestamp(System.currentTimeMillis())
                .setAttributesMap(response.getAttributesMap())
                .putAllFreshness(response.getFreshnessMap())
                .setRequestReceivedTimestamp(requestTimestamp)
                .setRequestTimestamp(responseTimestamp)
                .setZmiPathName(zmiPathName).build();

        try {
            sendMsg(Gossip.GossipMessageUDP.newBuilder().setGossipResponseUDP(responseUDP).build(), address);
        } catch(IOException ex) {
            // TODO redo 5 times?
            System.err.println(ex);
        }
    }

    private void handleGossipingRequestFromDB(Gossip.GossipingRequestFromDB request) {
        System.out.println("Handling request of gossiping");
        Gossip.GossipRequestUDP requestUDP = Gossip.GossipRequestUDP.newBuilder()
                .setRequestTimestamp(System.currentTimeMillis())
                .setZmiPathName(request.getContact().getPathName().getP()).build();

        ValueContact contact = ValueContact.fromProtobuf(request.getContact());
        // TODO set any data indicating that we are waiting for response?

        try {
            sendMsg(Gossip.GossipMessageUDP.newBuilder().setGossipRequestUDP(requestUDP).build(),
                    new InetSocketAddress(contact.getAddress(), GLOBAL_NETWORK_SERVICE_PORT));
        } catch (IOException ex) {
            // TODO redo?
            System.err.println(ex);
        }
    }


    class Receiver implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    Message message = receive();
                    System.out.println("Got message from " + message.senderAddress.toString());
                    handleMessage(message);

                    try {
                        sleep(3*1000);
                    } catch (InterruptedException ie) {
                        return;
                    }
                } catch (IOException ex) {
                    System.err.println("Receiver exception: " + ex);
                }
            }
        }

        private void handleMessage(Message message) {
            if (message.messageUDP.hasGossipRequestUDP()) {
                handleGossipRequestFromNetwork(message.messageUDP.getGossipRequestUDP(), message.senderAddress);
            }
            else if (message.messageUDP.hasGossipResponseUDP()) {
                handleGossipResponseFromNetwork(message.messageUDP.getGossipResponseUDP());
            }
            else {
                System.err.println("Invalid message" + message.toString());
            }
        }

        //TODO wtf is rtd
        private long rtd(long requestTimestamp, long requestReceivedTimestamp,
                       long responseTimestamp, long responseReceivedTimestamp) {
            return (responseReceivedTimestamp - requestTimestamp)
                    + (responseTimestamp - requestReceivedTimestamp);
        }

        private long timeDelta(long responseTimestamp, long rtd, long responseReceivedTimestamp) {
            return responseTimestamp + (long)(0.5*rtd) - responseReceivedTimestamp;
        }


        private Map<String, Long> getUpdatedFreshnessMap(Map<String, Long> freshness, long timeDelta) {
            Map<String, Long> updatedFresshnessMap = new HashMap<>();
            for (Map.Entry<String, Long> entry : freshness.entrySet()) {
                updatedFresshnessMap.put(entry.getKey(), entry.getValue() + timeDelta);
            }
            return updatedFresshnessMap;
        }

        void handleGossipResponseFromNetwork(core.Gossip.GossipResponseUDP response) {
            long time = System.currentTimeMillis();
            long rtd = rtd(response.getRequestTimestamp(), response.getRequestReceivedTimestamp(),
                    response.getResponseTimestamp(), time);

            long timeDelta = timeDelta(response.getResponseTimestamp(), rtd, time);

            Map<String, Long> freshness = getUpdatedFreshnessMap(response.getFreshnessMap(), timeDelta);

            Database.UpdateDatabase updateDatabase = Database.UpdateDatabase.newBuilder()
                    .setAttributesMap(response.getAttributesMap())
                    .putAllFreshness(freshness)
                    .setZmiPathName(response.getZmiPathName()).build();

            StreamObserver<Model.Empty> responseObserver = new StreamObserver<Model.Empty>() {
                @Override
                public void onNext(Model.Empty empty) {
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println(throwable);
                }

                @Override
                public void onCompleted() {
                }
            };
            databaseStub.receiveGossip(updateDatabase, responseObserver);
        }

        void handleGossipRequestFromNetwork(core.Gossip.GossipRequestUDP request, SocketAddress address) {
            System.out.println("Handling gossiping request");

            StreamObserver<Database.UpdateDatabase> responseObserver = new StreamObserver<Database.UpdateDatabase>() {
                @Override
                public void onNext(Database.UpdateDatabase updateDatabase) {
                    if (!updateDatabase.getZmiPathName().equals(request.getZmiPathName())) {
                        System.err.println("Invalid request send; ["
                                + updateDatabase.getZmiPathName() + "] != [" + request.getZmiPathName() + "]");
                        return;
                    }

                    try {
                        handleGossipingResponseFromDB(updateDatabase,
                                address, request.getRequestTimestamp(), System.currentTimeMillis(),
                                updateDatabase.getZmiPathName());
                    } catch (Exception e) {
                        System.err.println("onNext CurrentDatabase");
                        System.err.println(e);
                    }

                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println(throwable);
                }

                @Override
                public void onCompleted() {

                }
            };
            databaseStub.getCurrentDatabase(Database.CurrentDatabaseRequest.newBuilder()
                    .setZmiPathName(request.getZmiPathName()).build(), responseObserver);
        }
    }



    public class NetworkService extends NetworkGrpc.NetworkImplBase {

        @Override
        public void requestGossip(Gossip.GossipingRequestFromDB request, StreamObserver<Model.Empty> responseObserver) {
            try {
                handleGossipingRequestFromDB(request);
                responseObserver.onNext(Model.Empty.newBuilder().build());
                responseObserver.onCompleted();
            }
            catch (Exception ex) {
                responseObserver.onError(ex);
            }

        }
    }




}
