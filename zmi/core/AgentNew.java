package core;


import model.PathName;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import static java.lang.System.exit;

// Temporary name until refactored Agent.
public class AgentNew {


    static private Executor[] initExecutors() {
        Timer timer = new Timer();
        Network network = new Network();
        DatabaseUpdater databaseUpdater = new DatabaseUpdater(network, timer);

        network.setDatabaseUpdater(databaseUpdater);
        RMIModule rmiModule = new RMIModule(databaseUpdater);
        return new Executor[] {timer, network, databaseUpdater, rmiModule};
    }

    static public void main(String args[]) {
        if (args.length == 0) {
            System.err.println("Usage: ./agent zone_name");
            exit(1);
        }

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        Executor[] executors = initExecutors();
        for (Executor executor : executors) {
            Thread t = new Thread(executor);
            t.run();
        }



        try {
            String zoneName = args[0];
            core.Agent agent = new core.Agent(new PathName(zoneName));

            AgentIface stub =
                    (AgentIface) UnicastRemoteObject.exportObject(agent, 0);
            Registry registry = LocateRegistry.getRegistry(4242);
            registry.rebind(zoneName, stub);
            System.out.println("Agent bound");
            core.Agent.RunQueries queryRunner = new core.Agent.RunQueries(agent);
            Thread t = new Thread(queryRunner);
            t.run();
        } catch (Exception e) {
            System.err.println("Agent exception:");
            e.printStackTrace();
        }
    }
}
