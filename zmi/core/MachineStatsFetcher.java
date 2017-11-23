package core;

import model.*;
import org.hyperic.sigar.*;

import java.io.File;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MachineStatsFetcher {

    static AttributesMap getMachineStats(Sigar sigar) throws SigarException {
        AttributesMap map = new AttributesMap();
        map.add(new Attribute("cpu_load"), new ValueDouble(sigar.getCpuPerc().getCombined()));

        map.add(new Attribute("free_disc"), new ValueInt(new File("/").getFreeSpace()));
        map.add(new Attribute("total_disc"), new ValueInt(new File("/").getTotalSpace()));
        map.add(new Attribute("free_ram"), new ValueInt(getFreeMemory())); //  sigar.getMem().getFree()));
        map.add(new Attribute("total_ram"), new ValueInt(getTotalMemory()));  //sigar.getMem().getTotal()));
        map.add(new Attribute("free_swap"), new ValueInt(sigar.getSwap().getFree()));
        map.add(new Attribute("total_swap"), new ValueInt(sigar.getSwap().getTotal()));
        map.add(new Attribute("num_processes"), new ValueInt((long)sigar.getProcList().length));
        map.add(new Attribute("num_cores"), new ValueInt((long)Runtime.getRuntime().availableProcessors()));

        map.add(new Attribute("logged_users"), new ValueInt(runCommandAsLong("users | wc -w")));

        return map;
    }

    private static long runCommandAsLong(String command) {
        try {
            String output = runCommand(command);
            return Long.parseLong(output);
        }
        catch(Exception ex) {
                System.err.println(ex);
                return -1;
        }
    }

    private static String runCommand(String command) {
        try {
            Scanner s = new Scanner(runCommandRaw(command)).useDelimiter("\\A");
            String output = s.hasNext() ? s.next().trim() : "";
            return output;
        }
        catch(Exception ex){
            System.err.println(ex);
            return "";
        }
    }

    private static InputStream runCommandRaw(String command) {
        try {
            String cmd[] = {"/bin/sh",
                    "-c",
                    command};
            Process p = Runtime.getRuntime().exec(cmd);
            if (p.waitFor() != 0)
                System.err.println(command + " failed");
            return p.getInputStream();
        }
        catch(Exception ex) {
            System.err.println(ex);
            return null;
        }
    }

    private static int parseUnit(String unit) {
        if (unit.equals("M"))
            return 1;
        if (unit.equals("G"))
            return 1024;
        if (unit.equals("T"))
            return 1024 * 1024;
        return -1;
    }

    //private static Pattern topFreeMemoryPattern = Pattern.compile(".*MemRegions:.*\\s+(\\d+)(\\w)\\s+(resident).*");
    private static long getFreeMemory() {
        InputStream stream = runCommandRaw("top -l1 -stats mem");
        Scanner scanner = new Scanner(stream).useDelimiter("\\A");

        while (scanner.hasNextLine()) {
            String topKek = scanner.nextLine();
            Matcher matcher = topFreeMemoryPattern.matcher(topKek);
            if (matcher.matches()) {
                int multiplier = parseUnit(matcher.group(2));
                int result = Integer.parseInt(matcher.group(1));
                return result * multiplier;
            }
        }
        return -1;
    }

    //private static Pattern topTotalMemoryPattern = Pattern.compile(".*MemRegions:.*\\s+(\\d+)(\\w)\\s+(total).*");
    private static long getTotalMemory() {
        InputStream stream = runCommandRaw("top -l1 -stats mem");
        Scanner scanner = new Scanner(stream).useDelimiter("\\A");

        while (scanner.hasNextLine()) {
            String topKek = scanner.nextLine();
            Matcher matcher = topTotalMemoryPattern.matcher(topKek);
            if (matcher.matches()) {
                int multiplier = parseUnit(matcher.group(2));
                int result = Integer.parseInt(matcher.group(1));
                return result * multiplier;
            }
        }
        return -1;
    }


}
