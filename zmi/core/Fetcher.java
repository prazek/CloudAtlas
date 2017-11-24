package core;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import interpreter.MachineInfoFetcher;
import model.*;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Properties;

import static java.lang.System.exit;
import static java.lang.Thread.sleep;


public class Fetcher {
    private static Sigar sigar = new Sigar();

    private int collectionInterval = 300; //ms
    private int averagingInterval = 1000; // ms

    private Deque<AttributesMap> statsHistory;
    private PathName agentPathName;

    public Fetcher(String agentName, Properties config) {
        agentPathName = new PathName(agentName);
        statsHistory = new ArrayDeque<>();

        collectionInterval = Integer.parseInt(config.getProperty("collection_interval"));
        averagingInterval = Integer.parseInt(config.getProperty("averaging_interval"));
        


    }

    public void updateHistory() throws SigarException {
        int size = averagingInterval / collectionInterval;
        AttributesMap currentState = MachineStatsFetcher.getMachineStats(sigar);
        assert (statsHistory.size() <= size);
        if (statsHistory.size() == size)
            statsHistory.pop();

        statsHistory.add(currentState);
    }

    public AttributesMap calculateAverage() {
        AttributesMap combined = new AttributesMap();
        for (AttributesMap states: statsHistory)
            addMaps(combined, states);

        divideValues(combined, statsHistory.size());
        return combined;
    }

    public static void main(String[] args) throws RemoteException {
        if (args.length < 2) {
            System.err.println("Usage: ./fetcher zone_name fetcher.ini");
            exit(1);
        }
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        String agentName = args[0];
        String iniFileName = args[1];
        try {
            Registry registry = LocateRegistry.getRegistry(4242);
            AgentIface stub = (AgentIface) registry.lookup(agentName);
            AttributesMap machineInfo = MachineInfoFetcher.getMachineInfo();

            InputStream iniFile = new FileInputStream(iniFileName);
            Properties config = new Properties();
            config.load(iniFile);

            Fetcher fetcher = new Fetcher(agentName, config);
            fetcher.sendAttributes(stub, machineInfo);

            while (true) {
                fetcher.updateHistory();
                AttributesMap averageStats = fetcher.calculateAverage();

                fetcher.sendAttributes(stub, averageStats);
                sleep(fetcher.collectionInterval);
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

    private static void divideValues(AttributesMap map, long divider) {
        for (Map.Entry<Attribute, Value> stat : map) {
            Value dividerVal = null;
            if (stat.getValue().getType() == TypePrimitive.INTEGER)
                dividerVal = new ValueInt(divider);
            else if (stat.getValue().getType() == TypePrimitive.DOUBLE)
                dividerVal = new ValueDouble((double)divider);

            Value val = stat.getValue().divide(dividerVal);
            stat.setValue(val);
        }
    }

    private void sendAttributes(AgentIface stub, AttributesMap attributes) throws RemoteException {
        for (Map.Entry<Attribute, Value> attribute : attributes)
            stub.setZoneValue(agentPathName, attribute.getKey(), attribute.getValue());
    }

}
