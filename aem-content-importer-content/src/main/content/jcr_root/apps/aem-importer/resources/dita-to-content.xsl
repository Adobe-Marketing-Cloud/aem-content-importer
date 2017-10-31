<?xml version="1.0" encoding="UTF-8"?>
<!-- Dita to JCR -->
<xsl:stylesheet
    version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
    xmlns:cq="http://www.day.com/jcr/cq/1.0"
    xmlns:jcr="http://www.jcp.org/jcr/1.0"
    xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    xmlns:pd="http://www.adobe.com/pando">

    <xsl:output method="xml" indent="yes"/>
    <xsl:strip-space elements="*"/>
    <xsl:variable name="ditamapUri" select="base-uri()"/>
    <!-- Templates -->

    <xsl:template match="/">
        <!--
        <xsl:message>[xslt] matched '/'</xsl:message>
        <xsl:message><xsl:value-of select="concat('[xslt] ditamapUri: ', $ditamapUri)" /></xsl:message>
        -->
        <xsl:apply-templates select="map"/>
    </xsl:template>

    <xsl:template match="map">
        <!--
        <xsl:message>[xslt] matched 'map'</xsl:message>
        -->
        <jcr:root jcr:primaryType="cq:Page">
            <jcr:content
                jcr:primaryType="cq:PageContent"
                jcr:title="{title}"
                sling:resourceType="dev/components/pages/fullwidthcontentpage"
                cq:template="/apps/dev/templates/fullwidthcontentpage"
                pageTitle="{title}"
                browserTitle="{title}"
                metaDescription="{title}"
                alignment="left"
                disableGeoRouting="true"
                enableBreadcrumb="true"
                groupSectionNavItems="no"
                hideMerchandisingBar="inherit"
                hidePromoComponent="inherit"
                isFullWidthContent="true"
                marketSegmentRewrite="none"
                modalSize="426x240"
                showSubNav="true">
                <contentbody
                    jcr:primaryType="nt:unstructured"
                    sling:resourceType="beagle/components/parsys">
                    <sitemap
                        jcr:primaryType="nt:unstructured"
                        sling:resourceType="foundation/components/sitemap"
                        rootPath="."/>
                </contentbody>
            </jcr:content>
            <xsl:apply-templates select="topicref | mapref" mode="subpage">
                <xsl:with-param name="pathPrefix" select="''"/>
            </xsl:apply-templates>
        </jcr:root>
    </xsl:template>

    <xsl:template match="topicref" mode="subpage">
        <xsl:param name="pathPrefix"/>
        <xsl:variable name="xmlElementName" select="pd:pathToXMLElementName(@href)"/>
        <xsl:variable name="nextPathPrefix" select="concat('../', $pathPrefix)"/>
        <!--
        <xsl:message>[xslt] matched 'topicref'</xsl:message>
        <xsl:message><xsl:value-of select="concat('[xslt] pathPrefix: ', $pathPrefix)" /></xsl:message>
        <xsl:message><xsl:value-of select="concat('[xslt] xmlElementName: ', $xmlElementName)" /></xsl:message>
        <xsl:message><xsl:value-of select="concat('[xslt] nextPathPrefix: ', $nextPathPrefix)" /></xsl:message>
        -->
        <xsl:choose>
            <xsl:when test="@scope='local'">
                <xsl:element name="{$xmlElementName}">
                    <xsl:attribute name="jcr:primaryType" select="'cq:Page'"/>
                    <xsl:apply-templates select="document(@href)/(concept | task | reference)">
                        <xsl:with-param name="pathPrefix" select="$pathPrefix"/>
                        <xsl:with-param name="pageName" select="$xmlElementName"/>
                    </xsl:apply-templates>
                    <xsl:apply-templates select="topicref | mapref" mode="subpage">
                        <xsl:with-param name="pathPrefix" select="$nextPathPrefix"/>
                    </xsl:apply-templates>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:element name="{$xmlElementName}">
                    <xsl:attribute name="jcr:primaryType" select="'cq:Page'"/>
                    <xsl:apply-templates select="topicref | mapref" mode="subpage">
                        <xsl:with-param name="pathPrefix" select="$nextPathPrefix"/>
                    </xsl:apply-templates>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="mapref" mode="subpage">
        <xsl:param name="pathPrefix"/>
        <xsl:variable name="xmlElementName" select="pd:pathToXMLElementName(@href)"/>
        <xsl:variable name="nextPathPrefix" select="concat('../', $pathPrefix)"/>
        <xsl:element name="{$xmlElementName}">
            <xsl:attribute name="jcr:primaryType" select="'cq:Page'"/>
            <xsl:apply-templates select="topicref | mapref">
                <xsl:with-param name="pathPrefix" select="$nextPathPrefix"/>
            </xsl:apply-templates>
        </xsl:element>
    </xsl:template>

    <xsl:template match="concept | task | reference">
        <xsl:param name="pathPrefix"/>
        <xsl:param name="pageName"/>
        <xsl:variable name="concept-id" select="id"/>
        <jcr:content
            jcr:primaryType="cq:PageContent"
            cq:template="/apps/dev/templates/fullwidthcontentpage-with-subnav"
            jcr:title="{title}"
            sling:resourceType="dev/components/pages/fullwidthcontentpage"
            alignment="left"
            disableGeoRouting="true"
            enableBreadcrumb="true"
            showSubNav="true">
            <xsl:apply-templates select="conbody | taskbody | refbody">
                <xsl:with-param name="pathPrefix" select="$pathPrefix"/>
                <xsl:with-param name="pageName" select="$pageName"/>
                <xsl:with-param name="concept-id" select="$concept-id"/>
            </xsl:apply-templates>
        </jcr:content>
    </xsl:template>

    <xsl:template match="conbody | taskbody | refbody">
        <xsl:param name="pathPrefix"/>
        <xsl:param name="pageName"/>
        <xsl:param name="concept-id"/>
        <leftsection
            jcr:primaryType="nt:unstructured"
            sling:resourceType="foundation/components/iparsys">
            <sidebar
                jcr:primaryType="nt:unstructured"
                sling:resourceType="dev/components/structure/sidebar" />
        </leftsection>
        <contentbody
            jcr:primaryType="nt:unstructured"
            sling:resourceType="beagle/components/parsys">
            <xsl:variable name="html">
                <div id = "{$concept-id}">
                    <xsl:apply-templates select="*" mode="html">
                        <xsl:with-param name="pathPrefix" select="$pathPrefix"/>
                    </xsl:apply-templates>
                </div>
            </xsl:variable>

            <!-- debug code
            <xsl:result-document
                    method="html"
                    href="{concat('/home/ppiegaze/out/', $pageName, '.html')}">
                <xsl:apply-templates select="*" mode="html">
                    <xsl:with-param name="pathPrefix" select="$pathPrefix"/>
                </xsl:apply-templates>
            </xsl:result-document>
            end debug -->

            <xsl:variable name="serialized">
                <xsl:apply-templates select="$html" mode="serialize" />
            </xsl:variable>

            <title
                jcr:primaryType="nt:unstructured"
                sling:resourceType="dev/components/title"
                headingStyle="h1"/>
            <text
                jcr:primaryType="nt:unstructured"
                sling:resourceType="dev/components/text"
                cssClassName="cq-dev-text"
                textIsRich="true"
                text="{$serialized}"/>
        </contentbody>
    </xsl:template>

    <!--  HTML Conversion -->

    <!-- Elements -->

    <xsl:template match="xref" mode="html">
        <xsl:param name="pathPrefix"/>
        <xsl:element name="a">
            <xsl:attribute name="href">
                <xsl:choose>
                    <xsl:when test="@scope='local'">
                        <xsl:value-of select="pd:xrefToAnchor(@href, $pathPrefix, $ditamapUri)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="@href"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates select="@* except @href" mode="html">
                <xsl:with-param name="dita-el" select="name()"/>
            </xsl:apply-templates>
            <xsl:apply-templates select="* | text() | processing-instruction()" mode="html">
                <xsl:with-param name="pathPrefix" select="$pathPrefix"/>
            </xsl:apply-templates>
        </xsl:element>
    </xsl:template>

    <xsl:template match="image" mode="html">
        <xsl:param name="pathPrefix"/>
        <xsl:element name="img">
            <xsl:attribute name="src">
                <xsl:value-of select="concat($pathPrefix, @href)"/>
            </xsl:attribute>
            <xsl:apply-templates select="@* except @href" mode="html">
                <xsl:with-param name="dita-el" select="name()"/>
            </xsl:apply-templates>
            <xsl:apply-templates select="* | text() | processing-instruction()" mode="html">
                <xsl:with-param name="pathPrefix" select="$pathPrefix"/>
            </xsl:apply-templates>
        </xsl:element>
    </xsl:template>

    <xsl:template match="draft-comment" mode="html">
    </xsl:template>

    <xsl:template match="*" mode="html">
        <xsl:param name="pathPrefix"/>
        <xsl:element name="{pd:convert(name())}">
            <xsl:apply-templates select="@*" mode="html">
                <xsl:with-param name="dita-el" select="name()"/>
            </xsl:apply-templates>
            <xsl:apply-templates select="* | text() | processing-instruction()" mode="html">
                <xsl:with-param name="pathPrefix" select="$pathPrefix"/>
            </xsl:apply-templates>
        </xsl:element>
    </xsl:template>

    <!-- Attributes -->
    <xsl:template match="@*" mode="html">
        <xsl:attribute name="{name()}">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="@class" mode="html">
        <xsl:param name="dita-el"/>
        <xsl:attribute name="class">
            <xsl:value-of select="."/>
            <xsl:text> dita-</xsl:text>
            <xsl:value-of select="$dita-el"/>
        </xsl:attribute>
    </xsl:template>

    <!-- Text -->
    <xsl:template match="text()" mode="html">
        <xsl:value-of select="."/>
    </xsl:template>

    <!-- Processing Instructions -->
    <xsl:template match="processing-instruction()" mode="html">
        <xsl:value-of select="."/>
    </xsl:template>

    <!-- Serialize -->

    <!-- Elements -->
    <xsl:template match="*" mode="serialize">
        <xsl:text>&lt;</xsl:text>
        <xsl:value-of select="name()"/>
        <xsl:apply-templates select="@*" mode="serialize"/>
        <xsl:choose>
            <xsl:when test="node()">
                <xsl:text>&gt;</xsl:text>
                <xsl:apply-templates mode="serialize"/>
                <xsl:text>&lt;/</xsl:text>
                <xsl:value-of select="name()"/>
                <xsl:text>&gt;</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text> /&gt;</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Attributes -->
    <xsl:template match="@*" mode="serialize">
        <xsl:text> </xsl:text>
        <xsl:value-of select="name()"/>
        <xsl:text>="</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>"</xsl:text>
    </xsl:template>

    <!-- Text -->
    <xsl:template match="text()" mode="serialize">
        <xsl:value-of select="."/>
    </xsl:template>

    <!-- Functions -->

    <xsl:function name="pd:removeTrailingSlash">
        <xsl:param name="path"/>
        <xsl:value-of select="replace($path, '/+$', '')"/>
    </xsl:function>

    <xsl:function name="pd:removeExtension">
        <xsl:param name="path"/>
        <xsl:value-of select="replace($path, '\.[^\.]*$', '')"/>
    </xsl:function>

    <xsl:function name="pd:getFragment">
        <xsl:param name="path"/>
        <xsl:value-of select="tokenize($path, '#')[2]"/>
    </xsl:function>

    <xsl:function name="pd:pathToFileName">
        <xsl:param name="path"/>
        <xsl:value-of select="tokenize(pd:removeTrailingSlash($path), '/')[last()]"/>
    </xsl:function>

    <xsl:function name="pd:replaceNonAlphanum">
        <xsl:param name="name"/>
        <xsl:variable name="alphanum" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_0123456789'"/>
        <xsl:variable name="underscores" select="'_____________________________________'"/>
        <xsl:variable name="xmlName" select="translate($name, translate($name, $alphanum, ''), $underscores)"/>
        <xsl:choose>
            <xsl:when test="$xmlName=''">
                <xsl:variable name="out" select="'_dummy'"/>
                <xsl:value-of select="$out"/>
            </xsl:when>
            <xsl:when test="contains('0123456789', substring($xmlName, 1, 1))">
                <xsl:variable name="out" select="concat('_', $xmlName)"/>
                <xsl:value-of select="$out"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$xmlName"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="pd:pathToXMLElementName">
        <xsl:param name="path"/>
        <xsl:value-of select="pd:replaceNonAlphanum(pd:removeExtension(pd:pathToFileName($path)))"/>
    </xsl:function>

    <!-- Map dita to html -->
    <xsl:function name="pd:convert">
        <xsl:param name="dita-el"/>
        <xsl:variable name="mapping">
            <entry key="keyword">b</entry>
            <entry key="filepath">i</entry>
            <entry key="codeph">i</entry>
            <entry key="steps">ul</entry>
            <entry key="step">li</entry>
            <entry key="stepresult">p</entry>
            <entry key="choices">ul</entry>
            <entry key="choice">li</entry>
            <entry key="postreq">p</entry>
            <entry key="cmd">b</entry>
            <entry key="context">p</entry>
            <entry key="info">p</entry>
            <entry key="result">p</entry>
            <entry key="ul">ul</entry>
            <entry key="ol">ol</entry>
            <entry key="li">li</entry>
            <entry key="sl">dl</entry>
            <entry key="sli">dt</entry>
            <entry key="xref">a</entry>
            <entry key="term">b</entry>
            <entry key="b">b</entry>
            <entry key="title">h2</entry>
            <entry key="table">table</entry>
            <entry key="thead">thead</entry>
            <entry key="tbody">tbody</entry>
            <entry key="row">tr</entry>
            <entry key="entry">td</entry>
            <entry key="codeblock">code</entry>
            <entry key="menucascade">p</entry>
            <entry key="uicontrol">b</entry>
            <entry key="wintitle">b</entry>
            <entry key="image">img</entry>
            <entry key="note">p</entry>
            <entry key="section">div</entry>
            <entry key="draft-comment"></entry>
            <!-- TODO
            <entry key="tgroup">?</entry>
            <entry key="colspec">?</entry>
            -->
        </xsl:variable>
        <xsl:variable name="html-el" select="$mapping/entry[@key=$dita-el]"/>
        <xsl:choose>
            <xsl:when test="$html-el">
                <xsl:value-of select="$html-el"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$dita-el"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    <xsl:function name="pd:removeFragment">
        <xsl:param name="path"/>
        <xsl:value-of select="tokenize($path, '#')[1]"/>
    </xsl:function>

    <xsl:function name="pd:xrefToAnchor">
        <xsl:param name="targetUri"/>
        <xsl:param name="pathPrefix"/>
        <xsl:param name="ditamapUri"/>
<!--
        <xsl:message><xsl:value-of select="' '" /></xsl:message>
        <xsl:message><xsl:value-of select="' '" /></xsl:message>
        <xsl:message><xsl:value-of select="''" /></xsl:message>
        <xsl:message><xsl:value-of select="concat('[xslt] targetUri: ', $targetUri)" /></xsl:message>
        <xsl:message><xsl:value-of select="concat('[xslt] pathPrefix: ', $pathPrefix)" /></xsl:message>
        <xsl:message><xsl:value-of select="concat('[xslt] ditamapUri: ', $ditamapUri)" /></xsl:message>
-->
        <xsl:variable name="cleanTargetUri">
            <xsl:value-of select="pd:removeFragment($targetUri)"/>
        </xsl:variable>
        <xsl:message><xsl:value-of select="concat('cleanTargetUri: ', $cleanTargetUri)"/></xsl:message>

        <xsl:variable name="ancestorSet">
            <xsl:value-of select="document($ditamapUri)//*[@href=$cleanTargetUri]/ancestor::*"/>
        </xsl:variable>
<!--
        <xsl:variable name="ancestorSetString">
            <xsl:apply-templates select="$ancestorSet" mode="serialize"/>
        </xsl:variable>

        <xsl:message>************</xsl:message>
        <xsl:message><xsl:value-of select="$ancestorSetString"></xsl:value-of></xsl:message>
        <xsl:message>************</xsl:message>

        <xsl:for-each select="$ancestorSet">
            <xsl:message>start-ancestor-set-hrefs</xsl:message>
            <xsl:message><xsl:value-of select="."/></xsl:message>
            <xsl:message>end-ancestor-set-hrefs</xsl:message>
        </xsl:for-each>
-->

        <xsl:variable name="ancestorHrefPath">
            <xsl:for-each select="$ancestorSet">
                <xsl:if test="@href">
                    <xsl:value-of select="pd:removeExtension(@href)"/>
                </xsl:if>
                <xsl:text>/</xsl:text>
            </xsl:for-each>
        </xsl:variable>
<!--        <xsl:message><xsl:value-of select="concat('ancestorHrefPath: ', $ancestorHrefPath)"/></xsl:message>
-->
        <xsl:value-of select="concat($pathPrefix, $ancestorHrefPath, pd:convertFileExtension($targetUri))"/>
    </xsl:function>

    <xsl:function name="pd:convertFileExtension">
        <xsl:param name="href"/>
        <xsl:value-of>
            <xsl:value-of select="pd:removeExtension(pd:removeFragment($href))"/>
            <xsl:text>.html</xsl:text>
            <xsl:if test="pd:getFragment($href) != ''">
                <xsl:text>#</xsl:text>
                <xsl:value-of select="pd:getFragment($href)"/>
            </xsl:if>
        </xsl:value-of>
    </xsl:function>

</xsl:stylesheet>