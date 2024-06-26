package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Date lastResetTime = new Date();
    private long delay;


    private long getDiffTime() {
        return getCurrentTime() - lastResetTime.getTime();
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    private void changeCounterAndLastResetTime() {
        if (getDiffTime() >= delay) {
            requestCounter.set(0);
            lastResetTime = new Date(getCurrentTime());
        }
    }

    public void createDocument(Document document, String signature) throws InterruptedException, JsonProcessingException {
        delay = timeUnit.toMillis(1);

        synchronized (this) {
            changeCounterAndLastResetTime();

            while (requestCounter.get() >= requestLimit) {
                wait(delay - getDiffTime());
                changeCounterAndLastResetTime();
            }

            requestCounter.incrementAndGet();
        }

        String json = objectMapper.writeValueAsString(document);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Signature", signature)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws InterruptedException, JsonProcessingException {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);
        api.createDocument(new Document(), "Signature");
    }


    @Getter
    @Setter
    private static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private Date productionDate;
        private String productionType;
        private List<Product> products;
        private Date regDate;
        private String regNumber;
    }

    @Getter
    @Setter
    private static class Product {
        private String certificateDocument;
        private Date certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private Date productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }

    @Getter
    @Setter
    private static class Description {
        private String participantInn;
    }

}