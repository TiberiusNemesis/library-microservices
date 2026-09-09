package com.infnet.library.loans;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
public class LoanRepository {

    private final ConcurrentMap<Long, Loan> loans = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public List<Loan> findAll() {
        return loans.values().stream()
                .sorted(Comparator.comparing(Loan::id))
                .toList();
    }

    public Optional<Loan> findById(long id) {
        return Optional.ofNullable(loans.get(id));
    }

    public Loan create(long bookId, String borrowerName) {
        long id = nextId.getAndIncrement();
        Loan loan = new Loan(id, bookId, borrowerName);
        loans.put(id, loan);
        return loan;
    }
}
