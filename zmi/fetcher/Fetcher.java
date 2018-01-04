package fetcher;

import java.io.FileInputStream;
import java.io.InputStream;

import core.AgentGrpc;
import core.AgentOuterClass;
import core.Config;
import core.Database;
import interpreter.MachineInfoFetcher;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import model.*;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.util.*;

import static java.lang.System.exit;
import static java.lang.Thread.sleep;

public class Fetcher {
    private static Sigar sigar = new Sigar();

    private int collectionInterval = 300; //ms
    private int averagingInterval = 1000; // ms
    private String avgMethod;
    private Deque<AttributesMap> statsHistory;
    private PathName agentPathName;


    public Fetcher(String agentName, Properties config) {
        agentPathName = new PathName(agentName);
        statsHistory = new ArrayDeque<>();

        collectionInterval = Integer.parseInt(config.getProperty("collection_interval"));
        averagingInterval = Integer.parseInt(config.getProperty("averaging_interval"));
        setAvgMethod(config.getProperty("avg_method"));
    }

    private void setAvgMethod(String averagingMethod) {
        if (averagingMethod.equals("avg")) {
            avgMethod = "avg";
        }
        else if (averagingMethod.equals("exp")) {
            avgMethod = "exp";
        }
        else
            throw new RuntimeException("Unsupported averaging method [" + averagingMethod + "]");

    }

    public void updateHistory() throws SigarException {
        int size = averagingInterval / collectionInterval;
        AttributesMap currentState = MachineStatsFetcher.getMachineStats(sigar);
        assert (statsHistory.size() <= size);
        if (statsHistory.size() == size)
            statsHistory.pop();

        statsHistory.add(currentState);
    }

    public AttributesMap calculateExponentialAverage() {
        AttributesMap combined = new AttributesMap();
        for (AttributesMap states : statsHistory)
            expMaps(combined, states);
        return combined;
    }

    public AttributesMap calculateAverage() {
        AttributesMap combined = new AttributesMap();
        for (AttributesMap states : statsHistory)
            addMaps(combined, states);

        divideValues(combined, statsHistory.size());
        return combined;
    }

    public static void main(String[] args) {
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
            ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", Config.getAgentPort()).usePlaintext(true).build();

            AgentGrpc.AgentBlockingStub stub = AgentGrpc.newBlockingStub(channel);
            AttributesMap machineInfo = MachineInfoFetcher.getMachineInfo();

            InputStream iniFile = new FileInputStream(iniFileName);
            Properties config = new Properties();
            config.load(iniFile);

            Fetcher fetcher = new Fetcher(agentName, config);
            fetcher.sendAttributes(stub, machineInfo);

            while (true) {
                fetcher.updateHistory();
                AttributesMap averageStats;
                if (fetcher.avgMethod.equals("avg"))
                    averageStats = fetcher.calculateAverage();
                else if (fetcher.avgMethod.equals("exp"))
                    averageStats = fetcher.calculateExponentialAverage();
                else
                    throw new RuntimeException("Unknown avg");
                fetcher.sendAttributes(stub, averageStats);
                sleep(fetcher.collectionInterval);
            }
        } catch (Exception e) {
            System.err.println("Fetcher exception:");
            e.printStackTrace();
        }
    }


    private static void expMaps(AttributesMap combined, AttributesMap other) {
        Value p = new ValueDouble(0.8);
        Value mp = new ValueDouble(0.2);
        for (Map.Entry<Attribute, Value> stat : other) {
            Value accumulated = combined.getOrNull(stat.getKey());
            Value toAccumulate = stat.getValue();
            if (accumulated == null)
                accumulated = toAccumulate;
            else
                accumulated = accumulated.convertTo(TypePrimitive.DOUBLE).multiply(mp).
                        addValue(toAccumulate.convertTo(TypePrimitive.DOUBLE).multiply(p));

            combined.addOrChange(stat.getKey(), accumulated);
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

    private void sendAttributes(AgentGrpc.AgentBlockingStub stub, AttributesMap attributes) {
        for (Map.Entry<Attribute, Value> attribute : attributes)
            stub.setZoneValue(Database.SetZoneValueData.newBuilder()
                    .setPath(agentPathName.serialize())
                    .setAttribute(attribute.getKey().toString())
                    .setValue(attribute.getValue().serializeValue())
                    .build());
    }

}
