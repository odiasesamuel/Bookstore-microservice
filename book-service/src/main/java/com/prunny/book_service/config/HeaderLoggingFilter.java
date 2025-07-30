package com.prunny.book_service.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class HeaderLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        System.out.println("📥 Incoming request to BookService:");
        request.getHeaderNames().asIterator().forEachRemaining(header ->
            System.out.println("🔸 " + header + ": " + request.getHeader(header))
        );

        filterChain.doFilter(request, response);
    }
}
