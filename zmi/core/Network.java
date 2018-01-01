package core;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

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

    core.GossipOuterClass.Gossip receive() throws IOException {
        byte buf[] = new byte[2137];
        DatagramPacket packet
                = new DatagramPacket(buf, buf.length);
        receivingSocket.receive(packet);
        System.err.println(packet.getData().length);
        System.err.println(packet.getLength());
        byte buf2[] = Arrays.copyOf(buf, packet.getLength());
        return core.GossipOuterClass.Gossip.parseFrom(buf2);
    }


    public void sendMsg(core.GossipOuterClass.Gossip gossip, InetAddress address) throws IOException {

        byte bytes[] = gossip.toByteArray();
        System.out.println(bytes.length);
        System.out.println(gossip.toString());
        System.out.println(gossip.getPacketID());

        DatagramPacket packet
                = new DatagramPacket(bytes, bytes.length, address, 2137);
        receivingSocket.send(packet);
        System.out.println(bytes.length);
        GossipOuterClass.Gossip.parseFrom(bytes);
    }


    class Receiver implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    core.GossipOuterClass.Gossip gossip = receive();
                    System.out.println("Got gossip: "+ gossip.toString());
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
    }







}
