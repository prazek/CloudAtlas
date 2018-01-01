package core;


import model.ValueContact;
import model.ZMI;
import sun.misc.Request;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.Timestamp;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class Network extends Executor {

    private DatabaseUpdater databaseUpdater = null;
    private DatagramSocket receivingSocket;
    private Map<Long, RequestExtraData> requestsExtraData = new HashMap<>();


    Network() {
        try {
            // TODO make it configurable
            receivingSocket = new DatagramSocket(2137);
        } catch (SocketException ex) {
            System.err.println("Socket exception: " + ex);
            assert false;
        }

        Receiver receiver = new Receiver();
        Thread t = new Thread(receiver);
        t.start();
    }


    void setDatabaseUpdater(DatabaseUpdater databaseUpdater) {
        this.databaseUpdater = databaseUpdater;
    }


    @Override
    void execute(ExecuteContext context) {

    }
    class Message {
        Message(core.Gossip.GossipMessageUDP messageUDP, InetAddress senderAddress) {
            this.messageUDP = messageUDP;
            this.senderAddress = senderAddress;
        }
        core.Gossip.GossipMessageUDP messageUDP;
        InetAddress senderAddress;
    }

    Message receive() throws IOException {
        byte buf[] = new byte[2137];
        DatagramPacket packet
                = new DatagramPacket(buf, buf.length);
        receivingSocket.receive(packet);

        byte buf2[] = Arrays.copyOf(buf, packet.getLength());
        return new Message(core.Gossip.GossipMessageUDP.parseFrom(buf2), packet.getAddress());
    }


    public void sendMsg(core.Gossip.GossipMessageUDP gossip, InetAddress address) throws IOException {
        byte bytes[] = gossip.toByteArray();
        DatagramPacket packet
                = new DatagramPacket(bytes, bytes.length, address, 2137);
        receivingSocket.send(packet);
    }


    class RequestExtraData {
        RequestExtraData(long requestTimestamp,
                         long requestReceivedTimestamp,
                         InetAddress address) {
            this.requestTimestamp = requestTimestamp;
            this.requestReceivedTimestamp = requestReceivedTimestamp;
            this.address = address;
        }

        long requestTimestamp;
        long requestReceivedTimestamp;
        InetAddress address;
    }







    private void handleGossipingResponseFromDB(Gossip.GossipingResponseFromDB response) {
        // TODO
        RequestExtraData extraData = requestsExtraData.get(2137L);

        Gossip.GossipResponseUDP responseUDP = Gossip.GossipResponseUDP.newBuilder()
                .setResponseTimestamp(System.currentTimeMillis())
                .setAttributesMap(response.getAttributesMap())
                .putAllFreshness(response.getFreshnessMap())
                .setRequestReceivedTimestamp(extraData.requestReceivedTimestamp)
                .setRequestTimestamp(extraData.requestTimestamp).build();

        try {
            sendMsg(Gossip.GossipMessageUDP.newBuilder().setGossipResponseUDP(responseUDP).build(), extraData.address);
        } catch(IOException ex) {
            // TODO redo 5 times?
        }
    }

    private void handleGossipingRequestFromDB(Gossip.GossipingRequestFromDB request) {
        Gossip.GossipRequestUDP requestUDP = Gossip.GossipRequestUDP.newBuilder()
                .setRequestTimestamp(System.currentTimeMillis()).build();

        ValueContact contact = ValueContact.fromProtobuf(request.getContact());
        // TODO set any data indicating that we are waiting for response?

        try {
            sendMsg(Gossip.GossipMessageUDP.newBuilder().setGossipRequestUDP(requestUDP).build(),
                    contact.getAddress());
        } catch (IOException ex) {
            // TODO handle
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
                handleGossipRequest(message.messageUDP.getGossipRequestUDP(), message.senderAddress);
            }
            else if (message.messageUDP.hasGossipResponseUDP()) {
                handleGossipResponse(message.messageUDP.getGossipResponseUDP());
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


        private void updateFreshnessMap(Map<String, Long> freshness, long timeDelta) {
            for (Map.Entry<String, Long> entry : freshness.entrySet()) {
                entry.setValue(entry.getValue() + timeDelta);
            }
        }

        void handleGossipResponse(core.Gossip.GossipResponseUDP response) {
            long time = System.currentTimeMillis();
            long rtd = rtd(response.getRequestTimestamp(), response.getRequestReceivedTimestamp(),
                    response.getResponseTimestamp(), time);

            long timeDelta = timeDelta(response.getResponseTimestamp(), rtd, time);

            Map<String, Long> freshness = response.getFreshnessMap();
            updateFreshnessMap(freshness, timeDelta);

            Database.UpdateDatabase updateDatabase = Database.UpdateDatabase.newBuilder()
                    .setAttributesMap(response.getAttributesMap())
                    .putAllFreshness(freshness).build();


            // Send updateDatabase to database
        }

        void handleGossipRequest(core.Gossip.GossipRequestUDP request, InetAddress address) {

            // TODO
            int id = 42;
            requestsExtraData.put(42L,
                    new RequestExtraData(request.getRequestTimestamp(), System.currentTimeMillis(), address));


            Database.CurrentDatabaseRequest databaseRequest =
                    Database.CurrentDatabaseRequest.newBuilder().build();
            // ASK database for data


        }
    }







}
