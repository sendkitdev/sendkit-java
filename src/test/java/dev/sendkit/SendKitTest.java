package dev.sendkit;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

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
        server.createContext("/emails", exchange -> {
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
    void testSendEmailWithSingleStringTo() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"to\":[\"to@example.com\"]"));

            String response = "{\"id\":\"single_to_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", "to@example.com", "Hello")
                        .html("<p>Hi</p>")
        );

        assertEquals("single_to_123", result.getId());
    }

    @Test
    void testSendEmailWithDisplayName() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("Bob \\u003cto@example.com\\u003e") || body.contains("Bob <to@example.com>"));

            String response = "{\"id\":\"display_name_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", "Bob <to@example.com>", "Hello")
                        .html("<p>Hi</p>")
        );

        assertEquals("display_name_123", result.getId());
    }

    @Test
    void testSendEmailWithAllOptions() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"cc\":[\"cc@example.com\"]"));
            assertTrue(body.contains("\"bcc\":[\"bcc@example.com\"]"));
            assertTrue(body.contains("\"reply_to\":[\"reply@example.com\"]"));
            assertTrue(body.contains("\"name\":\"category\""));
            assertTrue(body.contains("\"value\":\"welcome\""));
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
                        .tags(List.of(new Emails.Tag("category", "welcome")))
                        .scheduledAt("2025-01-01T00:00:00Z")
        );

        assertEquals("email_456", result.getId());
    }

    @Test
    void testSendMimeEmail() {
        server.createContext("/emails/mime", exchange -> {
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
        server.createContext("/emails", exchange -> {
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
        server.createContext("/emails", exchange -> {
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
        server.createContext("/emails", exchange -> {
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

    @Test
    void testNullFieldsAreNotSerialized() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertFalse(body.contains("\"text\""), "null text field should not be serialized");
            assertFalse(body.contains("\"cc\""), "null cc field should not be serialized");
            assertFalse(body.contains("\"bcc\""), "null bcc field should not be serialized");
            assertFalse(body.contains("\"reply_to\""), "null reply_to field should not be serialized");
            assertFalse(body.contains("\"headers\""), "null headers field should not be serialized");
            assertFalse(body.contains("\"tags\""), "null tags field should not be serialized");
            assertFalse(body.contains("\"scheduled_at\""), "null scheduled_at field should not be serialized");
            assertFalse(body.contains("\"attachments\""), "null attachments field should not be serialized");

            String response = "{\"id\":\"no_nulls_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", "to@example.com", "Hello")
                        .html("<p>Hi</p>")
        );

        assertEquals("no_nulls_123", result.getId());
    }

    @Test
    void testSendEmailWithMultipleToRecipients() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"to\":[\"a@example.com\",\"b@example.com\"]"));

            String response = "{\"id\":\"multi_to_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", List.of("a@example.com", "b@example.com"), "Hello")
                        .html("<p>Hi</p>")
        );

        assertEquals("multi_to_123", result.getId());
    }

    @Test
    void testSendEmailWithHeaders() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"headers\":{\"X-Custom\":\"value\",\"X-Another\":\"test\"}") ||
                    body.contains("\"headers\":{\"X-Another\":\"test\",\"X-Custom\":\"value\"}"));

            String response = "{\"id\":\"headers_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", "to@example.com", "Hello")
                        .html("<p>Hi</p>")
                        .headers(Map.of("X-Custom", "value", "X-Another", "test"))
        );

        assertEquals("headers_123", result.getId());
    }

    @Test
    void testSendEmailWithTags() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"name\":\"category\""));
            assertTrue(body.contains("\"value\":\"welcome\""));
            assertTrue(body.contains("\"name\":\"campaign\""));
            assertTrue(body.contains("\"value\":\"onboarding\""));

            String response = "{\"id\":\"tags_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", "to@example.com", "Hello")
                        .html("<p>Hi</p>")
                        .tags(List.of(new Emails.Tag("category", "welcome"), new Emails.Tag("campaign", "onboarding")))
        );

        assertEquals("tags_123", result.getId());
    }

    @Test
    void testSendEmailWithReplyToList() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"reply_to\":[\"reply1@example.com\",\"reply2@example.com\"]"));

            String response = "{\"id\":\"reply_list_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", "to@example.com", "Hello")
                        .html("<p>Hi</p>")
                        .replyTo(List.of("reply1@example.com", "reply2@example.com"))
        );

        assertEquals("reply_list_123", result.getId());
    }

    @Test
    void testSendEmailWithCcSingleString() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"cc\":[\"cc@example.com\"]"));

            String response = "{\"id\":\"cc_single_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", "to@example.com", "Hello")
                        .html("<p>Hi</p>")
                        .cc("cc@example.com")
        );

        assertEquals("cc_single_123", result.getId());
    }

    @Test
    void testSendEmailWithBccSingleString() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"bcc\":[\"bcc@example.com\"]"));

            String response = "{\"id\":\"bcc_single_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", "to@example.com", "Hello")
                        .html("<p>Hi</p>")
                        .bcc("bcc@example.com")
        );

        assertEquals("bcc_single_123", result.getId());
    }

    @Test
    void testSendEmailWithReplyToSingleString() {
        server.createContext("/emails", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertTrue(body.contains("\"reply_to\":[\"reply@example.com\"]"));

            String response = "{\"id\":\"reply_single_123\"}";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        Emails.SendEmailResponse result = client().emails().send(
                new Emails.SendEmailParams("sender@example.com", "to@example.com", "Hello")
                        .html("<p>Hi</p>")
                        .replyTo("reply@example.com")
        );

        assertEquals("reply_single_123", result.getId());
    }
}
