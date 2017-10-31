<%@page import="com.adobe.aem.importer.DocImporterHelper"%>
<%@page import="org.apache.sling.commons.json.io.JSONWriter"%>
<%@page import="java.util.Set"%>
<%@page contentType="text/html; charset=utf-8"%>
<%@include file="/libs/foundation/global.jsp"%>

<%
	response.setContentType("application/json");
response.setCharacterEncoding("utf-8");
Set<Class<?>> transformers = DocImporterHelper.getAvailableTransformers();
JSONWriter w = new JSONWriter(response.getWriter());
w.array();
for(Class<?> transformer : transformers) {
	w.object();
	w.key("value").value(transformer.getName());
	w.key("text").value(transformer.getSimpleName());
	w.endObject();
}
w.endArray();
%>
