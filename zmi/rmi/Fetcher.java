package rmi;


import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.util.ArrayDeque;
import java.util.Deque;

import static java.lang.Thread.sleep;


public class Fetcher {
    private static Sigar sigar = new Sigar();

    // TODO make it configurable via .ini file.
    private static int collectionInterval = 100; //ms
    private static int averagingInterval = 1000; // ms

    private Deque<MachineStats> statsHistory;


    public Fetcher() {
        statsHistory = new ArrayDeque<>();
    }

    public void updateHistory() throws SigarException {
        int size = Fetcher.averagingInterval / collectionInterval;
        MachineStats currStats = MachineStatsGen.getMachineStats(sigar);
        assert (statsHistory.size() <= size);
        if (statsHistory.size() == size)
            statsHistory.pop();

        statsHistory.add(currStats);
    }

    public static void main(String[] args) throws RemoteException {
        if (args.length < 1) {
            System.err.println("Usage: ./fetcher zone_name");
        }
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        String agentName = args[0];
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            Agent stub = (Agent) registry.lookup(agentName);

            while (true) {
                Fetcher fetcher = new Fetcher();
                fetcher.updateHistory();
                MachineStats combined = new MachineStats();
                for (MachineStats stat : fetcher.statsHistory)
                    combined = combined.add(stat);

                combined = combined.div(fetcher.statsHistory.size());

                stub.updateMachineStats(combined);
                sleep(collectionInterval);
            }
        } catch (Exception e) {
            System.err.println("Fetcher exception:");
            e.printStackTrace();
        }
    }
}
