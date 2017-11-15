package changelater;


import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import model.PathName;
import model.ZMI;

import java.io.*;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
            server.createContext("/zmi/", new ZMIPage());
            server.createContext("/setFallbackContact/", new ZMIPage());
            server.createContext("/installQuery/", new ZMIPage());
            server.createContext("/uninstallQuery/", new ZMIPage());
            server.createContext("/attributes/", new AttributesPage(agent));
            server.createContext("/plot/", new PlotPage());
            server.createContext("/jquery.js", new ServeFileHandler("changelater/jquery.js", "application/javascript"));
            server.createContext("/jquery.flot.js", new ServeFileHandler("changelater/jquery.flot.js", "application/javascript"));
            server.createContext("/examples.css", new ServeFileHandler("changelater/examples.css", "text/css"));
            server.createContext("/example.html", new ServeFileHandler("changelater/example.html", "text/html"));
            server.setExecutor(null); // creates a default executor
            server.start();
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
                    String.format("<p>%s</p>", t.getRequestURI().getRawPath());
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
                System.out.println("attributes");
                Gson gson = new Gson();
                ZMI zmi = agent.zone(new PathName("/"));
                String response = zmi.toString();
                t.getResponseHeaders().add("Content-Type", "application/json");
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();

                System.out.println("json = ");
                System.out.println(gson.toJson("abc"));
                //System.out.println(gson.toJson("));


            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private static String inputStreamToString(InputStream is) throws IOException {
        return new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));
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
            String responseHtml = "xxx";

            try {
                InputStream html = this.getClass().getClassLoader().getResourceAsStream(path);


                responseHtml = inputStreamToString(html);

            } catch (Exception e) {
                System.err.println(e);
            }
            System.out.println(responseHtml);
            byte[] response = responseHtml.getBytes();
            System.out.println("Serve: " + response);
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
}

