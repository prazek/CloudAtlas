package changelater;

import model.Attribute;
import model.ValueDouble;
import model.ValueInt;
import org.hyperic.sigar.*;
import model.AttributesMap;


public class MachineStatsFetcher {

    static AttributesMap getMachineStats(Sigar sigar) throws SigarException {
        AttributesMap map = new AttributesMap();
        map.add(new Attribute("cpu_load"), new ValueDouble(sigar.getCpuPerc().getCombined()));
        //map.add(new Attribute("free_disc")
        //map.add(new Attribute("total_disc")

        map.add(new Attribute("free_ram"), new ValueInt(sigar.getMem().getFree()));
        map.add(new Attribute("total_ram"), new ValueInt(sigar.getMem().getTotal()));
        map.add(new Attribute("free_swap"), new ValueInt(sigar.getSwap().getFree()));
        map.add(new Attribute("total_swap"), new ValueInt(sigar.getSwap().getTotal()));
        map.add(new Attribute("num_processes"), new ValueInt((long)sigar.getProcList().length));
        // map.add(new Attribute("num_cores"), new ValueInt();
        // map.add(new Attribute("kernel_ver")
        // map.add(new Attribute("logged_users")
        // map.add(new Attribute("dns_names")

        return map;
    }

}
