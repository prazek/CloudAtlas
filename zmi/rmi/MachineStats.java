package rmi;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;




public class MachineStats {

    public double cpuLoad;
    public long freeDisc, totalDisc, freeRam, totalRam, freeSwap, totalSwap;
    // TODO numProcesses,

    public MachineStats() {}

    public MachineStats add(MachineStats other) {
        MachineStats res = other.clone();
        res.cpuLoad   += cpuLoad;
        res.freeDisc  += freeDisc;
        res.totalDisc += totalDisc;
        res.freeRam   += freeRam;
        res.freeSwap  += freeSwap;
        res.totalSwap += totalSwap;
        return res;
    }

    public MachineStats clone() {
        MachineStats res = new MachineStats();
        res.cpuLoad   = cpuLoad;
        res.freeDisc  = freeDisc;
        res.totalDisc = totalDisc;
        res.freeRam   = freeRam;
        res.freeSwap  = freeSwap;
        res.totalSwap = totalSwap;
        return res;
    }


    public MachineStats div(long divider) {
        MachineStats res = clone();
        res.cpuLoad   /= divider;
        res.freeDisc  /= divider;
        res.totalDisc /= divider;
        res.freeRam   /= divider;
        res.freeSwap  /= divider;
        res.totalSwap /= divider;
        return res;
    }

}
