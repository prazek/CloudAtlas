package changelater;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class MachineStats implements Serializable {

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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CPU: ");
        sb.append(cpuLoad);
        sb.append("\nFree disc: ");
        sb.append(freeDisc);
        sb.append("\nTotal disc ");
        sb.append(totalDisc);
        sb.append("\nFree ram: ");
        sb.append(freeRam);
        sb.append("\nTotal ram: ");
        sb.append(totalRam);
        sb.append("\nFree swap: ");
        sb.append(freeSwap);
        sb.append("\nTotal swap: ");
        sb.append(totalSwap);

        return sb.toString();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(toString());
    }
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.readObject();
    }

}
