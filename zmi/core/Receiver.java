package core;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

class Receiver implements Runnable {

    private NetworkGrpc.NetworkStub networkStub;
    private DatagramSocket receivingSocket;

    Map<Long, MessageStitcher> stichers = new HashMap<>();

    public Receiver(NetworkGrpc.NetworkStub stub, DatagramSocket socket) { networkStub = stub; receivingSocket = socket; }

    private class MessageStitcher {
        private long id;
        byte messages[][];
        int receivedCount;
        int totalCount;
        long timestampCreated;

        InetSocketAddress senderAddress;

        public MessageStitcher(Gossip.Subpacket initial, InetSocketAddress senderAddress) {
            this.senderAddress = senderAddress;
            totalCount = initial.getSubpacketCount();
            receivedCount = 0;
            id = initial.getPacketId();
            messages = new byte[totalCount][];
            onNewSubpacket(initial);
            timestampCreated = System.currentTimeMillis();
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

        public Gossip.GossipReceived stitchedMessage() {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (int i = 0; i < totalCount; ++i) {
                    outputStream.write(messages[i]);
                }
                return Gossip.GossipReceived.newBuilder()
                        .setGossip(core.Gossip.GossipMessageUDP.parseFrom(outputStream.toByteArray()))
                        .setInetAddress(ByteString.copyFrom(senderAddress.getAddress().getAddress()))
                        .setPort(senderAddress.getPort())
                        .build();
            } catch (IOException ex) {
                throw new RuntimeException("Failed to stitch message", ex);
            }
        }

        public boolean tooOld() {
            return System.currentTimeMillis() - timestampCreated > 10000;
        }
    };

    Gossip.GossipReceived stitchMessage(Gossip.Subpacket subpacket, InetSocketAddress senderAddress) {
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

    Gossip.GossipReceived receive() throws IOException {
        byte buf[] = new byte[2137*2];
        DatagramPacket packet
                = new DatagramPacket(buf, buf.length);

        System.err.println("Waiting for UDP packet");

        receivingSocket.receive(packet);

        System.err.println("UDP packet received");

        byte buf2[] = Arrays.copyOf(buf, packet.getLength());

        Gossip.Subpacket subpacket = Gossip.Subpacket.parseFrom(buf2);

        List<Long> toRemove = new ArrayList<>();
        for (Map.Entry<Long, MessageStitcher> e: stichers.entrySet()) {
            if (e.getValue().tooOld())
                toRemove.add(e.getKey());
        }

        for (Long remove : toRemove)
            stichers.remove(remove);

        return stitchMessage(subpacket, new InetSocketAddress(packet.getAddress(), packet.getPort()));
    }

    @Override
    public void run() {
        while (true) {
            try {
                Gossip.GossipReceived message = receive();
                if (message != null) {
                    System.out.println("Got message");
                    networkStub.received(message, new DatabaseService.NoOpResponseObserver());
                }
            } catch (IOException ex) {
                System.err.println("Receiver exception: " + ex);
            }
        }
    }


}
