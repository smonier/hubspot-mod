package org.jahia.se.modules.hubspot.services.impl;

import org.jahia.se.modules.hubspot.services.HubSpotService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component(
        service = {HubSpotService.class},
        configurationPid = "org.jahia.se.modules.hubspot.credentials",
        immediate = true
)
public class HubSpotServiceImpl implements HubSpotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HubSpotServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String hubspotUrl;
    private String defaultAuthorization;

    @Activate
    public void activate(Map<String, String> config) {
        String token = config.get("hubspot.token");
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("HubSpot token is not configured.");
        }

        String apiSchema = config.getOrDefault("hubspot.apiSchema", "https");
        String apiUrl = config.get("hubspot.apiUrl");
        String apiEndPoint = config.getOrDefault("hubspot.apiEndPoint", "/crm/v3/objects/contacts");

        if (apiUrl == null || apiUrl.isEmpty()) {
            throw new IllegalArgumentException("HubSpot API URL is not configured.");
        }

        defaultAuthorization = "Bearer " + token;
        hubspotUrl = String.format("%s://%s%s", apiSchema, apiUrl, apiEndPoint);

        LOGGER.info("Activated HubSpot Service with Base URL: {}", hubspotUrl);
    }

    @Override
    public Map<String, Object> createLead(Map<String, Object> leadData) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("properties", leadData);

        LOGGER.info("Sending POST request to HubSpot API with URL: {}", hubspotUrl);
        LOGGER.info("Payload: {}", objectMapper.writeValueAsString(payload));

        return sendRequest("POST", hubspotUrl, payload);
    }

    @Override
    public Map<String, Object> getLeadById(String leadId) throws Exception {
        String url = hubspotUrl + "/" + leadId;
        return sendRequest("GET", url, null);
    }

    @Override
    public Map<String, Object> updateLead(String leadId, Map<String, Object> leadData) throws Exception {
        String url = hubspotUrl + "/" + leadId;
        return sendRequest("PATCH", url, leadData);
    }

    @Override
    public boolean deleteLead(String leadId) throws Exception {
        String url = hubspotUrl + "/" + leadId;
        sendRequest("DELETE", url, null);
        return true;
    }

    private Map<String, Object> sendRequest(String method, String url, Map<String, Object> requestBody) throws Exception {
        LOGGER.info("Preparing to send {} request to URL: {}", method, url);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Authorization", defaultAuthorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        if ("POST".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            LOGGER.debug("Sending request with payload: {}", requestBody);
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                String jsonRequest = objectMapper.writeValueAsString(requestBody);
                os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            }
        }

        int statusCode = connection.getResponseCode();
        LOGGER.info("Received response with status code: {}", statusCode);

        InputStream inputStream = (statusCode >= 400) ? connection.getErrorStream() : connection.getInputStream();

        if (inputStream == null) {
            if (statusCode == 204) {
                LOGGER.warn("No content returned from the server (204 No Content).");
                return Map.of("message", "No content returned from the server");
            }
            throw new RuntimeException("No response from HubSpot API.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }

            String responseString = responseBuilder.toString();
            LOGGER.debug("Response body: {}", responseString);

            if (responseString.isEmpty()) {
                if (statusCode == 204) {
                    LOGGER.warn("Empty response body for 204 No Content.");
                    return Map.of("message", "No content returned from the server");
                }
                LOGGER.error("Empty response body received from HubSpot API.");
                throw new RuntimeException("Empty response received from HubSpot API.");
            }

            // Parse the response string as a Map and return it
            return objectMapper.readValue(responseString, HashMap.class);
        } catch (IOException e) {
            LOGGER.error("Error reading response from HubSpot API: {}", e.getMessage(), e);
            throw new RuntimeException("Error reading response from HubSpot API: " + e.getMessage());
        }
    }

    private void handleErrorStream(HttpURLConnection connection) throws IOException {
        try (InputStream errorStream = connection.getErrorStream();
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {

            StringBuilder errorBuilder = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorBuilder.append(line);
            }

            String errorResponse = errorBuilder.toString();
            LOGGER.error("Error response body: {}", errorResponse);
            throw new RuntimeException("Server returned an error: " + errorResponse);
        }
    }
}