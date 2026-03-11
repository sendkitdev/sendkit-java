package dev.sendkit;

import java.util.List;
import java.util.Map;

public class Emails {

    private final SendKit client;

    Emails(SendKit client) {
        this.client = client;
    }

    public SendEmailResponse send(SendEmailParams params) throws SendKitException {
        return client.post("/emails", params, SendEmailResponse.class);
    }

    public SendMimeEmailResponse sendMime(SendMimeEmailParams params) throws SendKitException {
        return client.post("/emails/mime", params, SendMimeEmailResponse.class);
    }

    public static class SendEmailParams {
        private final String from;
        private final List<String> to;
        private final String subject;
        private String html;
        private String text;
        private List<String> cc;
        private List<String> bcc;
        private String replyTo;
        private Map<String, String> headers;
        private List<String> tags;
        private String scheduledAt;
        private List<Attachment> attachments;

        public SendEmailParams(String from, List<String> to, String subject) {
            this.from = from;
            this.to = to;
            this.subject = subject;
        }

        public SendEmailParams(String from, String to, String subject) {
            this(from, List.of(to), subject);
        }

        public SendEmailParams html(String html) { this.html = html; return this; }
        public SendEmailParams text(String text) { this.text = text; return this; }
        public SendEmailParams cc(List<String> cc) { this.cc = cc; return this; }
        public SendEmailParams bcc(List<String> bcc) { this.bcc = bcc; return this; }
        public SendEmailParams replyTo(String replyTo) { this.replyTo = replyTo; return this; }
        public SendEmailParams headers(Map<String, String> headers) { this.headers = headers; return this; }
        public SendEmailParams tags(List<String> tags) { this.tags = tags; return this; }
        public SendEmailParams scheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; return this; }
        public SendEmailParams attachments(List<Attachment> attachments) { this.attachments = attachments; return this; }
    }

    public static class Attachment {
        private final String filename;
        private final String content;
        private String contentType;

        public Attachment(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }

        public Attachment contentType(String contentType) { this.contentType = contentType; return this; }
    }

    public static class SendEmailResponse {
        private String id;

        public String getId() { return id; }
    }

    public static class SendMimeEmailParams {
        private final String envelopeFrom;
        private final String envelopeTo;
        private final String rawMessage;

        public SendMimeEmailParams(String envelopeFrom, String envelopeTo, String rawMessage) {
            this.envelopeFrom = envelopeFrom;
            this.envelopeTo = envelopeTo;
            this.rawMessage = rawMessage;
        }
    }

    public static class SendMimeEmailResponse {
        private String id;

        public String getId() { return id; }
    }
}
