package rmi;

import model.ZMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;

public class Agent implements Remote {
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
}
