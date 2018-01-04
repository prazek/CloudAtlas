package client;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import core.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import model.*;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


public class Client {

    public static void main(String[] args) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        String agentName = args[0];
        try {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", Config.getAgentPort()).usePlaintext(true).build();

            AgentGrpc.AgentBlockingStub agentStub = AgentGrpc.newBlockingStub(channel);

            ManagedChannel signerChannel = ManagedChannelBuilder.forAddress("127.0.0.1", Config.getSignerPort()).usePlaintext(true).build();

            SignerGrpc.SignerBlockingStub signerStub = SignerGrpc.newBlockingStub(signerChannel);

            int clientPort = Config.getClientPort();

            HttpServer server = HttpServer.create(new InetSocketAddress(clientPort), 0);
            // Pages
            server.createContext("/", new MainPage());
            server.createContext("/zmi/", new ServeFileHandler("client/ZMI.html", "text/html"));
            server.createContext("/fallbackContacts/", new ContactsPage(agentStub));
            server.createContext("/installedQueries/", new InstalledQueriesPage(signerStub));
            server.createContext("/installQuery/", new InstallQueryPage(signerStub, agentStub));
            server.createContext("/uninstallQuery/", new UninstallQueryPage(signerStub, agentStub));
            server.createContext("/attributes/", new AttributesPage(agentStub));
            server.createContext("/plot/", new PlotPage());

            // Resources
            server.createContext("/zmi.js", new ServeFileHandler("client/zmi.js", "application/javascript"));
            server.createContext("/lib.js", new ServeFileHandler("client/lib.js", "application/javascript"));
            server.createContext("/jquery.js", new ServeFileHandler("client/jquery.js", "application/javascript"));
            server.createContext("/jquery.flot.js", new ServeFileHandler("client/jquery.flot.js", "application/javascript"));
            server.createContext("/zmi.css", new ServeFileHandler("client/zmi.css", "text/css"));

            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("Client running on port " + clientPort + "!");
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
                System.err.println(time() + ": Got Attributes request");
                Gson gson = new CustomJsonSerializer().getSerializer();
                System.err.println(time() + ": Starting gRPC request");
                Iterator<Model.Zone> zmiIterator = agent.getZones(Model.Empty.newBuilder().build());
                System.err.println(time() + ": got gRPC response");
                Map<PathName, ZMI> zmi = new HashMap<>();
                while(zmiIterator.hasNext()) {
                    System.err.println(time() + ": next zone requested");
                    Model.Zone zone = zmiIterator.next();
                    System.err.println(time() + ": converting zone");
                    zmi.put(PathName.fromProtobuf(zone.getPath()), ZMI.fromProtobuf(zone.getZmi()));
                    System.err.println(time() + ": ready for next");
                }
                System.err.println(time() + ": finished gRPC");
                //String response = "{}";
                String response = gson.toJson(zmi);
                System.err.println(time() + ": converted to JSON");
                t.getResponseHeaders().add("Content-Type", "application/json");
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                System.err.println(time() + ": replied");

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

    private static String time() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
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
        private final SignerGrpc.SignerBlockingStub signer;

        InstallQueryPage(SignerGrpc.SignerBlockingStub signer, AgentGrpc.AgentBlockingStub agent) {
            this.agent = agent;
            this.signer = signer;
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
                SignerOuterClass.SignedQuery signedQuery =
                        signer.signInstallQuery(Model.Query.newBuilder()
                            .setName(Model.QueryName.newBuilder()
                                    .setS(data.get("queryName")))
                            .setCode(data.get("query")).build());

                agent.installQuery(signedQuery);
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

    // TODO should it communicate with Signer? probably not
    private static class InstalledQueriesPage implements HttpHandler {
        private final SignerGrpc.SignerBlockingStub signer;

        InstalledQueriesPage(SignerGrpc.SignerBlockingStub signer) {
            this.signer = signer;
        }

        @Override
        public void handle(HttpExchange t) {
            try {
                System.err.println("Installed queries request");
                Gson gson = CustomJsonSerializer.getSerializer();
                System.err.println("getQueries");
                Model.AttributesMap fromAgent = signer.getQueries(Model.Empty.newBuilder().build());
                System.err.println("getQueries finished");
                AttributesMap queries = AttributesMap.fromProtobuf(fromAgent);
                System.err.println("converted to Internal");
                String response = gson.toJson(queries);
                System.err.println("converted to json");
                t.getResponseHeaders().add("Content-Type", "application/json");
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                System.err.println("finished installed queries");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class UninstallQueryPage implements HttpHandler {
        private final SignerGrpc.SignerBlockingStub signer;
        private final AgentGrpc.AgentBlockingStub agent;

        UninstallQueryPage(SignerGrpc.SignerBlockingStub signer, AgentGrpc.AgentBlockingStub agent) {
            this.signer = signer;
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
                SignerOuterClass.SignedUninstallQuery uninstallQuery =
                        signer.signQueryRemove(Model.QueryName.newBuilder().setS(data.get("queryName")).build());

                agent.uninstallQuery(uninstallQuery);
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
                Iterator<Model.ValueContact> s = agent.getFallbackContacts(Model.Empty.newBuilder().build());
                List<ValueContact> contacts = new ArrayList<>();
                while (s.hasNext()) {
                    contacts.add(ValueContact.fromProtobuf(s.next()));
                }
                Gson gson = new CustomJsonSerializer().getSerializer();
                response = gson.toJson(contacts);
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
                Database.ValueContacts.Builder builder = Database.ValueContacts.newBuilder();
                for (JsonElement e: contacts) {
                    String name = e.getAsJsonObject().get("name").getAsString();
                    String address = e.getAsJsonObject().get("address").getAsString();
                    // TODO(sbarzowski) not sure if getByName is a good idea
                    InetAddress addr = InetAddress.getByName(address);
                    Model.ValueContact c = Model.ValueContact.newBuilder().setPathName(Model.PathName.newBuilder().setP(name)).setInetAddress(ByteString.copyFrom(addr.getAddress())).build();
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

