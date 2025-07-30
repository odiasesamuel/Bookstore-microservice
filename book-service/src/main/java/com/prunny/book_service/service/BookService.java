package com.prunny.book_service.service;

import com.prunny.book_service.domain.Book;
import com.prunny.book_service.repository.BookRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.prunny.book_service.domain.Book}.
 */
@Service
@Transactional
public class BookService {

    private static final Logger LOG = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Save a book.
     *
     * @param book the entity to save.
     * @return the persisted entity.
     */
    public Book save(Book book) {
        LOG.debug("Request to save Book : {}", book);
        return bookRepository.save(book);
    }

    /**
     * Update a book.
     *
     * @param book the entity to save.
     * @return the persisted entity.
     */
    public Book update(Book book) {
        LOG.debug("Request to update Book : {}", book);
        return bookRepository.save(book);
    }

    /**
     * Partially update a book.
     *
     * @param book the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<Book> partialUpdate(Book book) {
        LOG.debug("Request to partially update Book : {}", book);

        return bookRepository
            .findById(book.getId())
            .map(existingBook -> {
                if (book.getTitle() != null) {
                    existingBook.setTitle(book.getTitle());
                }
                if (book.getAuthor() != null) {
                    existingBook.setAuthor(book.getAuthor());
                }
                if (book.getIsbn() != null) {
                    existingBook.setIsbn(book.getIsbn());
                }
                if (book.getPrice() != null) {
                    existingBook.setPrice(book.getPrice());
                }
                if (book.getPublishedDate() != null) {
                    existingBook.setPublishedDate(book.getPublishedDate());
                }
                if (book.getAvailableCopies() != null) {
                    existingBook.setAvailableCopies(book.getAvailableCopies());
                }

                return existingBook;
            })
            .map(bookRepository::save);
    }

    /**
     * Get all the books.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<Book> findAll(Pageable pageable) {
        LOG.debug("Request to get all Books");
        return bookRepository.findAll(pageable);
    }

    /**
     * Get one book by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Book> findOne(Long id) {
        LOG.debug("Request to get Book : {}", id);
        return bookRepository.findById(id);
    }

    /**
     * Get one book by isbn.
     *
     * @param isbn the isbn of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Book> findOneByIsbn(String isbn) {
        LOG.debug("Request to get Book : {}", isbn);
        return bookRepository.findByIsbn(isbn);
    }

    /**
     * Delete the book by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Book : {}", id);
        bookRepository.deleteById(id);
    }
}
