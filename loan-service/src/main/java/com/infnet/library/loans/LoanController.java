package com.infnet.library.loans;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanRepository repository;
    private final BookClient bookClient;

    public LoanController(LoanRepository repository, BookClient bookClient) {
        this.repository = repository;
        this.bookClient = bookClient;
    }

    @GetMapping
    public List<Loan> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Loan> getById(@PathVariable long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Loan> create(@Valid @RequestBody LoanRequest request) {
        if (!bookClient.bookExists(request.bookId())) {
            return ResponseEntity.notFound().build();
        }
        Loan loan = repository.create(request.bookId(), request.borrowerName());
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    @ExceptionHandler(BookClient.BookServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    void handleBookServiceUnavailable() {
    }

    public record LoanRequest(@NotNull Long bookId, @NotBlank String borrowerName) {
    }
}
