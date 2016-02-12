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
package org.craftercms.deployer.git.processor;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.craftercms.cstudio.publishing.utils.SearchUtils;
import org.craftercms.cstudio.publishing.utils.XmlUtils;
import org.craftercms.deployer.git.config.SiteConfiguration;
import org.craftercms.search.service.SearchService;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processor to update the Crafter Search engine index.
 *
 * @author Alfonso Vásquez
 */
public class SearchUpdateProcessor implements PublishingProcessor {

    private static final Log logger = LogFactory.getLog(SearchUpdateProcessor.class);

    protected SearchService searchService;
    protected String siteName;
    protected Map<String, String> fieldMappings;
    protected String charEncoding = CharEncoding.UTF_8;
    protected String tokenizeAttribute = "tokenized";
    protected Map<String, String> tokenizeSubstitutionMap = new HashMap<String, String>() {{
        put("_s", "_t");
        put("_smv", "_tmv");
    }};
    protected int order = Integer.MAX_VALUE;

    public SearchService getSearchService() {
        return searchService;
    }

    public String getSiteName() {
        return siteName;
    }

    public Map<String, String> getFieldMappings() {
        return fieldMappings;
    }

    public String getCharEncoding() {
        return charEncoding;
    }

    public String getTokenizeAttribute() {
        return tokenizeAttribute;
    }

    public Map<String, String> getTokenizeSubstitutionMap() {
        return tokenizeSubstitutionMap;
    }

    @Override
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * set a sitename to override in index
     *
     * @param siteName an override siteName in index
     */
    public void setSiteName(String siteName) {
        if (!StringUtils.isEmpty(siteName)) {
            // check if it is preview for backward compatibility
            if (!SITE_NAME_PREVIEW.equalsIgnoreCase(siteName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Overriding site name in index with " + siteName);
                }
                this.siteName = siteName;
            }
        }
    }

    public void setFieldMappings(Map<String, String> fieldMappings) {
        this.fieldMappings = fieldMappings;
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
    public void doProcess(SiteConfiguration siteConfiguration, PublishedChangeSet changeSet) throws PublishingException {
        String root = siteConfiguration.getLocalRepositoryRoot();
        String siteId = (!StringUtils.isEmpty(siteName))? siteName: siteConfiguration.getSiteId();

        if (StringUtils.isNotBlank(siteId)) {
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
                        try {
                            String xml = processXml(root, fileName);

                            searchService.update(siteId, fileName, xml, true);

                            if (logger.isDebugEnabled()) {
                                logger.debug(siteId + ":" + fileName + " added to search index");
                            }
                        } catch (DocumentException e) {
                            logger.warn("Cannot process XML file " + siteId + ":" + fileName + ". Continuing index " +
                                        "update...", e);
                        }
                    }
                } catch (Exception e) {
                    throw new PublishingException(e);
                }
            }
        }
    }

    protected String processXml(String root, String fileName) throws DocumentException {
        File file = new File(root + fileName);

        Document document = processDocument(root, file, XmlUtils.readXml(file, charEncoding));
        String xml = document.asXML();

        if (logger.isDebugEnabled()) {
            logger.debug("Processed XML:");
            logger.debug(xml);
        }

        return xml;
    }

    protected Document processDocument(String root, File file, Document document) throws DocumentException {
        document = renameFields(document);
        document = processTokenizeAttributes(document);

        return document;
    }

    protected Document renameFields(Document document) {
        return SearchUtils.renameFields(document, fieldMappings);
    }

    protected Document processTokenizeAttributes(Document document) {
        return SearchUtils.processTokenizeAttributes(document, tokenizeAttribute, tokenizeSubstitutionMap);
    }

}

