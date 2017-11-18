package interpreter;


import model.Attribute;
import model.AttributesMap;
import model.ValueString;

import java.net.InetAddress;
import java.util.Scanner;

public class MachineInfoFetcher {
    public static AttributesMap getMachineInfo() {
        AttributesMap map = new AttributesMap();

        try {
            Process p = Runtime.getRuntime().exec("uname -rs");
            p.waitFor();
            Scanner s = new Scanner(p.getInputStream()).useDelimiter("\\A");
            String output = s.hasNext() ? s.next() : "";

            map.add(new Attribute("kernel_ver"), new ValueString(output));
            // TODO list of dns_names?
            map.add(new Attribute("dns_names"), new ValueString(InetAddress.getLocalHost().getHostName()));
        } catch(Exception ex) {
        }

        return map;
    }
}
