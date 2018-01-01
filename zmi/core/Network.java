package core;


import model.ZMI;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.Timestamp;
import java.util.Arrays;
import java.util.Map;

import static java.lang.Thread.sleep;

public class Network extends Executor {

    private DatabaseUpdater databaseUpdater = null;
    private DatagramSocket receivingSocket;

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

    core.Gossip.GossipMessageUDP receive() throws IOException {
        byte buf[] = new byte[2137];
        DatagramPacket packet
                = new DatagramPacket(buf, buf.length);
        receivingSocket.receive(packet);

        byte buf2[] = Arrays.copyOf(buf, packet.getLength());
        return core.Gossip.GossipMessageUDP.parseFrom(buf2);
    }


    public void sendMsg(core.Gossip.GossipMessageUDP gossip, InetAddress address) throws IOException {
        byte bytes[] = gossip.toByteArray();
        DatagramPacket packet
                = new DatagramPacket(bytes, bytes.length, address, 2137);
        receivingSocket.send(packet);
    }


    class Receiver implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    core.Gossip.GossipMessageUDP gossip = receive();
                    System.out.println("Got gossip: "+ gossip.toString());
                    handleMessage(gossip);

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

        void handleMessage(core.Gossip.GossipMessageUDP message) {
            if (message.hasGossipRequestUDP()) {

            }
            else if (message.hasGossipResponseUDP()) {

            }
            else {
                System.err.println("Invalid message" + message.toString());
            }
        }
        //TODO wtf is rtd
        long rtd(long requestTimestamp, long requestReceivedTimestamp,
                       long responseTimestamp, long responseReceivedTimestamp) {
            return (responseReceivedTimestamp - requestTimestamp)
                    + (responseTimestamp - requestReceivedTimestamp);
        }

        long timeDelta(long responseTimestamp, long rtd, long responseReceivedTimestamp) {
            return responseTimestamp + (long)(0.5*rtd) - responseReceivedTimestamp;
        }


        void updateFreshnessMap( Map<String, Long> freshness, long timeDelta) {
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

        void handleGossipRequest(core.Gossip.GossipRequestUDP request) {
            // ASK database for data
        }
    }







}
