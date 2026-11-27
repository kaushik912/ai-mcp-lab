package com.aicommerce.carrier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/** Logs every MCP JSON-RPC call hitting /mcp: method, tool name and latency. */
@Component
public class McpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("mcp.protocol");
    private static final Pattern METHOD = Pattern.compile("\"method\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TOOL = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/mcp");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(req, 64 * 1024);
        long start = System.nanoTime();
        try {
            chain.doFilter(wrapped, res);
        } finally {
            String body = new String(wrapped.getContentAsByteArray(), StandardCharsets.UTF_8);
            Matcher m = METHOD.matcher(body);
            String method = m.find() ? m.group(1) : "-";
            String tool = "";
            if ("tools/call".equals(method)) {
                Matcher t = TOOL.matcher(body.substring(body.indexOf("\"params\"") < 0 ? 0 : body.indexOf("\"params\"")));
                tool = t.find() ? " tool=" + t.group(1) : "";
            }
            log.info("[MCP] {} {} rpc={}{} status={} {}ms", req.getMethod(), req.getRequestURI(), method, tool,
                    res.getStatus(), (System.nanoTime() - start) / 1_000_000);
        }
    }
}
