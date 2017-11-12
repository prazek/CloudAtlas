package rmi;

import model.ZMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet

public class Agent implements Remote {
    public HashSet<ZMI> zones() {}
    public void installQuery() {}
    public void uninstallQuery() {}
    // or string?
    public void setZoneValue(ZMI zmi) {}
}
