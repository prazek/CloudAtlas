package client;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import core.AgentGrpc;
import core.AgentIface;
import core.AgentOuterClass;
import core.Model;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import model.*;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.stream.Collectors;


public class Client {
    public static void main(String[] args) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        String agentName = args[0];
        try {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", 4321).usePlaintext(true).build();

            AgentGrpc.AgentBlockingStub agentStub = AgentGrpc.newBlockingStub(channel);

            HttpServer server = HttpServer.create(new InetSocketAddress(8042), 0);
            // Pages
            server.createContext("/", new MainPage());
            server.createContext("/zmi/", new ServeFileHandler("client/ZMI.html", "text/html"));
            server.createContext("/fallbackContacts/", new ContactsPage(agentStub));
            server.createContext("/installedQueries/", new InstalledQueriesPage(agentStub));
            server.createContext("/installQuery/", new InstallQueryPage(agentStub));
            server.createContext("/uninstallQuery/", new UninstallQueryPage(agentStub));
            server.createContext("/attributes/", new AttributesPage(agentStub));
            server.createContext("/plot/", new PlotPage());

            // Resouces
            server.createContext("/zmi.js", new ServeFileHandler("client/zmi.js", "application/javascript"));
            server.createContext("/lib.js", new ServeFileHandler("client/lib.js", "application/javascript"));
            server.createContext("/jquery.js", new ServeFileHandler("client/jquery.js", "application/javascript"));
            server.createContext("/jquery.flot.js", new ServeFileHandler("client/jquery.flot.js", "application/javascript"));
            server.createContext("/zmi.css", new ServeFileHandler("client/zmi.css", "text/css"));

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

    private static class AttributesPage implements HttpHandler {
        private final AgentGrpc.AgentBlockingStub agent;

        AttributesPage(AgentGrpc.AgentBlockingStub agent) {
            this.agent = agent;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                Gson gson = new CustomJsonSerializer().getSerializer();
                Iterator<Model.Zone> zmiIterator = agent.getZones(AgentOuterClass.Empty.newBuilder().build());
                Map<PathName, ZMI> zmi = new HashMap<>();
                while(zmiIterator.hasNext()) {
                    Model.Zone zone = zmiIterator.next();
                    zmi.put(PathName.fromProtobuf(zone.getPath()), ZMI.fromProtobuf(zone.getZmi()));
                }
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
                InputStream html = this.getClass().getClassLoader().getResourceAsStream("client/plot.html");


                responseHtml = inputStreamToString(html);

            } catch (Exception e) {
                System.err.println(e);
            }
            String response = responseHtml;
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
        private final AgentGrpc.AgentBlockingStub agent;


        InstallQueryPage(AgentGrpc.AgentBlockingStub agent) {
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
            try {
                agent.installQuery(Model.Query.newBuilder().setName(Model.QueryName.newBuilder().setS(data.get("queryName"))).setCode(data.get("query")).build());
            } catch (Exception ex) {
                System.err.println("Error:\n" + ex);
                t.getResponseHeaders().add("Content-Type", "text/html");
                t.sendResponseHeaders(400, ex.toString().length());
                OutputStream os = t.getResponseBody();
                os.write(ex.toString().getBytes());
                os.close();
                return;
            }
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private static class InstalledQueriesPage implements HttpHandler {
        private final AgentGrpc.AgentBlockingStub agent;

        InstalledQueriesPage(AgentGrpc.AgentBlockingStub agent) {
            this.agent = agent;
        }
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                Gson gson = CustomJsonSerializer.getSerializer();
                AttributesMap queries = AttributesMap.fromProtobuf(agent.getQueries(AgentOuterClass.Empty.newBuilder().build()));
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
        private final AgentGrpc.AgentBlockingStub agent;


        UninstallQueryPage(AgentGrpc.AgentBlockingStub agent) {
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
            try {
                agent.uninstallQuery(Model.QueryName.newBuilder().setS(data.get("queryName")).build());
            } catch (Exception ex) {
                System.err.println("Error:\n" + ex);
                t.getResponseHeaders().add("Content-Type", "text/html");
                t.sendResponseHeaders(400, ex.toString().length());
                OutputStream os = t.getResponseBody();
                os.write(ex.toString().getBytes());
                os.close();
                return;
            }
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private static class ContactsPage implements HttpHandler {
        private final AgentGrpc.AgentBlockingStub agent;

        ContactsPage(AgentGrpc.AgentBlockingStub agent) {
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

        private void handleGET(HttpExchange t) throws IOException {
            String response = "___";
            try {
                Iterator<Model.ValueContact> s = agent.getFallbackContacts(AgentOuterClass.Empty.newBuilder().build());
                //////////////////////////////////////////////////////////////////
                Gson gson = new CustomJsonSerializer().getSerializer();
                response = gson.toJson(s);
            } catch (Exception ex) {
                System.err.println("Error:\n" + ex);
                t.getResponseHeaders().add("Content-Type", "text/html");
                t.sendResponseHeaders(400, ex.toString().length());
                OutputStream os = t.getResponseBody();
                os.write(ex.toString().getBytes());
                os.close();
                return;
            }
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private void handlePOST(HttpExchange t) throws IOException {
            String response = "ok";
            try {
                String jString = inputStreamToString(t.getRequestBody());
                JsonParser parser = new JsonParser();
                JsonElement allContacts = parser.parse(jString);
                JsonArray contacts = allContacts.getAsJsonArray();
                Set<Model.ValueContact> newSet = new TreeSet<>();
                AgentOuterClass.ValueContacts.Builder builder = AgentOuterClass.ValueContacts.newBuilder();
                for (JsonElement e: contacts) {
                    String name = e.getAsJsonObject().get("name").getAsString();
                    String address = e.getAsJsonObject().get("address").getAsString();
                    // TODO(sbarzowski) not sure if getByName is a good idea
                    InetAddress addr = InetAddress.getByName(address);
                    Model.ValueContact c = Model.ValueContact.newBuilder().setPathName(Model.PathName.newBuilder().setP(name)).setInetAddress(addr.toString()).build();
                    builder.addContacts(c);
                }
                agent.setFallbackContacts(builder.build());
            } catch (Exception ex) {
                System.err.println("Error:\n" + ex);
                t.getResponseHeaders().add("Content-Type", "text/html");
                t.sendResponseHeaders(400, ex.toString().length());
                OutputStream os = t.getResponseBody();
                os.write(ex.toString().getBytes());
                os.close();
                return;
            }
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}

