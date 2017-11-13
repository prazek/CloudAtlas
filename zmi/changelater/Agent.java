package changelater;

import model.ZMI;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;

import static java.lang.System.exit;

public class Agent implements AgentIface {
    private MachineStats machineStats;

    public HashSet<ZMI> zones() throws RemoteException {
        return null;
    }
    public ZMI zone(String zoneName) throws RemoteException {
        return null;
    }
    public void installQuery(String query) throws RemoteException {

    }
    public void uninstallQuery(String query) throws RemoteException {

    }
    public void setZoneValue(String zoneName) throws RemoteException {

    }


    public void updateMachineStats(MachineStats machineStats) throws RemoteException {
        this.machineStats = machineStats;
        System.out.println(machineStats);

    }


    static public void main(String args[]) {
        if (args.length == 0) {
            System.err.println("Usage: ./agent zone_name");
            exit(1);
        }

        /*if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }*/
        try {
            String zoneName = args[0];
            Agent object = new Agent();
            AgentIface stub =
                    (AgentIface) UnicastRemoteObject.exportObject(object, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(zoneName, stub);
            System.out.println("Agent bound");
        } catch (Exception e) {
            System.err.println("Agent exception:");
            e.printStackTrace();
        }
    }
}
