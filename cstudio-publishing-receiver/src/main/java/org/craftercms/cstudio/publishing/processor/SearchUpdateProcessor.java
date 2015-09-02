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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.craftercms.search.service.SearchService;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Required;

/**
 * Processor to update the Crafter Search engine index.
 *
 * @author Alfonso Vásquez
 */
public class SearchUpdateProcessor implements PublishingProcessor {

    private static final Log logger = LogFactory.getLog(SearchUpdateProcessor.class);

    private SearchService searchService;
    private String siteName;
    private String charEncoding = CharEncoding.UTF_8;
    private String tokenizeAttribute = "tokenized";
    private Map<String, String> tokenizeSubstitutionMap = new HashMap<String, String>(){{
        put("_s","_t");
        put("_smv","_tmv");
    }};

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * set a sitename to override in index
     *
     * @param siteName
     *          an override siteName in index
     */
    public void setSiteName(String siteName) {
        if (!StringUtils.isEmpty(siteName)) {
            // check if it is preview for backward compatibility
            if (!SITE_NAME_PREVIEW.equalsIgnoreCase(siteName)) {
                if (logger.isDebugEnabled()) logger.debug("Overriding site name in index with " + siteName);
                this.siteName = siteName;
            }
        }
    }

    public void setCharEncoding(String charEncoding) {
        this.charEncoding = charEncoding;
    }

    public void setTokenizeAttribute(String tokenizeAttribute) {
        this.tokenizeAttribute = tokenizeAttribute;
    }

    public void setTokenizeSubstitutionMap(Map<String, String> tokenizeSubstitutionMap) {
        this.tokenizeSubstitutionMap = tokenizeSubstitutionMap;
    }

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters, PublishingTarget target)
        throws PublishingException {
        String root = target.getParameter(FileUploadServlet.CONFIG_ROOT);
        String contentFolder = target.getParameter(FileUploadServlet.CONFIG_CONTENT_FOLDER);
        String siteId = (!StringUtils.isEmpty(siteName)) ? siteName : parameters.get(FileUploadServlet.PARAM_SITE);

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
        return SearchUpdateProcessor.class.getSimpleName();
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
                        if (fileName.endsWith(".xml")) {
                            Document doc = parseTokenizeAttribute(file);
                            String parsedXml = doc.asXML();
                            if (logger.isDebugEnabled()) {
                                logger.debug("Parsed XML:");
                                logger.debug(parsedXml);
                            }
                            searchService.update(siteId, fileName, parsedXml, true);
                        } else {
                            try {
                                searchService.update(siteId, fileName, FileUtils.readFileToString(file, charEncoding),
                                                     true);

                                if (logger.isDebugEnabled()) {
                                    logger.debug(siteId + ":" + fileName + " added to search index");
                                }
                            } catch (IOException e) {
                                logger.warn("Cannot read file [" + file + "]. Continuing index update...", e);
                            }
                        }

                        if (logger.isDebugEnabled()) {
                            logger.debug(siteId + ":" + fileName + " added to search index");
                        }

                    }
                } catch (Exception e) {
                    throw new PublishingException(e);
                }
            }
        }
    }

    private Document parseTokenizeAttribute(File file) throws DocumentException, URISyntaxException {

        SAXReader reader = new SAXReader();

        try {
            reader.setEncoding(charEncoding);

            Document document = reader.read(file);
            String tokenizeXpath = String.format("//*[@%s=\"true\"]", tokenizeAttribute);
            if (logger.isDebugEnabled()) {
                logger.debug("Using tokenize XPath: " + tokenizeXpath);
            }
            List<Element> tokenizeElements = document.selectNodes(tokenizeXpath);
            if (logger.isDebugEnabled()) {
                logger.debug("Number of elements found to perform tokenize parsing: " + tokenizeElements.size());
            }

            if (CollectionUtils.isEmpty(tokenizeElements)) {
                return document;
            }
            for (Element tokenizeElement : tokenizeElements) {
                Element parent = tokenizeElement.getParent();
                String elemName = tokenizeElement.getName();
                if (logger.isDebugEnabled()) {
                    logger.debug("Parsing element: " + elemName);
                }
                for (String substitutionKey : tokenizeSubstitutionMap.keySet()) {
                    if (elemName.endsWith(substitutionKey)) {
                        String newElementName = elemName.substring(0, elemName.length() - substitutionKey.length()) +
                                                tokenizeSubstitutionMap.get(substitutionKey);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding new element for tokenized search: " + newElementName);
                        }
                        Element newElement = tokenizeElement.createCopy(newElementName);
                        parent.add(newElement);
                    }
                }
            }
            return document;
        } finally {
            reader.resetHandlers();
            reader = null;
        }
    }

}

