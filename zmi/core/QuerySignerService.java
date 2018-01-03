package core;

import interpreter.Interpreter;
import interpreter.QueryResult;
import interpreter.TestHierarchy;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import model.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class QuerySignerService extends SignerGrpc.SignerImplBase {
    private Map<Attribute, List<Attribute>> queryAttributes = new HashMap<>();
    private ZMI fakeZMI;
    AttributesMap queries = new AttributesMap();

    QuerySignerService(ZMI fakeZMI) {
        this.fakeZMI = fakeZMI;
    }

    class BadQueryException extends Exception {
        String msg;

        BadQueryException(String msg) {
            this.msg = msg;
        }
    }

    @Override
    public void getQueries(Model.Empty request, StreamObserver<Model.AttributesMap> responseObserver) {
        try {
            responseObserver.onNext(queries.serialize());
            responseObserver.onCompleted();
        } catch (Exception r) {
            responseObserver.onError(r);
        }
    }

    @Override
    public void signInstallQuery(Model.Query request, StreamObserver<SignerOuterClass.SignedQuery> responseObserver) {
        Attribute queryCertificate = new Attribute(request.getName().getS());
        String query = request.getCode();

        try {

            Interpreter interpreter = new Interpreter(fakeZMI);
            List<QueryResult> results;
            try {
                results = interpreter.run(query);
            } catch (Exception e) {
                throw new BadQueryException(e.getMessage());
            }

            // Put attributes if first run of this query
            if (queryAttributes.containsKey(queryCertificate)) {
                throw new BadQueryException("Query already exist");
            }

            ArrayList<Attribute> createdAttributes = new ArrayList<>();
            for (QueryResult r : results) {
                Attribute producedValueName = r.getName();
                createdAttributes.add(producedValueName);
                if (fakeZMI.getAttributes().getOrNull(producedValueName) != null) {
                    throw new BadQueryException(
                            "Query [" + query + "] is producing value [" + producedValueName +
                                    "] that was added by other query or saved as attribute");
                }
            }
            queryAttributes.put(queryCertificate, createdAttributes);
            for (QueryResult r : results) {
                fakeZMI.getAttributes().addOrChange(r.getName(), r.getValue());
                System.out.println("Applying result for [" + r.getName() + "] with value [" + r.getValue() + "]");
            }

            System.out.println("adding query to the set");
            queries.add(request.getName().getS(), new ValueString(query));

            System.out.println("replying");

            SignerOuterClass.SignedQuery signedQuery = signQuery(request.getName(), request.getCode());
            responseObserver.onNext(signedQuery);
            System.err.println("response sent");
            responseObserver.onCompleted();
            System.out.println("installed");
        } catch (BadQueryException ex) {
            System.err.println(ex);
            responseObserver.onError(Status.INTERNAL
                .withDescription(ex.msg)
                .withCause(ex) // This can be attached to the Status locally, but NOT transmitted to the client!
                .asRuntimeException());
        } catch (Exception ex) {
            System.err.println(ex);
            responseObserver.onError(ex);
        }

    }

    SignerOuterClass.SignedQuery signQuery(Model.QueryName queryName, String query) {

        // TODO sign something
        Model.Query returnedQuery = Model.Query.newBuilder().setName(queryName).setCode(query).build();
        System.err.println("query prepared");
        return SignerOuterClass.SignedQuery.newBuilder().setQuery(returnedQuery).build();
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

    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
        ZMI fakeZMI = TestHierarchy.createTestHierarchy();

        int port = 9876;
        ServerBuilder serverBuilder = ServerBuilder.forPort(port).directExecutor();
        serverBuilder.addService(new QuerySignerService(fakeZMI));
        Server server = serverBuilder.build();
        server.start();

        System.err.println("Server started, listening on " + port);
        server.awaitTermination();
    }

}





