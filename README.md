# SendKit Java SDK

Official Java SDK for the [SendKit](https://sendkit.dev) email API.

## Requirements

- Java 11+

## Installation

### Maven

```xml
<dependency>
    <groupId>dev.sendkit</groupId>
    <artifactId>sendkit</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'dev.sendkit:sendkit:1.0.0'
```

## Usage

```java
import dev.sendkit.SendKit;
import dev.sendkit.Emails;

import java.util.List;

SendKit client = new SendKit("your-api-key");

// Send an email
Emails.SendEmailResponse response = client.emails().send(
    new Emails.SendEmailParams("from@example.com", List.of("to@example.com"), "Hello")
        .html("<p>Hello world!</p>")
        .text("Hello world!")
);

System.out.println("Email sent: " + response.getId());
```

### Send with options

```java
Emails.SendEmailResponse response = client.emails().send(
    new Emails.SendEmailParams("from@example.com", List.of("to@example.com"), "Hello")
        .html("<p>Hello!</p>")
        .cc(List.of("cc@example.com"))
        .bcc(List.of("bcc@example.com"))
        .replyTo("reply@example.com")
        .tags(List.of("welcome"))
        .scheduledAt("2025-01-01T00:00:00Z")
        .attachments(List.of(
            new Emails.Attachment("file.pdf", "base64content")
                .contentType("application/pdf")
        ))
);
```

### Send MIME email

```java
Emails.SendMimeEmailResponse response = client.emails().sendMime(
    new Emails.SendMimeEmailParams("from@example.com", "to@example.com", rawMimeString)
);
```

### Error handling

```java
try {
    client.emails().send(params);
} catch (SendKitException e) {
    System.out.println(e.getMessage());    // "Invalid email"
    System.out.println(e.getName());       // "validation_error"
    System.out.println(e.getStatusCode()); // 422
}
```

## License

MIT
