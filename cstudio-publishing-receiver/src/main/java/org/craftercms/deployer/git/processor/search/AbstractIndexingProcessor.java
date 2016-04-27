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
package org.craftercms.deployer.git.processor.search;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.deployer.git.config.SiteConfiguration;
import org.craftercms.deployer.git.processor.PublishingProcessor;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;

/**
 * Abstract {@link PublishingProcessor} that provides a base for all processors that update/delete files from
 * a search index.
 *
 * @author avasquez
 */
public abstract class AbstractIndexingProcessor implements PublishingProcessor {

    private static final Log logger = LogFactory.getLog(AbstractIndexingProcessor.class);

    public static final String DEFAULT_DEFAULT_INDEX_ID_FORMAT = "%s-default";

    protected int order;
    protected String indexId;
    protected String defaultIndexIdFormat;
    protected boolean useNoIndexId;
    protected String siteName;
    protected SearchService searchService;

    public AbstractIndexingProcessor() {
        order = Integer.MAX_VALUE;
        defaultIndexIdFormat = DEFAULT_DEFAULT_INDEX_ID_FORMAT;
    }

    public void setIndexId(String indexId) {
        this.indexId = indexId;
    }

    public void setDefaultIndexIdFormat(String defaultIndexIdFormat) {
        this.defaultIndexIdFormat = defaultIndexIdFormat;
    }

    public void setUseNoIndexId(boolean useNoIndexId) {
        this.useNoIndexId = useNoIndexId;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void doProcess(SiteConfiguration siteConfiguration, PublishedChangeSet changeSet) throws PublishingException {
        String root = siteConfiguration.getLocalRepositoryRoot();
        String siteName = getActualSiteId(siteConfiguration);
        String indexId = getActualIndexId(siteName);

        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();
        int updateCount = 0;

        if (CollectionUtils.isNotEmpty(createdFiles)) {
            updateCount += update(indexId, siteName, root, createdFiles, false);
        }
        if (CollectionUtils.isNotEmpty(updatedFiles)) {
            updateCount += update(indexId, siteName, root, updatedFiles, false);
        }
        if (CollectionUtils.isNotEmpty(deletedFiles)) {
            updateCount += update(indexId, siteName, root, deletedFiles, true);
        }

        if (updateCount > 0) {
            searchService.commit(indexId);
        }
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    protected String getActualSiteId(SiteConfiguration siteConfiguration) {
        return StringUtils.isNotEmpty(siteName)? siteName : siteConfiguration.getSiteId();
    }

    protected String getActualIndexId(String siteName) {
        if (useNoIndexId) {
            return null;
        } else {
            return StringUtils.isNotEmpty(indexId)? indexId : String.format(defaultIndexIdFormat, siteName);
        }
    }

    protected void doUpdate(String indexId, String siteName, String id, File file, String xml) {
        searchService.update(indexId, siteName, id, xml, true);

        logger.info("File " + file + " added to " + getIndexNameStr(indexId) + " index");
    }

    protected void doUpdateFile(String indexId, String siteName, String id, File file) {
        searchService.updateFile(indexId, siteName, id, file);

        logger.info("File " + file + " added to " + getIndexNameStr(indexId) + " index");
    }

    protected void doUpdateFile(String indexId, String siteName, String id, File file,
                                Map<String, List<String>> additionalFields) {
        searchService.updateFile(indexId, siteName, id, file, additionalFields);

        logger.info("File " + file + " added to " + getIndexNameStr(indexId) + " index");
    }

    protected void doDelete(String indexId, String siteName, String id, File file) {
        searchService.delete(indexId, siteName, id);

        logger.info("File " + file + " deleted from " + getIndexNameStr(indexId) + " index");
    }

    protected String getIndexNameStr(String indexId) {
        return StringUtils.isNotEmpty(indexId)? "'" + indexId + "'": "default";
    }

    protected abstract int update(String indexId, String siteName, String root, List<String> fileNames,
                                   boolean delete) throws PublishingException;

}
