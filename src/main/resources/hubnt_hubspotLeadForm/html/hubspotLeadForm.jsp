<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<jsp:useBean id="random" class="java.util.Random" scope="application"/>

<%--Add files used by the webapp--%>
<template:addResources type="css" resources="webapp/${requestScope.webappCssFileName}" media="screen"/>
<template:addResources type="css" resources="primereact-overrides.css"/>

<template:addResources type="javascript" resources="webapp/${requestScope.webappJsFileName}"/>

<c:set var="_uuid_" value="${currentNode.identifier}"/>
<c:set var="language" value="${currentResource.locale.language}"/>
<c:set var="workspace" value="${renderContext.workspace}"/>
<c:set var="isEdit" value="${renderContext.editMode}"/>

<c:set var="site" value="${renderContext.site.siteKey}"/>
<c:set var="host" value="${url.server}"/>

<c:set var="targetId" value="REACT_Hubspot_${fn:replace(random.nextInt(),'-','_')}"/>
<jcr:node var="user" path="${renderContext.user.localPath}"/>


<div id="${targetId}"></div>

<script>
    const hubspot_context_${targetId} = {
        host: "${host}",
        workspace: "${workspace}",
        isEdit:${isEdit},
        scope: "${site}",//site key
        locale: "${language}",
        hubspotId: "${_uuid_}",
        gqlServerUrl: "${host}/modules/graphql",
        contextServerUrl: window.digitalData ? window.digitalData.contextServerPublicUrl : undefined,
        actionUrl: "${host}/hotspotAction",//digitalData is set in live mode only
    };

    window.addEventListener("DOMContentLoaded", (event) => {
        const callHubspotApp = () => {
            if (typeof window.hubspotUIApp === 'function') {
                window.hubspotUIApp("${targetId}", hubspot_context_${targetId});
            } else {
                console.error("Error: window.hubspotUIApp is not defined or is not a function.");
            }
        };

        <c:choose>
        <c:when test="${isEdit}">
        setTimeout(callHubspotApp, 500); // Delayed execution in edit mode
        </c:when>
        <c:otherwise>
        callHubspotApp(); // Immediate execution in non-edit mode
        </c:otherwise>
        </c:choose>
    });
</script>
