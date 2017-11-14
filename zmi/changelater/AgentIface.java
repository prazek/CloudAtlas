package changelater;

import model.Attribute;
import model.PathName;
import model.Value;
import model.ZMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;

public interface AgentIface extends Remote {
    public HashMap<PathName, ZMI> zones() throws RemoteException;
    public ZMI zone(PathName zoneName) throws RemoteException;
    public void installQuery(String query) throws RemoteException;
    public void uninstallQuery(String query) throws RemoteException;
    public void setZoneValue(PathName zoneName, Attribute attribute, Value value) throws RemoteException;
}
