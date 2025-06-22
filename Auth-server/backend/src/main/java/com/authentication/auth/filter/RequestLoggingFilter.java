package com.authentication.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

public class RequestLoggingFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // Using System.out.println to ensure visibility regardless of logging config
        System.out.println("--- [RequestLoggingFilter] Processing request: " + httpRequest.getMethod() + " " + httpRequest.getRequestURI() + " ---");
        chain.doFilter(request, response);
    }
}
