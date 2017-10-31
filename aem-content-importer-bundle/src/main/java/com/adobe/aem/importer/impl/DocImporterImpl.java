/*******************************************************************************
 * Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
 *
 * Licensed under the Apache License 2.0.
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/

package com.adobe.aem.importer.impl;

import com.adobe.aem.importer.DocImporter;
import com.adobe.aem.importer.xml.FilterXmlBuilder;
import com.adobe.aem.importer.xml.RejectingEntityResolver;
import com.day.cq.commons.jcr.JcrUtil;
import net.sf.saxon.Configuration;
import net.sf.saxon.jaxp.TransformerImpl;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.config.ConfigurationException;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.Importer;
import org.apache.jackrabbit.vault.fs.io.JcrArchive;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

@Component
@org.apache.felix.scr.annotations.Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "AEM Content Importer"),
    @Property(name = Constants.SERVICE_VENDOR, value = "Adobe")})
@Service(value = DocImporter.class)
public class DocImporterImpl implements DocImporter {
    private static final Logger log = LoggerFactory.getLogger(DocImporterImpl.class);
    private Session session;

    private String xsltFilePath;
    private String masterFileName;
    private String graphicsFolderName;
    private String targetPath;

    private Node importRootNode;
    private Node sourceFolderNode;
    private Properties properties;

    @Reference
    private SlingRepository slingRepository;

    @Activate
    protected final void activate(final Map<String, String> properties) throws Exception {}

    @Deactivate
    protected final void deactivate(final Map<String, String> properties) {}

    private boolean initImport(String gitRepoJcrPath){
        try {
            this.session = slingRepository.loginAdministrative(null);
            if (!this.session.nodeExists(gitRepoJcrPath)){
                log.info("[docs-git-importer] Import root path " + gitRepoJcrPath + " not found!");
                return false;
            }
            log.info("[docs-git-importer] Import root path: " + gitRepoJcrPath);

            this.importRootNode = this.session.getNode(gitRepoJcrPath);
            if (!this.importRootNode.hasNode(DocImporter.CONFIG_FILE_NAME))
            {
                log.info("[docs-git-importer] Config file " + DocImporter.CONFIG_FILE_NAME + " not found!");
                return false;
            }
            log.info("[docs-git-importer] Config file: " + DocImporter.CONFIG_FILE_NAME);

            this.properties = new Properties();
            this.properties.loadFromXML(JcrUtils.readFile(this.importRootNode.getNode(DocImporter.CONFIG_FILE_NAME)));
            String sourceFolder = properties.getProperty(DocImporter.CONFIG_PARAM_SOURCE_FOLDER, DocImporter.DEFAULT_SOURCE_FOLDER);
            if (!this.importRootNode.hasNode(sourceFolder)) {
                log.info("[docs-git-importer] Source folder " + sourceFolder + " not found!");
                return false;
            }
            log.info("[docs-git-importer] Source folder: " + sourceFolder);
            this.sourceFolderNode = importRootNode.getNode(sourceFolder);

            this.masterFileName = properties.getProperty(DocImporter.CONFIG_PARAM_MASTER_FILE, DocImporter.DEFAULT_MASTER_FILE);
            if (!this.sourceFolderNode.hasNode(this.masterFileName)){
                log.info("[docs-git-importer] Master file " + this.masterFileName + " not found!");
                return false;
            }
            log.info("[docs-git-importer] Master file: " + this.masterFileName);

            this.graphicsFolderName = properties.getProperty(DocImporter.CONFIG_PARAM_GRAPHICS_FOLDER, DocImporter.DEFAULT_GRAPHICS_FOLDER);
            log.info("[docs-git-importer] Graphics folder: " + this.graphicsFolderName);

            this.targetPath = properties.getProperty(DocImporter.CONFIG_PARAM_TARGET_PATH, DocImporter.DEFAULT_TARGET_PATH);
            log.info("[docs-git-importer] Target path: " + this.targetPath);

            String sourceFormat = this.properties.getProperty(DocImporter.CONFIG_PARAM_SOURCE_FORMAT, DocImporter.DEFAULT_SOURCE_FORMAT);
            log.info("[docs-git-importer] Source format: " + sourceFormat);

            if (sourceFormat.equalsIgnoreCase(DocImporter.SOURCE_FORMAT_DOCBOOK)) {
                this.xsltFilePath = DocImporter.DOCBOOK_XSLT_PATH;
            } else {
                this.xsltFilePath = DocImporter.DITA_XSLT_PATH;
            }

            // Remove any existing imported content
            log.info("[docs-git-importer] Checking for existing imported content at: " + targetPath);
            if (session.nodeExists(targetPath)) {
                session.getNode(targetPath).remove();
                log.info("[docs-git-importer] Removed existing imported content at: " + targetPath);
            }

        } catch(RepositoryException e) {
            log.error("[docs-git-importer] ", e);
        } catch (IOException e) {
            log.error("[docs-git-importer] ", e);
        }
        return true;
    }

    public void doImport(String gitRepoJcrPath) {
        try {
            if (!initImport(gitRepoJcrPath)){
                log.info("[docs-git-importer] initImport failed!");
                return;
            }

            Node xsltNode = this.session.getNode(xsltFilePath);
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setEntityResolver(new RejectingEntityResolver());
            URIResolver uriResolver = new DocImporterURIResolver(xsltNode, this.sourceFolderNode, xmlReader);
            TransformerFactoryImpl transformerFactoryImpl = new TransformerFactoryImpl();
            transformerFactoryImpl.setURIResolver(uriResolver);
            Transformer transformer = transformerFactoryImpl.newTransformer(new StreamSource(JcrUtils.readFile(xsltNode)));
            TransformerImpl transformerImpl = (TransformerImpl) transformer;
            transformerImpl.getUnderlyingController().setUnparsedTextURIResolver(new DocImporterUnparsedTextURIResolver(this.sourceFolderNode));

            for (Entry<Object, Object> entry : properties.entrySet()) {
                transformer.setParameter(entry.getKey().toString(), entry.getValue());
                log.info("[docs-git-importer] transformer.setParameter: " + entry.getKey().toString() + " = " + entry.getValue());
            }
            transformer.setParameter("xsltFilePath", this.xsltFilePath);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            transformer.transform(new SAXSource(xmlReader, new InputSource(JcrUtils.readFile(this.sourceFolderNode.getNode(masterFileName)))), new StreamResult(output));
            InputStream result = new ByteArrayInputStream(output.toByteArray());

            if (this.session.itemExists(DocImporter.CONTENT_PACKAGE_PATH)){
                this.session.removeItem(DocImporter.CONTENT_PACKAGE_PATH);
                this.session.save();
            }

            Node contentPackageNode = JcrUtils.getOrCreateByPath(DocImporter.CONTENT_PACKAGE_PATH, "nt:folder", "nt:folder", this.session, true);
            this.session.getWorkspace().copy(DocImporter.CONTENT_PACKAGE_TEMPLATE_PATH + "/META-INF", contentPackageNode.getPath() + "/META-INF");
            Node vaultNode = contentPackageNode.getNode("META-INF/vault");
            Node contentXMLNode = JcrUtil.createPath(contentPackageNode.getPath() + "/jcr_root" + this.targetPath, "nt:folder", "nt:folder", this.session, true);
            JcrUtils.putFile(contentXMLNode, ".content.xml", "application/xml", result);

            if (this.graphicsFolderName != null && this.sourceFolderNode.hasNode(this.graphicsFolderName)) {
                JcrUtil.copy(this.sourceFolderNode.getNode(graphicsFolderName), contentXMLNode, this.graphicsFolderName);
            }

            JcrUtils.putFile(vaultNode, "filter.xml", "application/xml", FilterXmlBuilder.fromRoot(this.targetPath + "/").toStream(this.graphicsFolderName));
            JcrArchive archive = new JcrArchive(contentPackageNode, "/");
            archive.open(true);
            Importer importer = new Importer();
            importer.getOptions().setImportMode(ImportMode.REPLACE);
            importer.getOptions().setAccessControlHandling(AccessControlHandling.MERGE);
            importer.run(archive, contentPackageNode.getSession().getNode("/"));
            this.session.save();
            log.info("[docs-git-importer] session saved.");

        } catch(RepositoryException e) {
            log.error("[docs-git-importer] ", e);
        } catch (TransformerException e) {
            log.error("[docs-git-importer] ", e);
        } catch (SAXException e){
            log.error("[docs-git-importer] ", e);
        } catch (IOException e) {
            log.error("[docs-git-importer] ", e);
        } catch (ConfigurationException e){
            log.error("[docs-git-importer] ", e);
        }
    }

    private class DocImporterURIResolver implements URIResolver {
        private Node xsltNode;
        private Node srcNode;
        private XMLReader xmlReader;

        public DocImporterURIResolver(Node xsltNode, Node srcNode, XMLReader xmlReader) {
            this.xsltNode = xsltNode;
            this.srcNode = srcNode;
            this.xmlReader = xmlReader;
        }

        public Source resolve(String href, String base) throws TransformerException {
            try {
                final Node node = (href.endsWith("xsl") ? this.xsltNode.getParent().getNode(href) : this.srcNode.getNode(href));
                return new SAXSource(this.xmlReader, new InputSource(JcrUtils.readFile(node)));
            } catch (RepositoryException e) {
                throw new TransformerException("Cannot resolve " + href + " in either [parent of " + this.xsltNode + " or " + this.srcNode + "]");
            }
        }
    }

    private class DocImporterUnparsedTextURIResolver implements UnparsedTextURIResolver {
        private Node srcNode;

        public DocImporterUnparsedTextURIResolver(Node srcNode) {
            this.srcNode = srcNode;
        }

        public Reader resolve(URI absoluteURI, String encoding, Configuration config) throws net.sf.saxon.trans.XPathException {
            String absolutePath = absoluteURI.getPath();
            InputStreamReader isr;

            // Hardcoded hack, requires that HTML files are always in the html/ subdir of the src/ dir
            int pos = absolutePath.lastIndexOf("html/");
            String relativePath = absolutePath.substring(pos);

            try {
                if(this.srcNode.hasNode(relativePath)) {
                    isr = new InputStreamReader(JcrUtils.readFile(this.srcNode.getNode(relativePath)));
                } else {
                    String message = "<html><body><h2>HTML file " + relativePath + " not found<h2></body></html>";
                    isr = new InputStreamReader(IOUtils.toInputStream(message, "UTF-8"));
                    log.info("HTML file " + relativePath + " not found");
                }
                return isr;
            } catch (RepositoryException e) {
                throw new net.sf.saxon.trans.XPathException("Oops...", e);
            } catch (IOException e) {
                throw new net.sf.saxon.trans.XPathException("Oops...", e);
            }
        }
    }
}