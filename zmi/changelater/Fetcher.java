package changelater;


import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import model.*;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import static java.lang.System.exit;
import static java.lang.Thread.sleep;


public class Fetcher {
    private static Sigar sigar = new Sigar();

    // TODO make it configurable via .ini file.
    private static int collectionInterval = 100; //ms
    private static int averagingInterval = 1000; // ms

    private Deque<AttributesMap> statsHistory;


    public Fetcher() {
        statsHistory = new ArrayDeque<>();
    }

    public void updateHistory() throws SigarException {
        int size = Fetcher.averagingInterval / collectionInterval;
        AttributesMap currentState = MachineStatsFetcher.getMachineStats(sigar);
        assert (statsHistory.size() <= size);
        if (statsHistory.size() == size)
            statsHistory.pop();

        statsHistory.add(currentState);
    }

    public static void main(String[] args) throws RemoteException {
        if (args.length < 1) {
            System.err.println("Usage: ./fetcher zone_name");
            exit(1);
        }
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        String agentName = args[0];
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            AgentIface stub = (AgentIface) registry.lookup(agentName);

            while (true) {
                Fetcher fetcher = new Fetcher();
                fetcher.updateHistory();
                AttributesMap combined = new AttributesMap();
                for (AttributesMap states: fetcher.statsHistory)
                    addMaps(combined, states);

                for (Map.Entry<Attribute, Value> stat : combined) {
                    Value val = stat.getValue().divide(new ValueInt((long)fetcher.statsHistory.size()));
                    stub.setZoneValue(new PathName(agentName), stat.getKey(), val);
                }

                sleep(collectionInterval);
            }
        } catch (Exception e) {
            System.err.println("Fetcher exception:");
            e.printStackTrace();
        }
    }

    private static void addMaps(AttributesMap combined, AttributesMap other) {
        for (Map.Entry<Attribute, Value> stat : other) {
            Value accumulated = combined.getOrNull(stat.getKey());
            Value toAccumulate = stat.getValue();
            if (accumulated == null)
                accumulated = toAccumulate;
            else
                accumulated = accumulated.addValue(toAccumulate);

            combined.addOrChange(stat.getKey(), accumulated);
        }
    }
}
