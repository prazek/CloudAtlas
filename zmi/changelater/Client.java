package changelater;


import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
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
            Registry registry = LocateRegistry.getRegistry("localhost");
            AgentIface agent = (AgentIface) registry.lookup(agentName);

            HttpServer server = HttpServer.create(new InetSocketAddress(8042), 0);
            server.createContext("/", new MainPage());
            server.createContext("/zmi/", new ServeFileHandler("changelater/zmi.html", "text/html"));
            server.createContext("/setFallbackContact/", new ZMIPage());
            server.createContext("/installQuery/", new InstallQueryPage(agent));
            server.createContext("/uninstallQuery/", new ZMIPage());
            server.createContext("/attributes/", new AttributesPage(agent));
            server.createContext("/plot/", new PlotPage());
            server.createContext("/lib.js", new ServeFileHandler("changelater/lib.js", "application/javascript"));

            server.createContext("/jquery.js", new ServeFileHandler("changelater/jquery.js", "application/javascript"));
            server.createContext("/jquery.flot.js", new ServeFileHandler("changelater/jquery.flot.js", "application/javascript"));
            server.createContext("/zmi.css", new ServeFileHandler("changelater/zmi.css", "text/css"));
            server.createContext("/example.html", new ServeFileHandler("changelater/example.html", "text/html"));
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
                Gson gson = new Gson();
                ZMI zmi = agent.zone(new PathName("/uw/violet07"));
                //ZMI other = agent.zone(new PathName("/pjwstk"));
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
                InputStream html = this.getClass().getClassLoader().getResourceAsStream("changelater/plot.html");


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
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < pairs.length; i++) {
            String[] fields = pairs[i].split("=");
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
                    handleGET(t);
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

        private void handleGET(HttpExchange t) throws IOException {

            String response = stringFromFile("changelater/installQuery.html");
            System.out.println("Response: " + response);
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}

