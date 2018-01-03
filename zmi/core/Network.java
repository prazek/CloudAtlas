package core;


import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import model.ValueContact;
import model.ZMI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.security.Timestamp;
import java.util.*;

import static java.lang.Thread.sleep;

public class Network {

    private DatagramSocket receivingSocket;
    DatabaseServiceGrpc.DatabaseServiceStub databaseStub;
    public static final int GLOBAL_NETWORK_SERVICE_PORT = 2222;
    static final int FRAGMENTATION_SIZE = 10000;
    static final int MAX_SUBPACKETS = 1000;

    Random random = new Random();

    Map<Long, MessageStitcher> stichers = new HashMap<>();

    Network() {
        try {
            // TODO make it configurable
            receivingSocket = new DatagramSocket(GLOBAL_NETWORK_SERVICE_PORT);
        } catch (SocketException ex) {
            System.err.println("Socket exception: " + ex);
            throw new RuntimeException("Failed to set up a socket");
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

    private class MessageStitcher {
        private long id;
        byte messages[][];
        int receivedCount;
        int totalCount;

        SocketAddress senderAddress;

        public MessageStitcher(Gossip.Subpacket initial, SocketAddress senderAddress) {
            this.senderAddress = senderAddress;
            totalCount = initial.getSubpacketCount();
            receivedCount = 0;
            id = initial.getPacketId();
            messages = new byte[totalCount][];
            onNewSubpacket(initial);
        }

        public void onNewSubpacket(Gossip.Subpacket subpacket) {
            if (messages[subpacket.getSequenceNr()] == null) {
                messages[subpacket.getSequenceNr()] = subpacket.getData().toByteArray();
                ++receivedCount;
            }
        }

        public boolean ready() {
            return receivedCount == totalCount;
        }

        public Message stitchedMessage() {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (int i = 0; i < totalCount; ++i) {
                    outputStream.write(messages[i]);
                }
                return new Message(core.Gossip.GossipMessageUDP.parseFrom(outputStream.toByteArray()), senderAddress);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to stitch message", ex);
            }
        }
    };

    Message stitchMessage(Gossip.Subpacket subpacket, SocketAddress senderAddress) {
        System.err.println("Stitching...");
        System.err.println(subpacket.getPacketId() + " " + subpacket.getSequenceNr());
        MessageStitcher sticher = stichers.get(subpacket.getPacketId());
        if (sticher == null) {
            sticher = new MessageStitcher(subpacket, senderAddress);
            stichers.put(sticher.id, sticher);
        } else {
            sticher.onNewSubpacket(subpacket);
        }
        if (sticher.ready()) {
            return sticher.stitchedMessage();
        }
        return null;
    }

    Message receive() throws IOException {
        byte buf[] = new byte[2137*2];
        DatagramPacket packet
                = new DatagramPacket(buf, buf.length);

        System.err.println("Waiting for UDP packet");

        receivingSocket.receive(packet);

        System.err.println("UDP packet received");

        byte buf2[] = Arrays.copyOf(buf, packet.getLength());

        Gossip.Subpacket subpacket = Gossip.Subpacket.parseFrom(buf2);

        return stitchMessage(subpacket, packet.getSocketAddress());
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
                    if (message != null) {
                        System.out.println("Got message from " + message.senderAddress.toString());
                        handleMessage(message);
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
