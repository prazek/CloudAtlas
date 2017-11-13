package changelater;

import model.Attribute;
import model.ValueDouble;
import model.ValueInt;
import org.hyperic.sigar.*;
import model.AttributesMap;


public class MachineStatsFetcher {

    static AttributesMap getMachineStats(Sigar sigar) throws SigarException {
        AttributesMap map = new AttributesMap();
        map.add(new Attribute("cpuLoad"), new ValueDouble(sigar.getCpuPerc().getCombined()));
        map.add(new Attribute("freeRam"), new ValueInt(sigar.getMem().getFree()));
        map.add(new Attribute("totalRam"), new ValueInt(sigar.getMem().getTotal()));
        map.add(new Attribute("freeSwap"), new ValueInt(sigar.getSwap().getFree()));
        map.add(new Attribute("totalSwap"), new ValueInt(sigar.getSwap().getTotal()));

        return map;
    }

}
