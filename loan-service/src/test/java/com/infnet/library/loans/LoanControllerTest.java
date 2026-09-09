package com.infnet.library.loans;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LoanControllerTest {

    private static HttpServer bookServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startBookServer() throws IOException {
        bookServer = HttpServer.create(new InetSocketAddress(0), 0);
        bookServer.createContext("/books/1", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        bookServer.createContext("/books/404", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        bookServer.createContext("/books/204", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        bookServer.createContext("/books/200", exchange -> {
            try {
                Thread.sleep(250);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        bookServer.createContext("/books/503", exchange -> exchange.close());
        bookServer.start();
    }

    @AfterAll
    static void stopBookServer() {
        bookServer.stop(0);
    }

    @DynamicPropertySource
    static void configureBookServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("BOOK_SERVICE_URL", () -> "http://localhost:" + bookServer.getAddress().getPort());
        registry.add("BOOK_SERVICE_CONNECT_TIMEOUT_MS", () -> 100);
        registry.add("BOOK_SERVICE_READ_TIMEOUT_MS", () -> 100);
    }

    @Test
    void createsLoanWhenBookServiceFindsBook() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"borrowerName\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.borrowerName").value("Ana"));
    }

    @Test
    void returnsNotFoundWhenBookServiceDoesNotFindBook() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":404,\"borrowerName\":\"Ana\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundWhenBookServiceReturnsNoContent() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":204,\"borrowerName\":\"Ana\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsServiceUnavailableWhenBookServiceResponseExceedsReadTimeout() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":200,\"borrowerName\":\"Ana\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void listsLoans() throws Exception {
        mockMvc.perform(get("/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getsAnExistingLoanById() throws Exception {
        String responseBody = mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"borrowerName\":\"Bruno\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode loan = objectMapper.readTree(responseBody);

        mockMvc.perform(get("/loans/" + loan.get("id").asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loan.get("id").asLong()));
    }

    @Test
    void returnsNotFoundForAnUnknownLoan() throws Exception {
        mockMvc.perform(get("/loans/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsRequestsWithMissingBookIdOrBlankBorrowerName() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowerName\":\"Ana\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1,\"borrowerName\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsServiceUnavailableWhenBookServiceCannotBeReached() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":503,\"borrowerName\":\"Ana\"}"))
                .andExpect(status().isServiceUnavailable());
    }
}
