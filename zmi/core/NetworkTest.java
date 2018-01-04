package core;

import sun.awt.ConstrainableGraphics;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static java.lang.Thread.sleep;

public class NetworkTest {

    static class SenderTest implements Runnable {

        @Override
        public void run() {
            Network network = new Network();
            int id = 42;
            try {
                InetAddress addr = InetAddress.getByName("127.0.0.1");
                while (true) {
                    try {
                        core.Gossip.GossipResponseUDP gossip =
                                core.Gossip.GossipResponseUDP.newBuilder().setResponseTimestamp(id++).build();

                        network.sendMsg(core.Gossip.GossipMessageUDP.newBuilder().
                                setGossipResponseUDP(gossip).build(), new InetSocketAddress(addr, Config.getGlobalNetworkServicePort()));

                    } catch (IOException ex) {
                        System.err.println("IO exception: " + ex);
                    }
                    try {
                        sleep(3 * 1000);
                    } catch (InterruptedException ie) {
                        return;
                    }
                }
            }
            catch (UnknownHostException ex) {}
        }

    }


    static public void main(String args[]) {
        SenderTest senderTest = new SenderTest();
        Thread t = new Thread(senderTest);
        t.run();
    }

}
