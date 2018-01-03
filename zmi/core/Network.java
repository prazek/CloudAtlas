package core;


import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.StreamObserver;
import model.ValueContact;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.*;

import static java.lang.Thread.sleep;

public class Network {

    private DatagramSocket receivingSocket;
    DatabaseServiceGrpc.DatabaseServiceStub databaseStub;
    public static final int GLOBAL_NETWORK_SERVICE_PORT = 2222;
    static final int FRAGMENTATION_SIZE = 3;
    static final int MAX_SUBPACKETS = 1000;

    Random random = new Random();


    Network() {
        try {
            // TODO make it configurable
            receivingSocket = new DatagramSocket(GLOBAL_NETWORK_SERVICE_PORT);
        } catch (SocketException ex) {
            System.err.println("Socket exception: " + ex);
            throw new RuntimeException("Failed to set up a socket");
        }

        ManagedChannel networkChannel = InProcessChannelBuilder.forName("network_module").directExecutor().build();
        NetworkGrpc.NetworkStub networkStub = NetworkGrpc.newStub(networkChannel);

        Receiver receiver = new Receiver(networkStub, receivingSocket);
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

    List<Gossip.Subpacket> splitPacket(byte message[]) {
        ArrayList<Gossip.Subpacket> subpackets = new ArrayList<>();
        int size = (message.length + FRAGMENTATION_SIZE - 1) / FRAGMENTATION_SIZE;
        if (size > MAX_SUBPACKETS) {
            throw new RuntimeException("Too big packet");
        }
        long packetId = random.nextLong();
        int sequenceNumber = 0;
        for (int cur = 0; cur < message.length;) {
            int len = Integer.min(message.length - cur, FRAGMENTATION_SIZE);
            Gossip.Subpacket subpacket = Gossip.Subpacket.newBuilder()
                    .setData(ByteString.copyFrom(message, cur, len))
                    .setSequenceNr(sequenceNumber)
                    .setPacketId(packetId)
                    .setSubpacketCount(size)
                    .build();
            subpackets.add(subpacket);
            cur += len;
            ++sequenceNumber;
        }
        return subpackets;
    }


    public void sendMsg(core.Gossip.GossipMessageUDP gossip, SocketAddress address) throws IOException {
        byte bytes[] = gossip.toByteArray();

        List<Gossip.Subpacket> packets = splitPacket(bytes);
        for (Gossip.Subpacket subpacket: packets) {
            byte subpacketBytes[] = subpacket.toByteArray();
            DatagramPacket datagramPacket
                    = new DatagramPacket(subpacketBytes, subpacketBytes.length, address);
            receivingSocket.send(datagramPacket);
            System.err.println("UDP packet sent");
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

    void handleGossipResponseFromNetwork(Gossip.GossipResponseUDP response) {
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

    void handleGossipRequestFromNetwork(Gossip.GossipRequestUDP request, SocketAddress address) {
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

        @Override
        public void received(Gossip.GossipReceived request, StreamObserver<Model.Empty> responseObserver) {
            try {
                handleMessage(new Message(request.getGossip(),
                        new InetSocketAddress(InetAddress.getByAddress(request.getInetAddress().toByteArray()), request.getPort())));
            }
            catch (Exception ex) {
                responseObserver.onError(ex);
            }
        }
    }




}
