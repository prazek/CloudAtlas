package core;


import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import model.CustomJsonSerializer;
import model.AttributesMap;
import model.PathName;
import model.ZMI;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class Client {
    public static void main(String[] args) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        String agentName = args[0];
        try {
            Registry registry = LocateRegistry.getRegistry(4242);
            AgentIface agent = (AgentIface) registry.lookup(agentName);

            HttpServer server = HttpServer.create(new InetSocketAddress(8042), 0);
            // Pages
            server.createContext("/", new MainPage());
            server.createContext("/zmi/", new ServeFileHandler("core/zmi.html", "text/html"));
            server.createContext("/setFallbackContact/", new ZMIPage());
            server.createContext("/installedQueries/", new InstalledQueriesPage(agent));
            server.createContext("/installQuery/", new InstallQueryPage(agent));
            server.createContext("/uninstallQuery/", new UninstallQueryPage(agent));
            server.createContext("/attributes/", new AttributesPage(agent));
            server.createContext("/plot/", new PlotPage());

            // Resouces
            server.createContext("/lib.js", new ServeFileHandler("core/lib.js", "application/javascript"));
            server.createContext("/jquery.js", new ServeFileHandler("core/jquery.js", "application/javascript"));
            server.createContext("/jquery.flot.js", new ServeFileHandler("core/jquery.flot.js", "application/javascript"));
            server.createContext("/zmi.css", new ServeFileHandler("core/zmi.css", "text/css"));

            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("Client running!");
        } catch (Exception e) {
            System.err.println("ComputeEngine exception:");
            e.printStackTrace();
        }
    }
    private static class MainPage implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Location", "/zmi/");
            t.sendResponseHeaders(302, 0);
        }
    }
    private static class ZMIPage implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "<h1>Main page</h1>" +
                    String.format("<p>%s</p>", t.getRequestURI().getRawPath())
                    + "<form>\n" +
                    "  Node name: <input type=\"text\" name=\"name\"><br>\n" +
                    "  Query: <input type=\"text\" name=\"name\"><br>\n" +
                    "  <input type=\"submit\" value=\"Submit\">\n" +
                    "</form>"
                    ;
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private static class AttributesPage implements HttpHandler {
        private final AgentIface agent;

        AttributesPage(AgentIface agent) {
            this.agent = agent;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                Gson gson = new CustomJsonSerializer().getSerializer();
                HashMap<PathName, ZMI> zmi = agent.zones();
                String response = gson.toJson(zmi);
                t.getResponseHeaders().add("Content-Type", "application/json");
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private static String inputStreamToString(InputStream is) throws IOException {
        return new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));
    }

    private static String stringFromFile(String path) throws IOException {
        InputStream html = Client.class.getClassLoader().getResourceAsStream(path);
        return inputStreamToString(html);

    }

    private static class ServeFileHandler implements HttpHandler {
        String path;
        String mimeType;
        ServeFileHandler(String path, String mimeType) {
            this.path = path;
            this.mimeType = mimeType;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            String responseHtml;
            try {
                responseHtml = stringFromFile(path);
            } catch (Exception e) {
                System.err.println(e);
                t.sendResponseHeaders(500, 0);
                OutputStream os = t.getResponseBody();
                os.close();
                return;
            }
            byte[] response = responseHtml.getBytes();
            System.out.println(mimeType);
            t.getResponseHeaders().add("Content-Type", mimeType);
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    private static class PlotPage implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String responseHtml = "xxx";
            try {
                InputStream html = this.getClass().getClassLoader().getResourceAsStream("core/plot.html");


                responseHtml = inputStreamToString(html);

            } catch (Exception e) {
                System.err.println(e);
            }
            String response = responseHtml;
            System.out.println("Response: " + response);
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private static Map<String, String> parseFormUrlencoded(String data) throws UnsupportedEncodingException {
        String[] pairs = data.split("\\&");
        System.err.println(data);
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < pairs.length; i++) {
            String[] fields = pairs[i].split("=", 2);
            if (fields.length != 2)
                throw new IllegalArgumentException("String not in correct format");
            String name = URLDecoder.decode(fields[0], "UTF-8");
            String value = URLDecoder.decode(fields[1], "UTF-8");
            result.put(name, value);
        }
        return result;
    }

    private static class InstallQueryPage implements HttpHandler {
        private final AgentIface agent;

        InstallQueryPage(AgentIface agent) {
            this.agent = agent;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                if (t.getRequestMethod().equals("GET")) {
                    t.sendResponseHeaders(405, 0);
                    OutputStream os = t.getResponseBody();
                    os.close();
                    return;
                } else if (t.getRequestMethod().equals("POST")) {
                    handlePOST(t);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            throw new UnsupportedOperationException();

        }

        private void handlePOST(HttpExchange t) throws IOException {
            String response = "ok";
            Map<String, String> data = parseFormUrlencoded(inputStreamToString(t.getRequestBody()));
            System.err.println(data);
            try {
                agent.installQuery(data.get("queryName"), data.get("query"));
            } catch (Exception ex) {
                System.err.println("Error:\n" + ex);
                t.getResponseHeaders().add("Content-Type", "text/html");
                t.sendResponseHeaders(400, ex.toString().length());
                OutputStream os = t.getResponseBody();
                os.write(ex.toString().getBytes());
                os.close();
                return;
            }
            System.out.println("Response: " + response);
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private static class InstalledQueriesPage implements HttpHandler {
        private final AgentIface agent;

        InstalledQueriesPage(AgentIface agent) {
            this.agent = agent;
        }
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                Gson gson = CustomJsonSerializer.getSerializer();
                AttributesMap queries = agent.getQueries();
                //ZMI other = agent.zone(new PathName("/pjwstk"));
                String response = gson.toJson(queries);
                t.getResponseHeaders().add("Content-Type", "application/json");
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class UninstallQueryPage implements HttpHandler {
        private final AgentIface agent;

        UninstallQueryPage(AgentIface agent) {
            this.agent = agent;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                if (t.getRequestMethod().equals("GET")) {
                    t.sendResponseHeaders(405, 0);
                    OutputStream os = t.getResponseBody();
                    os.close();
                    return;
                } else if (t.getRequestMethod().equals("POST")) {
                    handlePOST(t);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            throw new UnsupportedOperationException();

        }

        private void handlePOST(HttpExchange t) throws IOException {
            String response = "ok";
            Map<String, String> data = parseFormUrlencoded(inputStreamToString(t.getRequestBody()));
            System.err.println(data);
            try {
                agent.uninstallQuery(data.get("queryName"));
            } catch (Exception ex) {
                System.err.println("Error:\n" + ex);
                t.getResponseHeaders().add("Content-Type", "text/html");
                t.sendResponseHeaders(400, ex.toString().length());
                OutputStream os = t.getResponseBody();
                os.write(ex.toString().getBytes());
                os.close();
                return;
            }
            System.out.println("Response: " + response);
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}

