/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.service.impl.SolrQuery;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.cstudio.publishing.processor.SearchIndexingProcessor.DEFAULT_DEFAULT_INDEX_ID_FORMAT;

/**
 * Scans the created and updated files for component updates, then executes a search query to retrieve the pages
 * that reference the components. This pages are added to a new updated list, which is then passed to the actual
 * search indexing processor (not added to the original updated list since the updates are only for search purposes).
 *
 * @author avasquez
 */
public class ReIndexPagesOnComponentUpdateProcessor extends AbstractPublishingProcessor {

    private static final Log logger = LogFactory.getLog(ReIndexPagesOnComponentUpdateProcessor.class);

    public static final String DEFAULT_COMPONENTS_ROOT = "/site/components";
    public static final String DEFAULT_PAGES_QUERY_PATTERN = "*:%s";

    protected String indexId;
    protected String defaultIndexIdFormat;
    protected boolean ignoreIndexId;
    protected String siteName;
    protected String componentsRoot;
    protected String pagesQueryPattern;
    protected SearchService searchService;
    protected PublishingProcessor actualIndexingProcessor;

    public ReIndexPagesOnComponentUpdateProcessor() {
        defaultIndexIdFormat = DEFAULT_DEFAULT_INDEX_ID_FORMAT;
        componentsRoot = DEFAULT_COMPONENTS_ROOT;
        pagesQueryPattern = DEFAULT_PAGES_QUERY_PATTERN;
    }

    public void setIndexId(String indexId) {
        this.indexId = indexId;
    }

    public void setDefaultIndexIdFormat(String defaultIndexIdFormat) {
        this.defaultIndexIdFormat = defaultIndexIdFormat;
    }

    public void setIgnoreIndexId(boolean ignoreIndexId) {
        this.ignoreIndexId = ignoreIndexId;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public void setComponentsRoot(String componentsRoot) {
        this.componentsRoot = componentsRoot;
    }

    public void setPagesQueryPattern(String pagesQueryPattern) {
        this.pagesQueryPattern = pagesQueryPattern;
    }

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @Required
    public void setActualIndexingProcessor(PublishingProcessor actualIndexingProcessor) {
        this.actualIndexingProcessor = actualIndexingProcessor;
    }

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters,
                          PublishingTarget target) throws PublishingException {
        String siteName = getActualSiteId(parameters);
        String indexId = getActualIndexId(siteName);
        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();
        List<String> newUpdatedFiles = new ArrayList<>(updatedFiles);

        for (String path : createdFiles) {
            if (isComponent(path)) {
                addPagesThatIncludeComponentToUpdatedFiles(indexId, path, createdFiles, newUpdatedFiles);
            }
        }

        for (String path : updatedFiles) {
            if (isComponent(path)) {
                addPagesThatIncludeComponentToUpdatedFiles(indexId, path, createdFiles, newUpdatedFiles);
            }
        }

        doIndexing(new PublishedChangeSet(createdFiles, newUpdatedFiles, deletedFiles), parameters, target);
    }

    protected void doIndexing(PublishedChangeSet changeSet, Map<String, String> parameters,
                              PublishingTarget target) throws PublishingException {
        actualIndexingProcessor.doProcess(changeSet, parameters, target);
    }

    @SuppressWarnings("unchecked")
    protected List<String> getPagesThatIncludeComponent(String indexId, String componentPath) {
        Query query = createPagesThatIncludeComponentQuery(componentPath);
        Map<String, Object> result = searchService.search(indexId, query);
        Map<String, Object> response = (Map<String, Object>)result.get("response");
        List<Map<String, Object>> documents = (List<Map<String, Object>>)response.get("documents");
        List<String> pages = new ArrayList<>();

        for (Map<String, Object> document : documents) {
            pages.add((String)document.get("localId"));
        }

        return pages;
    }

    protected boolean isComponent(String path) {
        return path.startsWith(componentsRoot);
    }

    protected boolean isBeingUpdated(String path, List<String> createdFiles, List<String> updatedFiles) {
        return createdFiles.contains(path) || updatedFiles.contains(path);
    }

    protected void addPagesThatIncludeComponentToUpdatedFiles(String indexId, String component,
                                                              List<String> createdFiles,
                                                              List<String> updatedFiles) {
        List<String> pages = getPagesThatIncludeComponent(indexId, component);
        if (CollectionUtils.isNotEmpty(pages)) {
            for (String page : pages) {
                if (!isBeingUpdated(page, createdFiles, updatedFiles)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Page " + page + " includes updated component " + component + ". Adding it to " +
                                     "list of updated files.");
                    }

                    updatedFiles.add(page);
                }
            }
        }
    }

    protected Query createPagesThatIncludeComponentQuery(String componentId) {
        String queryStatement = String.format(pagesQueryPattern, componentId);
        SolrQuery query = new SolrQuery();

        query.setQuery(queryStatement);
        query.setFieldsToReturn("localId");

        return query;
    }

    protected String getActualSiteId(Map<String, String> parameters) {
        return StringUtils.isNotEmpty(siteName)? siteName : parameters.get(FileUploadServlet.PARAM_SITE);
    }

    protected String getActualIndexId(String siteName) {
        if (ignoreIndexId) {
            return null;
        } else {
            return StringUtils.isNotEmpty(indexId)? indexId : String.format(defaultIndexIdFormat, siteName);
        }
    }

}