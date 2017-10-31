/*******************************************************************************
 * Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
 *
 * Licensed under the Apache License 2.0.
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/
package com.adobe.aem.importer.impl;

import com.adobe.aem.importer.DocImporter;
import com.adobe.aem.importer.GitListener;
import com.adobe.granite.codesharing.Directory;
import com.adobe.granite.codesharing.File;
import com.adobe.granite.codesharing.Project;
import com.adobe.granite.codesharing.github.GitHubPushEvent;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.webdav.util.EncodeUtil;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import com.adobe.granite.codesharing.github.GitHubPushConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Properties;


@Component(
    label = "Document Importer Git Listener",
    description = "Listens for push events on linked Git repository and imports DITA or DocBook documentation changes",
    immediate = true
)
@org.apache.felix.scr.annotations.Properties({
    @Property(
        label = "Event Topics",
        value = {GitHubPushConstants.EVT_TOPIC},
        description = "This event handler responds to Git Push Events.",
        name = EventConstants.EVENT_TOPIC,
        propertyPrivate = true
    )
})
@Service
public class GitListenerImpl implements GitListener {
    private static Logger log = LoggerFactory.getLogger(GitListenerImpl.class);

    @Reference
    private SlingRepository repository;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private DocImporter docImporter;

    public void handleEvent(final Event osgiEvent) {
        log.info("[docs-git-importer] GitListener triggered");
        Project gitProject = (Project)osgiEvent.getProperty(GitHubPushConstants.EVT_PROJECT);
        GitHubPushEvent gitHubPushEvent = (GitHubPushEvent)osgiEvent.getProperty(GitHubPushConstants.EVT_GHEVENT);
        
        // Determine location for local cache of git repo in JCR tree
        URI gitRepoUrl = gitHubPushEvent.getRepoUrl();
        String gitRepoJcrPath = DocImporter.GIT_REPOS_FOLDER_PATH + "/" + gitRepoUrl.getHost() + gitRepoUrl.getPath();
        
        try {
            Session session = repository.loginAdministrative(null);

            // Fetch config file from git to local cache, get its contents
            Properties config = fetchConfig(session, gitProject, gitRepoJcrPath);
            log.info("[docs-git-importer] Config fetched");

            // If config indicates fullFetch = true then fetch entire git repo
            String isFullFetch = config.getProperty(DocImporter.CONFIG_PARAM_FULL_FETCH);
            if ( isFullFetch != null && "true".equals(isFullFetch)) {
                log.info("[docs-git-importer] Full fetch");

                // If a previous local copy of the git repo exists, remove it
                removeCache(session, gitRepoJcrPath);
                log.info("[docs-git-importer] Cache removed");

                // Refetch config
                fetchConfig(session, gitProject, gitRepoJcrPath);
                log.info("[docs-git-importer] Config refetched");

                // Fetch source directory
                log.info("[docs-git-importer] Start fetch source folder");
                fetchGitDirectory(session, gitProject, gitRepoJcrPath, config.getProperty(DocImporter.CONFIG_PARAM_SOURCE_FOLDER));

            // If config indicates fullFetch = false (or no fullFetch specified)
            // then fetch only new and modified files and delete deleted files.
            } else {
                log.info("[docs-git-importer] Not full fetch, fetch only changes");
                List<String> added = gitHubPushEvent.getAddedFileNames();
                List<String> modified = gitHubPushEvent.getModifiedFileNames();
                List<String> deleted = gitHubPushEvent.getDeletedFileNames();
                modified.addAll(added);

                for (String path : modified){
                    try {
                        fetchGitFile(session, gitProject, gitRepoJcrPath, path);
                    } catch (IOException e) {
                        log.error("[docs-git-importer] File exceeds GitHUb API limit of 1 MB. Skipping " + path);
                    }
                }

                for (String path : deleted){
                    String deleteNodePath = gitRepoJcrPath + "/" + path;
                    if (session.nodeExists(deleteNodePath)){
                        session.getNode(deleteNodePath).remove();
                        session.save();
                    }
                }
            }

            // Run importer on local git repo cache
            docImporter.doImport(gitRepoJcrPath);

        } catch (IOException e) {
            log.error("[docs-git-importer] ", e);
        } catch (RepositoryException e) {
            log.error("[docs-git-importer] ", e);
        }
    }

    private Properties fetchConfig(Session session, Project gitProject, String gitRepoJcrPath) throws RepositoryException, IOException {
        Node configNode = fetchGitFile(session, gitProject, gitRepoJcrPath, DocImporter.CONFIG_FILE_NAME);
        Properties config = new java.util.Properties();
        config.loadFromXML(JcrUtils.readFile(configNode));
        return config;
    }

    private Node fetchGitFile(Session session, Project gitProject, String gitRepoJcrPath, String path) throws RepositoryException, IOException {
        log.info("[docs-git-importer] Fetching file: " + path);
        File file = gitProject.getFile(EncodeUtil.escapePath(path));

        String fileName = getFileName(path);
        String parentPath = getParentPath(gitRepoJcrPath, path);

        Node parentNode = JcrUtils.getOrCreateByPath(parentPath, "nt:folder", "nt:folder", session, true);
        Node node = JcrUtils.putFile(parentNode, fileName, "application/xml", IOUtils.toInputStream(file.getContent(), "UTF-8"));

        session.save();
        return node;
    }
    
    private void fetchGitDirectory(Session session, Project gitProject, String gitRepoJcrPath, String path) throws RepositoryException, IOException {
        log.info("[docs-git-importer] Fetching directory: " + path);
        Directory directory = gitProject.getDirectory(path);

        List<String> fileList = directory.listFiles();
        for (String filePath : fileList) {
            try {
                fetchGitFile(session, gitProject, gitRepoJcrPath, filePath);
            } catch (IOException e) {
                log.error("[docs-git-importer] File exceeds GitHUb API limit of 1 MB. Skipping " + path);
            }
        }

        List<String> directoryList = directory.listDirectories();
        for (String directoryPath : directoryList) {
            fetchGitDirectory(session, gitProject, gitRepoJcrPath, directoryPath);
        }
    }

    private void removeCache(Session session, String gitRepoJcrPath) throws RepositoryException {
        log.info("[docs-git-importer] Check if old cache exists");
        if (session.nodeExists(gitRepoJcrPath)) {
            log.info("[docs-git-importer] Removing old cache");
            Node gitRepoNode = session.getNode(gitRepoJcrPath);
            gitRepoNode.remove();
            session.save();
            log.info("Removed existing copy of git repo at: " + gitRepoJcrPath);
        }
    }

    private String getParentPath(String sourcePath, String path) {
        int lastForwardSlashPos = path.lastIndexOf("/");
        String parentPath = sourcePath;
        if (lastForwardSlashPos > 0) {
            parentPath = parentPath + "/" + path.substring(0, lastForwardSlashPos);
        }
        return parentPath;
    }

    private String getFileName(String path) {
        String[] split = path.split("/");
        return split[split.length - 1];
    }
}