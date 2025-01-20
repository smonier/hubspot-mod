package org.jahia.se.modules.hubspot.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.jahia.bin.filters.AbstractServletFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component(service = AbstractServletFilter.class, configurationPid = "org.jahia.se.modules.hubspot.credentials")
public class ProxyServlet extends AbstractServletFilter {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServlet.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String hubspotUrl;
    private String defaultAuthorization;

    @Activate
    public void activate(Map<String, String> config) {
        String token = config.get("hubspot.token");
        defaultAuthorization = "Bearer " + token;
        hubspotUrl = String.format("%s://%s", config.get("hubspot.apiSchema"), config.get("hubspot.apiUrl"));

        logger.info("Activated ProxyServlet with HubSpot Base URL: {}", hubspotUrl);
        setUrlPatterns(new String[]{"/hubspot/*"});
    }

    @Override
    public void init(FilterConfig filterConfig) {
        logger.debug("Initializing ProxyServlet with FilterConfig: {}", filterConfig);
    }

    @Override
    public void destroy() {
        logger.debug("Destroying ProxyServlet");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String method = request.getMethod();
        String path = request.getRequestURI().replaceFirst("/hubspot", ""); // Dynamically remove prefix
        String targetUrl = hubspotUrl + path;

        if ("GET".equalsIgnoreCase(method) && request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        logger.info("Received {} request to HubSpot URL: {}", method, targetUrl);

        String authorization = request.getHeader("Authorization") != null
                ? request.getHeader("Authorization")
                : defaultAuthorization;

        try {
            if ("POST".equalsIgnoreCase(method)) {
                handlePostRequest(request, response, targetUrl, authorization);
            } else if ("GET".equalsIgnoreCase(method)) {
                handleGetRequest(response, targetUrl, authorization);
            } else {
                logger.warn("Unsupported HTTP method: {}", method);
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not supported");
            }
        } catch (IOException e) {
            logger.error("Error handling request to HubSpot", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error communicating with HubSpot API");
        }
    }

    private void handlePostRequest(HttpServletRequest request, HttpServletResponse response, String targetUrl, String authorization) throws IOException {
        String requestBody = IOUtils.toString(request.getReader());
        logger.debug("POST Request Body: {}", requestBody);

        HttpURLConnection connection = createConnection(new URL(targetUrl), "POST", authorization);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        handleResponse(response, connection);
    }

    private void handleGetRequest(HttpServletResponse response, String targetUrl, String authorization) throws IOException {
        HttpURLConnection connection = createConnection(new URL(targetUrl), "GET", authorization);
        handleResponse(response, connection);
    }

    private HttpURLConnection createConnection(URL url, String method, String authorization) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Authorization", authorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Accept-Language", "en");
        connection.setDoOutput("POST".equalsIgnoreCase(method));
        return connection;
    }

    private void handleResponse(HttpServletResponse response, HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        response.setStatus(status);

        try (InputStream inputStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
            if (inputStream == null) {
                response.getWriter().write("{}"); // Empty response body
                return;
            }

            String responseBody = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            logger.debug("HubSpot API Response: {}", responseBody);
            response.getWriter().write(responseBody);
        } catch (IOException e) {
            logger.error("Error reading HubSpot API response", e);
            throw e;
        }
    }
}