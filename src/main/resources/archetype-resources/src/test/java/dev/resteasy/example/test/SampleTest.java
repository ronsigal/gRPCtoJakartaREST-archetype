package dev.resteasy.grpc.test;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
//import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import ${generate-package}.${generate-prefix}ServiceGrpc;
import ${generate-package}.${generate-prefix}_Server;
import ${generate-package}.${generate-prefix}_proto.GeneralEntityMessage;
import ${generate-package}.${generate-prefix}_proto.GeneralReturnMessage;
import ${generate-package}.${generate-prefix}_proto.gString;
import com.google.protobuf.Message;

import dev.resteasy.example.SampleServer;
import dev.resteasy.example.grpc.greet.GeneralGreeting;
import dev.resteasy.grpc.arrays.Array_proto;
import dev.resteasy.grpc.bridge.runtime.protobuf.JavabufTranslator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;


/**
 * SampleTest + SampleServer constitute a sample client and server, where SampleTest
 * makes a gRPC invocation on the Jakarta REST SampleServer.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SampleTest {

    private static JavabufTranslator translator;
    private static ManagedChannel channelPlaintext;
    private static ${generate-prefix}ServiceGrpc.${generate-prefix}ServiceBlockingStub stub;

    static {
        Class<?> clazz;
        try {
            clazz = Class.forName("${generate-package}.${generate-prefix}JavabufTranslator");
            translator = (JavabufTranslator) clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {
       String currentDirectory = System.getProperty("user.dir");
       String webDir = currentDirectory + "/src/main/webapp/WEB-INF/web.xml";
       File webXml = new File(webDir);
       final var resolver = Maven.resolver()
                .loadPomFromFile("pom.xml");
        Archive<?> ar = ShrinkWrap.create(WebArchive.class, SampleTest.class.getSimpleName() + ".war")
                .addClass(SampleServer.class)
                .addPackage(${generate-prefix}_Server.class.getPackage())
                .addPackage(GeneralGreeting.class.getPackage())
                .addClass(Array_proto.class)
                .addAsLibrary(resolver.resolve("dev.resteasy.grpc:grpc-bridge-runtime")
                        .withoutTransitivity()
                        .asSingleFile())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(webXml, "web.xml")
                ;
//        ar.as(ZipExporter.class).exportTo(new File("/tmp/sample.war"), true);
        return ar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        accessServletContexts();
        channelPlaintext = ManagedChannelBuilder.forTarget("localhost:9555").usePlaintext().build();
        stub = ${generate-prefix}ServiceGrpc.newBlockingStub(channelPlaintext);
    }

    @AfterClass
    public static void afterClass() throws InterruptedException {
        if (channelPlaintext != null) {
            channelPlaintext.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    static void accessServletContexts() {
        try (
            Client client = ClientBuilder.newClient();
            var response = client.target("http://localhost:8080/grpc-test/grpcToJakartaRest/grpcserver/context")
                              .request()
                              .get()) {
            Assert.assertTrue(204 == response.getStatus());
        }
    }

    /**
     * testEcho1() creates a javabuf gString from a Java String by calling JavabufTranslator.translateToJavabuf(),
     * stores it in a GeneralEntityMessage, and translates the returned gString to a String
     * calling JavabufTranslator.translateFromJavabuf().
     */
    @Test
    public void testEcho1() throws Exception {
        Message m = translator.translateToJavabuf("hello");
        GeneralEntityMessage.Builder builder = GeneralEntityMessage.newBuilder();
        GeneralEntityMessage gem = builder.setGStringField((gString) m).build();
        GeneralReturnMessage response = stub.echo(gem);
        Message reply = response.getGStringField();
        String s = (String) translator.translateFromJavabuf(reply);
        Assert.assertTrue("hello".equals(s));
    }

    /**
     * testEcho2() is the same as testEcho1(), except that it creates a javabuf gString using 
     * gString.Builder, and it calls gString.getValue() to get a String from the returned gString.
     */
    @Test
    public void testEcho2() throws Exception {
        Message m = gString.newBuilder().setValue("goodbye").build();
        GeneralEntityMessage.Builder builder = GeneralEntityMessage.newBuilder();
        GeneralEntityMessage gem = builder.setGStringField((gString) m).build();
        GeneralReturnMessage response = stub.echo(gem);
        String reply = response.getGStringField().getValue();
        Assert.assertTrue("goodbye".equals(reply));
    }
}
