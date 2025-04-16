package org.jahia.se.modules.hubspot.initializers;

import org.jahia.osgi.BundleUtils;
import org.jahia.se.modules.hubspot.services.HubSpotService;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.ValueImpl;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.jahia.services.content.nodetypes.renderer.ModuleChoiceListRenderer;
import org.jahia.services.render.RenderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.*;

public class HubspotFormsChoiceListInitializer implements ModuleChoiceListInitializer, ModuleChoiceListRenderer {


    private static final Logger logger = LoggerFactory.getLogger(HubspotFormsChoiceListInitializer.class);
    private String key;

    @Override
    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public List<ChoiceListValue> getChoiceListValues(ExtendedPropertyDefinition epd, String param,
                                                     List<ChoiceListValue> values, Locale locale,
                                                     Map<String, Object> context) {

        List<ChoiceListValue> choiceList = new ArrayList<>();

        try {
            HubSpotService hubSpotService = BundleUtils.getOsgiService(HubSpotService.class, null);
            if (hubSpotService == null) {
                logger.warn("HubSpotService is not available (null). Returning empty form list.");
                return choiceList;
            }

            List<Map<String, Object>> forms = hubSpotService.getForms();
            String portalId = hubSpotService.getPortalId();

            for (Map<String, Object> form : forms) {
                String name = (String) form.get("name");
                String guid = (String) form.get("guid");

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("formId", guid);

                String embedCode = "<script charset=\"utf-8\" type=\"text/javascript\" src=\"//js.hsforms.net/forms/embed/v2.js\"></script>\n" +
                        "<script>hbspt.forms.create({region: \"eu1\", portalId: \"" + portalId + "\", formId: \"" + guid + "\"});</script>";
                metadata.put("embedCode", embedCode);

                choiceList.add(new ChoiceListValue(name, metadata, new ValueImpl(guid, PropertyType.STRING, false)));
            }

        } catch (Exception e) {
            logger.error("Failed to fetch HubSpot forms for dropdown", e);
        }

        return choiceList;
    }

    public Map<String, Object> getObjectRendering(RenderContext context, ExtendedPropertyDefinition propDef, Object propertyValue) throws RepositoryException {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("displayName", propertyValue.toString());

        return map;
    }

    public String getStringRendering(RenderContext context, ExtendedPropertyDefinition propDef, Object propertyValue) throws RepositoryException {

        return propertyValue.toString();
    }

    public String getStringRendering(RenderContext context, JCRPropertyWrapper propertyWrapper) throws RepositoryException {
        return propertyWrapper.getValue().getString();
    }

    public Map<String, Object> getObjectRendering(RenderContext context, JCRPropertyWrapper propertyWrapper) throws RepositoryException {

        Map<String, Object> map = new HashMap<String, Object>();

        map.put("displayName", propertyWrapper.getValue().getString());

        return map;
    }

    public String getStringRendering(Locale locale, ExtendedPropertyDefinition propDef, Object propertyValue) throws RepositoryException {

        return propertyValue.toString();
    }

    public Map<String, Object> getObjectRendering(Locale locale, ExtendedPropertyDefinition propDef, Object propertyValue) throws RepositoryException {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("displayName", propertyValue.toString());

        return map;
    }
}