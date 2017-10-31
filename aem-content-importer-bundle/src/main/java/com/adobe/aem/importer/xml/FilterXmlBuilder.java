/*******************************************************************************
* Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
*
* Licensed under the Apache License 2.0.
* http://www.apache.org/licenses/LICENSE-2.0
******************************************************************************/

package com.adobe.aem.importer.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class FilterXmlBuilder {

   private String root;

    private FilterXmlBuilder(String root) {
        this.root = root;
    }

    public String toXml(String nodename) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("<workspaceFilter version=\"1.0\">")
        .append("<filter root=\"").append(root).append(nodename).append("\" />")
        .append("</workspaceFilter>");

        return sb.toString();
    }

    public InputStream toStream(String nodename) {
        return new ByteArrayInputStream(toXml(nodename).getBytes());
    }

    public static FilterXmlBuilder fromRoot(String root) {
        return new FilterXmlBuilder(root);
    }
}
