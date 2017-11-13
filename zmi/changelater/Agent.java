package changelater;

import model.Attribute;
import model.PathName;
import model.Value;
import model.ZMI;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.HashSet;

import static java.lang.System.exit;

public class Agent implements AgentIface {
    ZMI zmi;
    PathName pathName;

    Agent(PathName pathName) {
        this.pathName = pathName;
    }


    public HashSet<ZMI> zones() throws RemoteException {
        return null;
    }
    public ZMI zone(PathName zoneName) throws RemoteException {

        return null;
    }
    public void installQuery(String query) throws RemoteException {

    }
    public void uninstallQuery(String query) throws RemoteException {

    }

    public void setZoneValue(PathName zoneName, Attribute valueName, Value value) throws RemoteException {
        System.out.println(zoneName + " " + valueName + " " + value.toString());

        if (zoneName.equals(pathName)) {
            zmi.getAttributes().addOrChange(valueName, value);

        } else {
            System.err.println("not sure if error?");
        }
    }



    static public void main(String args[]) {
        if (args.length == 0) {
            System.err.println("Usage: ./agent zone_name");
            exit(1);
        }

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        try {
            String zoneName = args[0];
            Agent object = new Agent(new PathName(zoneName));
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
