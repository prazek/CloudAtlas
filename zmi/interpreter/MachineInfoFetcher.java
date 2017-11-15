package interpreter;


import model.Attribute;
import model.AttributesMap;
import model.ValueString;

import java.io.IOException;
import java.io.StringWriter;
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
            // map.add(new Attribute("dns_names")
        } catch(Exception ex) {
        }

        return map;
    }
}
