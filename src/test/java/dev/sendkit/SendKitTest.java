package dev.sendkit;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SendKitTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private SendKit client() {
        return new SendKit("sk_test_123", "http://localhost:" + port);
    }

    @Test
    void testSendEmail() {
        server.createContext("/v1/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"from\":\"sender@example.com\""));
            assertTrue(body.contains("\"subject\":\"Hello\""));
            assertEquals("Bearer sk_test_123", exchange.getRequestHeaders().getFirst("Authorization"));
            assertEquals("application/json", exchange.getRequestHeaders().getFirst("Content-Type"));

            String response = "{\"id\":\"email_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", List.of("to@example.com"), "Hello")
                        .html("<p>Hi</p>")
        );

        assertEquals("email_123", result.getId());
    }

    @Test
    void testSendEmailWithAllOptions() {
        server.createContext("/v1/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"cc\":[\"cc@example.com\"]"));
            assertTrue(body.contains("\"bcc\":[\"bcc@example.com\"]"));
            assertTrue(body.contains("\"reply_to\":\"reply@example.com\""));
            assertTrue(body.contains("\"tags\":[\"welcome\"]"));
            assertTrue(body.contains("\"scheduled_at\":\"2025-01-01T00:00:00Z\""));

            String response = "{\"id\":\"email_456\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", List.of("to@example.com"), "Hello")
                        .html("<p>Hi</p>")
                        .text("Hi")
                        .cc(List.of("cc@example.com"))
                        .bcc(List.of("bcc@example.com"))
                        .replyTo("reply@example.com")
                        .tags(List.of("welcome"))
                        .scheduledAt("2025-01-01T00:00:00Z")
        );

        assertEquals("email_456", result.getId());
    }

    @Test
    void testSendMimeEmail() {
        server.createContext("/v1/emails/mime", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"envelope_from\":\"from@example.com\""));
            assertTrue(body.contains("\"envelope_to\":\"to@example.com\""));
            assertTrue(body.contains("\"raw_message\":\"MIME-Version: 1.0\""));

            String response = "{\"id\":\"mime_789\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendMimeEmailResponse result = client().emails().sendMime(
                new Emails.SendMimeEmailParams("from@example.com", "to@example.com", "MIME-Version: 1.0")
        );

        assertEquals("mime_789", result.getId());
    }

    @Test
    void testApiError() {
        server.createContext("/v1/emails", exchange -> {
            String response = "{\"name\":\"validation_error\",\"message\":\"Invalid email\",\"status_code\":422}";
            exchange.sendResponseHeaders(422, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        SendKitException ex = assertThrows(SendKitException.class, () ->
                client().emails().send(
                        new Emails.SendEmailParams("bad", List.of("to@example.com"), "Hello")
                )
        );

        assertEquals("Invalid email", ex.getMessage());
        assertEquals("validation_error", ex.getName());
        assertEquals(422, ex.getStatusCode());
    }

    @Test
    void testMissingApiKey() {
        SendKitException ex = assertThrows(SendKitException.class, () ->
                new SendKit("", "http://localhost:" + port)
        );
        assertEquals("Missing API key", ex.getMessage());
        assertEquals("missing_api_key", ex.getName());
    }

    @Test
    void testNullApiKey() {
        SendKitException ex = assertThrows(SendKitException.class, () ->
                new SendKit(null, "http://localhost:" + port)
        );
        assertEquals("Missing API key", ex.getMessage());
    }

    @Test
    void testCustomBaseUrl() {
        server.createContext("/v1/emails", exchange -> {
            String response = "{\"id\":\"custom_url\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        SendKit customClient = new SendKit("sk_test_123", "http://localhost:" + port + "/");
        Emails.SendEmailResponse result = customClient.emails().send(
                new Emails.SendEmailParams("sender@example.com", List.of("to@example.com"), "Hello")
                        .html("<p>Hi</p>")
        );

        assertEquals("custom_url", result.getId());
    }

    @Test
    void testAttachments() {
        server.createContext("/v1/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"filename\":\"test.pdf\""));
            assertTrue(body.contains("\"content\":\"base64data\""));
            assertTrue(body.contains("\"content_type\":\"application/pdf\""));

            String response = "{\"id\":\"attach_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", List.of("to@example.com"), "Hello")
                        .html("<p>Hi</p>")
                        .attachments(List.of(
                                new Emails.Attachment("test.pdf", "base64data").contentType("application/pdf")
                        ))
        );

        assertEquals("attach_123", result.getId());
    }
}
