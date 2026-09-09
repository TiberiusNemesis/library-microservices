package com.infnet.library.books;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
public class BookRepository {

    private final ConcurrentMap<Long, Book> books = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public BookRepository() {
        create("Clean Code", "Robert C. Martin");
    }

    public List<Book> findAll() {
        return books.values().stream()
                .sorted(java.util.Comparator.comparing(Book::id))
                .toList();
    }

    public Optional<Book> findById(long id) {
        return Optional.ofNullable(books.get(id));
    }

    public Book create(String title, String author) {
        long id = nextId.getAndIncrement();
        Book book = new Book(id, title, author);
        books.put(id, book);
        return book;
    }
}
