package core;

import model.*;
import org.hyperic.sigar.*;

import java.io.File;


public class MachineStatsFetcher {

    static AttributesMap getMachineStats(Sigar sigar) throws SigarException {
        AttributesMap map = new AttributesMap();
        map.add(new Attribute("cpu_load"), new ValueDouble(sigar.getCpuPerc().getCombined()));

        map.add(new Attribute("free_disc"), new ValueInt(new File("/").getFreeSpace()));
        map.add(new Attribute("total_disc"), new ValueInt(new File("/").getTotalSpace()));

        map.add(new Attribute("free_ram"), new ValueInt(Runtime.getRuntime().freeMemory())); //  sigar.getMem().getFree()));
        // todo fix total memory
        map.add(new Attribute("total_ram"), new ValueInt(Runtime.getRuntime().totalMemory()));  //sigar.getMem().getTotal()));
        map.add(new Attribute("free_swap"), new ValueInt(sigar.getSwap().getFree()));
        map.add(new Attribute("total_swap"), new ValueInt(sigar.getSwap().getTotal()));
        map.add(new Attribute("num_processes"), new ValueInt((long)sigar.getProcList().length));
        map.add(new Attribute("num_cores"), new ValueInt((long)Runtime.getRuntime().availableProcessors()));


        // map.add(new Attribute("logged_users")

        return map;
    }

}