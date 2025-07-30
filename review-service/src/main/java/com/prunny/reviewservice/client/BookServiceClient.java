package com.prunny.reviewservice.client;

import com.prunny.reviewservice.service.dto.BookDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "bookservice")
public interface BookServiceClient {
    @GetMapping("/api/books/isbn/{isbn}")
    BookDTO getBookByIsbn(@PathVariable("isbn") String isbn);
}
