package core;

import interpreter.Interpreter;
import interpreter.QueryResult;
import interpreter.TestHierarchy;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import model.Attribute;
import model.ZMI;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class QuerySignerService extends SignerGrpc.SignerImplBase {
    private Map<Attribute, List<Attribute>> queryAttributes = new HashMap<>();
    private ZMI fakeZMI;

    QuerySignerService(ZMI fakeZMI) {
        this.fakeZMI = fakeZMI;
    }

    @Override
    public void signInstallQuery(Model.Query request, StreamObserver<SignerOuterClass.SignedQuery> responseObserver) {
        Attribute queryCertificate = new Attribute(request.getName().getS());
        String query = request.getCode();

        try {

            Interpreter interpreter = new Interpreter(fakeZMI);
            List<QueryResult> results = interpreter.run(query);

            // Put attributes if first run of this query
            if (queryAttributes.containsKey(queryCertificate)) {
                throw new RuntimeException("Query already exist");
            }

            ArrayList<Attribute> createdAttributes = new ArrayList<>();
            for (QueryResult r : results) {
                Attribute producedValueName = r.getName();
                createdAttributes.add(producedValueName);
                if (fakeZMI.getAttributes().getOrNull(producedValueName) != null) {
                    throw new RuntimeException(
                            "Query [" + query + "] is producing value [" + producedValueName +
                                    "] that was added by other query or saved as attribute");
                }
            }
            queryAttributes.put(queryCertificate, createdAttributes);
            for (QueryResult r : results) {
                fakeZMI.getAttributes().addOrChange(r.getName(), r.getValue());
                System.out.println("Applying result for [" + r.getName() + "] with value [" + r.getValue() + "]");
            }


        } catch (Exception ex) {
            responseObserver.onError(ex);
        }

        // TODO sign something
        Model.Query returnedQuery = Model.Query.newBuilder().setName(request.getName()).setCode(request.getCode()).build();
        responseObserver.onNext(SignerOuterClass.SignedQuery.newBuilder().setQuery(returnedQuery).build());
        responseObserver.onCompleted();
    }

    @Override
    public void signQueryRemove(Model.QueryName request, StreamObserver<SignerOuterClass.SignedUnistallQuery> responseObserver) {
        String queryCertificate = request.getS();
        try {
            if (!queryCertificate.startsWith("&"))
                throw new RuntimeException("name must start with &");

            uninstallQueryInZone(fakeZMI, queryCertificate);
        } catch (Exception ex) {
            responseObserver.onError(ex);
            return;
        }


        responseObserver.onNext(SignerOuterClass.SignedUnistallQuery.newBuilder().build());
        responseObserver.onCompleted();
    }


    private synchronized void uninstallQueryInZone(ZMI z, String queryName) {
        z.getAttributes().remove(queryName);
        for (Attribute attr : queryAttributes.get(new Attribute(queryName))) {
            z.getAttributes().remove(attr);
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        /*
        QuerySignerService signerService = new QuerySignerService();
        Server signerServer = InProcessServerBuilder.forName("signer_module").addService(signerService).build();
        signerServer.start();
        ManagedChannel signerChannel = InProcessChannelBuilder.forName("signer_module").build();
        SignerGrpc.SignerStub signerStub = SignerGrpc.newStub(signerChannel);
        */
        ZMI fakeZMI = TestHierarchy.createTestHierarchy();

        int port = 2137;
        ServerBuilder serverBuilder = ServerBuilder.forPort(port);
        serverBuilder.addService(new QuerySignerService(fakeZMI));
        Server server = serverBuilder.build();
        server.start();

        System.err.println("Server started, listening on " + port);


    }

}





