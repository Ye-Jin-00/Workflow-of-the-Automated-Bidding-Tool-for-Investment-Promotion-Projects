package com.sincit.bid.common;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 为每次请求生成 traceId，写入日志 MDC 与响应头，便于跨端排查问题。
 */
@Component
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put(TRACE_ID, traceId);
        try {
            if (response instanceof HttpServletResponse) {
                ((HttpServletResponse) response).setHeader("X-Trace-Id", traceId);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
