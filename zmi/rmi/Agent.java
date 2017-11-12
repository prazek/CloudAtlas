package rmi;

import model.ZMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet

public class Agent implements Remote {
    public HashSet<ZMI> zones() {}
    public ZMI zone(String zoneName) {}
    public void installQuery(String query) {}
    public void uninstallQuery(String query) {}
    // or string?
    public void setZoneValue(String zoneName) {}
}
