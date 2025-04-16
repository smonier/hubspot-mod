package org.jahia.se.modules.hubspot.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.se.modules.hubspot.services.HubSpotService;
import org.jahia.services.render.URLResolver;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component(service = Action.class, immediate = true)
public class HubSpotAction extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(HubSpotAction.class);

    private HubSpotService hubSpotService;

    @Activate
    public void activate() {
        setName("hubspotAction");
        setRequireAuthenticatedUser(false);
        setRequiredMethods("GET,POST");

    }

    @Reference(service = HubSpotService.class)
    public void setHubSpotService(HubSpotService hubSpotService) {
        this.hubSpotService = hubSpotService;
    }

    @Override
    public ActionResult doExecute(
            HttpServletRequest request,
            RenderContext renderContext,
            Resource resource,
            JCRSessionWrapper session,
            Map<String, List<String>> parameters,
            URLResolver urlResolver) throws Exception {

        try {
            String method = request.getMethod();
            LOGGER.info("HubSpot Action triggered with method: {}", method);

            // Extract payload from the request
            StringBuilder payloadBuilder = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    payloadBuilder.append(line);
                }
            }
            String payload = payloadBuilder.toString();
            LOGGER.debug("Extracted payload: {}", payload);

            if (payload.isEmpty()) {
                LOGGER.warn("Payload is empty");
                return new ActionResult(400, null, new JSONObject().put("error", "Payload is required"));
            }

            JSONObject payloadJson = new JSONObject(payload);

            if ("POST".equalsIgnoreCase(method)) {
                JSONObject properties = payloadJson.getJSONObject("properties");
                Map<String, Object> leadData = convertJsonToMap(properties);
                Map<String, Object> response = hubSpotService.createLead(leadData);

                // Log the response for debugging
                LOGGER.info("Response from HubSpot API: {}", response);

                // Return response as JSON to the frontend
                return new ActionResult(201, null, new JSONObject(response));
            } else {
                return new ActionResult(405, null, new JSONObject().put("error", "Method not allowed"));
            }
        } catch (Exception e) {
            LOGGER.error("Error in HubSpot Action", e);
            return new ActionResult(500, null, new JSONObject().put("error", e.getMessage()));
        }
    }

    private ActionResult handlePost(JSONObject payloadJson) throws JSONException {
        try {
            LOGGER.info("Handling POST request with payload: {}", payloadJson);

            if (!payloadJson.has("properties")) {
                return new ActionResult(400, null, new JSONObject().put("error", "'properties' key is missing"));
            }

            JSONObject properties = payloadJson.getJSONObject("properties");
            Map<String, Object> leadData = convertJsonToMap(properties);

            // Call the HubSpot service
            Map<String, Object> hubspotResponse = hubSpotService.createLead(leadData);

            // Return success response
            JSONObject responseJson = new JSONObject();
            responseJson.put("message", "Lead created successfully");
            responseJson.put("hubspotResponse", hubspotResponse);

            LOGGER.debug("Returning response: {}", responseJson);
            return new ActionResult(200, null, responseJson);
        } catch (Exception e) {
            LOGGER.error("Error processing POST request", e);
            return new ActionResult(500, null, new JSONObject().put("error", e.getMessage()));
        }
    }

    private ActionResult handleGet(Map<String, List<String>> parameters) throws JSONException {
        try {
            LOGGER.info("Handling GET request with parameters: {}", parameters);

            String leadId = parameters.get("leadId") != null ? parameters.get("leadId").get(0) : null;
            if (leadId == null || leadId.isEmpty()) {
                return new ActionResult(400, null, new JSONObject().put("error", "Lead ID is required"));
            }

            // Fetch lead from HubSpot
            Map<String, Object> response = hubSpotService.getLeadById(leadId);

            return new ActionResult(200, null, new JSONObject(response));
        } catch (Exception e) {
            LOGGER.error("Error fetching lead from HubSpot", e);
            return new ActionResult(500, null, new JSONObject().put("error", e.getMessage()));
        }
    }

    private Map<String, Object> convertJsonToMap(JSONObject jsonObject) throws JSONException {
        Map<String, Object> map = new HashMap<>();
        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String key = it.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.put(key, convertJsonToMap((JSONObject) value)); // Recursive call for nested JSONObjects
            } else {
                map.put(key, value);
            }
        }
        return map;
    }
}