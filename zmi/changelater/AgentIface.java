package changelater;

import model.ZMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;

public interface AgentIface extends Remote {
    public HashSet<ZMI> zones() throws RemoteException;
    public ZMI zone(String zoneName) throws RemoteException;
    public void installQuery(String query) throws RemoteException;
    public void uninstallQuery(String query) throws RemoteException;
    public void setZoneValue(String zoneName) throws RemoteException;


    public void updateMachineStats(MachineStats machineState) throws RemoteException;
}
