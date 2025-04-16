package org.jahia.se.modules.hubspot.services;

import java.util.List;
import java.util.Map;

public interface HubSpotService {
    Map<String, Object> createLead(Map<String, Object> leadData) throws Exception;
    Map<String, Object> getLeadById(String leadId) throws Exception;
    Map<String, Object> updateLead(String leadId, Map<String, Object> leadData) throws Exception;
    boolean deleteLead(String leadId) throws Exception;
    List<Map<String, Object>> getForms() throws Exception;
    String getPortalId();
}