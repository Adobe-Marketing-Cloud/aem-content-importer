/*******************************************************************************
 * Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
 *
 * Licensed under the Apache License 2.0.
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/
package com.adobe.aem.importer.servlet;

import com.adobe.aem.importer.DocImporter;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;


@SlingServlet(
    label = "DocImporter Servlet",
    description = "Servlet for uploading a DITA or DocBook document set as zip file",
    methods = {"POST"},
    resourceTypes = {"aem-importer/components/upload-content"})
@org.apache.felix.scr.annotations.Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "AEM Content Importer - Servlet for uploading document set"),
    @Property(name = Constants.SERVICE_VENDOR, value = "Adobe")})
public class UploadContentServlet extends SlingAllMethodsServlet {

    private static Logger log = LoggerFactory.getLogger(UploadContentServlet.class);

    @Reference
    private ContentImporter contentImporter;

    @Reference
    private DocImporter docImporter;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        try {
            InputStream is = request.getRequestParameter("file").getInputStream();
            Resource resource = request.getResource();
            Node resourceNode = resource.adaptTo(Node.class);
            Session session = resourceNode.getSession();

            if(session.itemExists(DocImporter.ZIP_UPLOAD_FOLDER_PATH)){
                session.removeItem(DocImporter.ZIP_UPLOAD_FOLDER_PATH);
                session.save();
            }
            Node parentNode = JcrUtil.createPath(DocImporter.ZIP_UPLOAD_FOLDER_PATH, "nt:folder", "nt:folder", session, true);

            this.contentImporter.importContent(
                parentNode,
                DocImporter.ZIP_FILE_NAME,
                is,
                new ImportOptions() {
                    @Override
                    public boolean isCheckin() {
                        return false;
                    }

                    @Override
                    public boolean isAutoCheckout() {
                        return false;
                    }

                    @Override
                    public boolean isIgnoredImportProvider(String extension) {
                        return false;
                    }

                    @Override
                    public boolean isOverwrite() {
                        return true;
                    }

                    @Override
                    public boolean isPropertyOverwrite() {
                        return true;
                    }
                },
                null);

            docImporter.doImport(DocImporter.GIT_REPOS_FOLDER_NAME);

        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }
    }
}
