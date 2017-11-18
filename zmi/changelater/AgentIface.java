package changelater;

import model.Attribute;
import model.PathName;
import model.Value;
import model.ZMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface AgentIface extends Remote {
    HashMap<PathName, ZMI> zones() throws RemoteException;
    ZMI zone(PathName zoneName) throws RemoteException;
    void installQuery(String name, String query) throws RemoteException, Exception;
    void uninstallQuery(String query) throws RemoteException;
    void setZoneValue(PathName zoneName, Attribute attribute, Value value) throws RemoteException;
}
