package com.oms.api.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class MdcFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String CUSTOMER_ID_HEADER = "X-Customer-Id";

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_CUSTOMER_ID = "customerId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }

            String traceId = request.getHeader(TRACE_ID_HEADER);
            String customerId = request.getHeader(CUSTOMER_ID_HEADER);

            MDC.put(MDC_REQUEST_ID, requestId);
            if (traceId != null && !traceId.isBlank()) {
                MDC.put(MDC_TRACE_ID, traceId);
            }
            if (customerId != null && !customerId.isBlank()) {
                MDC.put(MDC_CUSTOMER_ID, customerId);
            }

            response.setHeader(REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
