package interpreter;

import model.PathName;
import model.ValueString;
import model.ZMI;

import java.io.FileInputStream;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedInterpreter {
    private static Pattern pattern = Pattern.compile("(&\\w+)\\s*:\\s*(.*)");

    public static void main(String[] args) throws Exception {
        if(args.length > 0)
            System.setIn(new FileInputStream(args[0]));

        ZMI root = TestHierarchy.createTestHierarchy();
        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\\n");
        while(scanner.hasNext()) {
            String fullQuery = scanner.next();
            Matcher matcher = pattern.matcher(fullQuery);
            if (matcher.matches()) {
                String query = matcher.group(2);
                executeQueries(root, query);
            }
            else {
                throw new RuntimeException("Can't parse [" + fullQuery + "]");
            }
        }
        scanner.close();
    }

    private static PathName getPathName(ZMI zmi) {
        String name = ((ValueString)zmi.getAttributes().get("name")).getValue();
        return zmi.getFather() == null? PathName.ROOT : getPathName(zmi.getFather()).levelDown(name);
    }

    private static void executeQueries(ZMI zmi, String query) throws Exception {
        if(!zmi.getSons().isEmpty()) {
            for (ZMI son : zmi.getSons())
                executeQueries(son, query);
            Interpreter interpreter = new Interpreter(zmi);
            List<QueryResult> result = interpreter.run(query);
            PathName zone = getPathName(zmi);
            for (QueryResult r : result) {
                System.out.println(zone + ": " + r);
                zmi.getAttributes().addOrChange(r.getName(), r.getValue());
            }
        }
    }
}
