package com.infnet.library.loans;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class BookClient {

    private final RestClient restClient;

    public BookClient(
            @Value("${BOOK_SERVICE_URL:http://localhost:8081}") String bookServiceUrl,
            @Value("${BOOK_SERVICE_CONNECT_TIMEOUT_MS:2000}") int connectTimeoutMillis,
            @Value("${BOOK_SERVICE_READ_TIMEOUT_MS:2000}") int readTimeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);
        this.restClient = RestClient.builder()
                .baseUrl(bookServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean bookExists(long bookId) {
        try {
            ResponseEntity<Void> response = restClient.get()
                    .uri("/books/{id}", bookId)
                    .retrieve()
                    .toBodilessEntity();
            return response.getStatusCode() == HttpStatus.OK;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw new BookServiceUnavailableException(exception);
        } catch (RestClientException exception) {
            throw new BookServiceUnavailableException(exception);
        }
    }

    static class BookServiceUnavailableException extends RuntimeException {

        BookServiceUnavailableException(Throwable cause) {
            super(cause);
        }
    }
}
