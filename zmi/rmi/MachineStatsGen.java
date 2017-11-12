package rmi;

import org.hyperic.sigar.*;

public class MachineStatsGen {

    static MachineStats getMachineStats(Sigar sigar) throws SigarException {
        MachineStats stats = new MachineStats();
        stats.cpuLoad = sigar.getCpuPerc().getCombined();
        stats.freeRam = sigar.getMem().getFree();
        stats.totalRam = sigar.getMem().getTotal();
        stats.freeSwap = sigar.getSwap().getFree();
        stats.totalSwap = sigar.getSwap().getTotal();
        return stats;
    }

}
