package ee.cyber.xroad.mediator.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

import ee.cyber.xroad.mediator.MediatorSystemProperties;
import ee.cyber.xroad.mediator.TestResources;
import ee.ria.xroad.common.SystemProperties;
import ee.ria.xroad.common.message.SoapFault;

/**
 * ClientMediator integration test program.
 */
public final class ClientMediatorIntegrationTest {

    private static final int SERVER_PORT = 8060;

    private static Server dummyService;

    private static boolean doFault = false;

    private ClientMediatorIntegrationTest() {
    }

    /**
     * Main program entry point.
     * @param args command-line arguments
     * @throws Exception in case of any errors
     */
    public static void main(String[] args) throws Exception {
        System.setProperty(SystemProperties.CONFIGURATION_PATH,
                "src/test/resources/globalconf");

        System.setProperty(MediatorSystemProperties.IDENTIFIER_MAPPING_FILE,
                "src/test/resources/identifiermapping.xml");

        System.setProperty(MediatorSystemProperties.V5_XROAD_PROXY_ADDRESS,
                "http://127.0.0.1:" + SERVER_PORT);
        System.setProperty(MediatorSystemProperties.V5_XROAD_URIPROXY_ADDRESS,
                "http://127.0.0.1:" + SERVER_PORT);

        startServer();

        ClientMediator mediator = new ClientMediator();
        try {
            mediator.start();

            /*doPost("xroad-mtom.request", "Multipart/Related; "
                    + "start-info=\"application/soap+xml\"; "
                    + "type=\"application/xop+xml\"; "
                    + "boundary=\"jetty771207119h3h10dty\"");*/

            doPost("xroad-mimesoap.request", "Multipart/Related; "
                    + "start-info=\"application/soap+xml\"; "
                    + "type=\"application/xop+xml\"; "
                    + "boundary=\"jetty771207119h3h10dty\"");

            //doPost("v5xroad-simple.request");
            //doPost("v5xroad-test.request");
            //doPost("xroad-simple.request");

            //doGet("?uri=http://www.google.com");
            //doGet("?producer=foobarbaz");
            //doGet("?foo=bar");
            //doGet("listMembers?foo=bar&baz=buzz");

            //doFault = true;
            //doPost("v5xroad-test.request");

        } finally {
            mediator.stop();
            mediator.join();

            if (dummyService != null) {
                dummyService.stop();
                dummyService.join();
            }
        }
    }

    private static void doPost(String requestFileName, String contentType)
            throws Exception {
        String host = MediatorSystemProperties.getClientMediatorConnectorHost();
        int port = MediatorSystemProperties.getClientMediatorHttpPort();

        URL url = new URL("http://" + host + ":" + port);

        System.out.println("Sending POST request " + requestFileName + " to "
                + url);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType);

        OutputStream out = conn.getOutputStream();
        IOUtils.copy(TestResources.get(requestFileName), out);

        System.out.println("Response: ");
        IOUtils.copy(conn.getInputStream(), System.out);
    }

    private static void doGet(String requestUrl) throws Exception {
        String host = MediatorSystemProperties.getClientMediatorConnectorHost();
        int port = MediatorSystemProperties.getClientMediatorHttpPort();

        URL url = new URL("http://" + host + ":" + port + "/" + requestUrl);

        System.out.println("Sending GET request to " + url);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("GET");

        System.out.println("Response: ");
        IOUtils.copy(conn.getInputStream(), System.out);
    }

    private static void startServer() throws Exception {
        dummyService = new Server();

        Connector connector = new SelectChannelConnector();
        connector.setPort(SERVER_PORT);
        connector.setHost("127.0.0.1");
        dummyService.addConnector(connector);

        dummyService.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target,
                    Request baseRequest,
                    HttpServletRequest request,
                    HttpServletResponse response)
                    throws IOException, ServletException {
                System.out.println("BEGIN " + request.getMethod()
                        + " REQUEST: " + target);

                Enumeration<String> headers = request.getHeaderNames();
                while (headers.hasMoreElements()) {
                    String headerName = headers.nextElement();
                    System.out.println("### " + headerName + ": "
                            + request.getHeader(headerName));
                }
                System.out.println("### Content-Length: "
                        + request.getContentLength());

                response.setStatus(HttpServletResponse.SC_OK);

                if (request.getMethod().equalsIgnoreCase("GET")) {
                    Map<String, String[]> params = request.getParameterMap();
                    System.out.println("REQUEST PARAMETERS:");
                    for (Map.Entry<String, String[]> e : params.entrySet()) {
                        System.out.println(e.getKey() + " : "
                                + StringUtils.join(e.getValue(), ", "));
                    }

                    response.setContentType("text/plain");
                    String data = "Reponse content for GET request";
                    IOUtils.write(data, response.getOutputStream());
                } else { // POST
                    response.setContentType(request.getContentType());

                    byte[] data = IOUtils.toByteArray(request.getInputStream());

                    System.out.print("REQUEST CONTENT: ");
                    IOUtils.write(data, System.out);
                    System.out.println();

                    if (doFault) {
                        doFault = false;

                        String faultXml = SoapFault.createFaultXml("CODE",
                                "STRING", "ACTOR", "detail");
                        IOUtils.write(faultXml, response.getOutputStream());
                    } else {
                        IOUtils.write(data, response.getOutputStream());
                    }
                }

                baseRequest.setHandled(true);

                System.out.println("END REQUEST");
                System.out.println();
            }
        });

        dummyService.start();
    }
}
