/*
 * Copyright (C) 2007-2013 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.cstudio.publishing.processor;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.craftercms.search.service.SearchService;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class SearchUpdateFlattenXmlProcessor implements PublishingProcessor {

    private static final Log logger = LogFactory.getLog(SearchUpdateFlattenXmlProcessor.class);

    private SearchService searchService;
    private String siteName;
    private String includeElementXPathQuery;
    private String charEncoding = CharEncoding.UTF_8;

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @Required
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    @Required
    public void setIncludeElementXPathQuery(String includeElementXPathQuery) {
        this.includeElementXPathQuery = includeElementXPathQuery;
    }

    public void setCharEncoding(String charEncoding) {
        this.charEncoding = charEncoding;
    }

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters, PublishingTarget target) throws PublishingException {
        String root = target.getParameter(FileUploadServlet.CONFIG_ROOT);
        String contentFolder = target.getParameter(FileUploadServlet.CONFIG_CONTENT_FOLDER);
        String siteId = parameters.get(FileUploadServlet.PARAM_SITE);
        if (StringUtils.isEmpty(siteId)) {
            siteId = siteName;
        }

        root += "/" + contentFolder;
        if (org.springframework.util.StringUtils.hasText(siteId)) {
            root = root.replaceAll(FileUploadServlet.CONFIG_MULTI_TENANCY_VARIABLE, siteId);
        }

        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();

        if (CollectionUtils.isNotEmpty(createdFiles)) {
            update(siteId, root, createdFiles, false);
        }
        if (CollectionUtils.isNotEmpty(updatedFiles)) {
            update(siteId, root, updatedFiles, false);
        }
        if (CollectionUtils.isNotEmpty(deletedFiles)) {
            update(siteId, root, deletedFiles, true);
        }

        searchService.commit();
    }

    @Override
    public String getName() {
        return SearchUpdateFlattenXmlProcessor.class.getSimpleName();
    }

    private void update(String siteId, String root, List<String> fileNames, boolean delete) throws PublishingException {
        for (String fileName : fileNames) {
            if (fileName.endsWith(".xml")) {
                try {
                    if (delete) {
                        searchService.delete(siteId, fileName);

                        if (logger.isDebugEnabled()) {
                            logger.debug(siteId + ":" + fileName + " deleted from search index");
                        }
                    } else {
                        File file = new File(root + fileName);
                        try {
                            String flattenedXml = flattenXml(root, file);
                            searchService.update(siteId, fileName, flattenedXml, true);

                            if (logger.isDebugEnabled()) {
                                logger.debug(siteId + ":" + fileName + " added to search index");
                            }
                        } catch (IOException e) {
                            logger.warn("Cannot read file [" + file + "]. Continuing index update...", e);
                        }
                    }
                } catch (Exception e) {
                    throw new PublishingException(e);
                }
            }
        }
    }

    private String flattenXml(String root, File file) throws IOException, DocumentException, URISyntaxException {
        SAXReader reader = new SAXReader();
        SAXReader includeReader = new SAXReader();

        try {
            String fileContents = FileUtils.readFileToString(file, charEncoding);

            reader.setEncoding(charEncoding);
            String finalXml = "";

            Document document = reader.read(file);
            List<Element> includeElements = document.selectNodes(includeElementXPathQuery);
            if (CollectionUtils.isEmpty(includeElements)) {
                return fileContents;
            }
            for (Element includeElement : includeElements) {
                String includeSrcPath = root + "/" + includeElement.getTextTrim();
                if (StringUtils.isEmpty(includeSrcPath)) {
                    continue;
                }

                File includeFile = new File(includeSrcPath);
                if (includeFile != null && includeFile.exists()) {
                    includeReader.setEncoding(charEncoding);
                    Document includeDocument = includeReader.read(includeFile);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Include found in " + file.getAbsolutePath() + ": " + includeSrcPath);
                    }

                    doInclude(includeElement, includeSrcPath, includeDocument);
                }

            }
            finalXml = document.asXML();
            return finalXml;
        } finally {
            reader.resetHandlers();
            reader = null;
            includeReader.resetHandlers();
            includeReader = null;
        }
    }

    private void doInclude(Element includeElement, String includeSrcPath, Document includeSrc) {
        List<Node> includeElementParentChildren = includeElement.getParent().content();
        int includeElementIdx = includeElementParentChildren.indexOf(includeElement);
        Element includeSrcRootElement = includeSrc.getRootElement().createCopy();

        // Remove the <include> element
        includeElementParentChildren.remove(includeElementIdx);

        // Add the src root element
        includeElementParentChildren.add(includeElementIdx, includeSrcRootElement);
    }
}
