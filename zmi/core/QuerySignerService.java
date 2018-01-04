package core;

import com.google.protobuf.ByteString;
import interpreter.Interpreter;
import interpreter.QueryResult;
import interpreter.TestHierarchy;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.*;


class QuerySignerService extends SignerGrpc.SignerImplBase {
    private Map<Attribute, List<Attribute>> queryAttributes = new HashMap<>();
    private ZMI fakeZMI;
    AttributesMap queries = new AttributesMap();
    Signature signingEngine;
    KeyPair keyPair;

    QuerySignerService(ZMI fakeZMI, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        this.fakeZMI = fakeZMI;
        signingEngine = Signature.getInstance("SHA256withRSA");
        keyPair = new KeyPair(publicKey, privateKey);

    }

    static public class PrivateKeyReader {
        public static PrivateKey get(String filename)
                throws Exception {

            byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

            PKCS8EncodedKeySpec spec =
                    new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        }
    }

    static public class PublicKeyReader {

        public static PublicKey get(String filename)
                throws Exception {

            byte[] keyBytes = Files.readAllBytes(Paths.get(filename));

            X509EncodedKeySpec spec =
                    new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }
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

            if (!queryCertificate.getName().startsWith("&")) {
                throw new BadQueryException("name must start with &");
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
        try {

            Model.Query returnedQuery = Model.Query.newBuilder().setName(queryName).setCode(query).build();
            System.err.println("query prepared");

            signingEngine.initSign(keyPair.getPrivate());
            signingEngine.update(returnedQuery.toByteArray());
            byte[] signature = signingEngine.sign();

            signingEngine.initVerify(keyPair.getPublic());
            signingEngine.update(returnedQuery.toByteArray());
            if (!signingEngine.verify(signature)) {
                System.err.println("WTF is this?");
            }

            return SignerOuterClass.SignedQuery.newBuilder()
                    .setQuery(returnedQuery)
                    .setSignedQueryBytes(ByteString.copyFrom(signature)).build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public void signQueryRemove(Model.QueryName request, StreamObserver<SignerOuterClass.SignedUninstallQuery> responseObserver) {
        String queryCertificate = request.getS();
        try {
            if (!queryCertificate.startsWith("&"))
                throw new RuntimeException("name must start with &");

            uninstallQueryInZone(fakeZMI, queryCertificate);
            queries.remove(queryCertificate);
        } catch (Exception ex) {
            responseObserver.onError(ex);
            return;
        }

        Model.QueryName queryName = Model.QueryName.newBuilder().setS(queryCertificate).build();
        try {
            signingEngine.initSign(keyPair.getPrivate());
            signingEngine.update(queryName.toByteArray());
            byte[] signature = signingEngine.sign();

            responseObserver.onNext(SignerOuterClass.SignedUninstallQuery.newBuilder()
                    .setName(queryName)
                    .setSignedNameBytes(ByteString.copyFrom(signature)).build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private synchronized void uninstallQueryInZone(ZMI z, String queryName) {
        System.err.println("Uninstalling");
        z.getAttributes().remove(queryName);
        Attribute queryNameAttr = new Attribute(queryName);
        for (Attribute attr : queryAttributes.get(queryNameAttr)) {
            z.getAttributes().remove(attr);
        }
        queryAttributes.remove(queryNameAttr);
        queries.remove(queryNameAttr);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: query-signer public-key private-key");
            return;
        }

        System.out.println(args);
        PublicKey publicKey = PublicKeyReader.get(args[0]);
        PrivateKey privateKey = PrivateKeyReader.get(args[1]);


        ZMI fakeZMI = TestHierarchy.createTestHierarchy();

        int port = Config.getSignerPort();
        ServerBuilder serverBuilder = ServerBuilder.forPort(port).directExecutor();
        serverBuilder.addService(new QuerySignerService(fakeZMI, publicKey, privateKey));
        Server server = serverBuilder.build();
        server.start();

        System.err.println("Server started, listening on " + port);
        server.awaitTermination();
    }

}





