package core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Network extends Executor {

    private DatabaseUpdater databaseUpdater = null;
    private DatagramSocket socket;

    Network() {
        try {
            // TODO make it configurable
            socket = new DatagramSocket(2137);
        } catch (SocketException ex) {
            System.err.println("Socket exception: " + ex);
            assert false;
        }
    }


    void setDatabaseUpdater(DatabaseUpdater databaseUpdater) {
        this.databaseUpdater = databaseUpdater;
    }


    @Override
    void execute(ExecuteContext context) {

    }

    void receive() throws IOException {
        byte buf[] = new byte[256];
        DatagramPacket packet
                = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
    }


    void sendMsg(byte bytes[]) throws IOException {
        DatagramPacket packet
                = new DatagramPacket(bytes, bytes.length);
        socket.send(packet);
    }
}
