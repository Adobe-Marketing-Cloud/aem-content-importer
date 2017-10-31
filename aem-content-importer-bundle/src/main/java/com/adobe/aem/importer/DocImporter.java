/*******************************************************************************
* Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
*
* Licensed under the Apache License 2.0.
* http://www.apache.org/licenses/LICENSE-2.0
******************************************************************************/

package com.adobe.aem.importer;

/**
 * XML Transformer Interface
 */
public interface DocImporter {

    // Default config file name
    String CONFIG_FILE_NAME = "config.xml";

    // Supported source formats
    String SOURCE_FORMAT_DITA = "dita";
    String SOURCE_FORMAT_DOCBOOK = "docbook";

    // Config parameters and default values
    String CONFIG_PARAM_SOURCE_FORMAT = "sourceFormat";
    String DEFAULT_SOURCE_FORMAT = SOURCE_FORMAT_DITA;

    String CONFIG_PARAM_SOURCE_FOLDER = "sourceFolder";
    String DEFAULT_SOURCE_FOLDER = "src";

    String CONFIG_PARAM_MASTER_FILE = "masterFile";
    String DEFAULT_MASTER_FILE = "default.ditamap";

    String CONFIG_PARAM_GRAPHICS_FOLDER = "graphicsFolder";
    String DEFAULT_GRAPHICS_FOLDER = "graphics";

    String CONFIG_PARAM_TARGET_PATH = "targetPath";
    String DEFAULT_TARGET_PATH = "/content/imported";

    String CONFIG_PARAM_FULL_FETCH = "fullFetch";

    // XSLT locations
    String DITA_XSLT_PATH = "/apps/aem-importer/resources/dita-to-content.xsl";
    String DOCBOOK_XSLT_PATH = "/apps/aem-importer/resources/docbook-to-content.xsl";

    // Package template location
    String CONTENT_PACKAGE_TEMPLATE_PATH = "/apps/aem-importer/resources/package-tpl";

    // Working locations
    String ROOT_WORKING_PATH = "/var/doc-importer";
    String CONTENT_PACKAGE_NAME = "package";
    String CONTENT_PACKAGE_PATH = ROOT_WORKING_PATH + "/" + CONTENT_PACKAGE_NAME;
    String GIT_REPOS_FOLDER_NAME = "git-repos";
    String GIT_REPOS_FOLDER_PATH = ROOT_WORKING_PATH + "/" + GIT_REPOS_FOLDER_NAME;
    String ZIP_UPLOAD_FOLDER_NAME = "zips";
    String ZIP_UPLOAD_FOLDER_PATH = ROOT_WORKING_PATH + "/" + ZIP_UPLOAD_FOLDER_NAME;
    String ZIP_FILE_NAME = "tmp.zip";

    /**
	 * Initialize and execute import of content
	 */
	void doImport(String importRootPath);
}
