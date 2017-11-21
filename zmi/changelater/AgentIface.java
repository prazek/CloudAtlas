package changelater;

import model.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Set;

public interface AgentIface extends Remote {
    HashMap<PathName, ZMI> zones() throws RemoteException;
    ZMI zone(PathName zoneName) throws RemoteException;
    void installQuery(String name, String query) throws RemoteException, Exception;
    void uninstallQuery(String query) throws RemoteException;
    void setZoneValue(PathName zoneName, Attribute attribute, Value value) throws RemoteException;
    void setFallbackContacts(Set<ValueContact> fallbackContacts) throws RemoteException;
    Set<ValueContact> getFallbackContacts() throws RemoteException;
    AttributesMap getQueries() throws RemoteException;
}
