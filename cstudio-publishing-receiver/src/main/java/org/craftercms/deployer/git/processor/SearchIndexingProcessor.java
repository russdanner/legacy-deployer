package org.craftercms.deployer.git.processor;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.deployer.git.config.SiteConfiguration;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link PublishingProcessor} that uses a {@link BatchIndexer} to update a Crafter Search index. This processor
 * replaces all previous search processors.
 *
 * @author avasquez
 */
public class SearchIndexingProcessor extends AbstractPublishingProcessor {

    public static final String DEFAULT_DEFAULT_INDEX_ID_FORMAT = "%s-default";

    protected String indexId;
    protected String defaultIndexIdFormat;
    protected boolean ignoreIndexId;
    protected String siteName;
    protected SearchService searchService;
    protected BatchIndexer batchIndexer;

    public SearchIndexingProcessor() {
        defaultIndexIdFormat = DEFAULT_DEFAULT_INDEX_ID_FORMAT;
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

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @Required
    public void setBatchIndexer(BatchIndexer batchIndexer) {
        this.batchIndexer = batchIndexer;
    }

    @Override
    public void doProcess(SiteConfiguration siteConfiguration, PublishedChangeSet changeSet) throws PublishingException {
        String rootFolder = siteConfiguration.getLocalRepositoryRoot();
        String siteName = getActualSiteId(siteConfiguration);
        String indexId = getActualIndexId(siteName);

        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();
        int updateCount = 0;

        if (CollectionUtils.isNotEmpty(createdFiles)) {
            updateCount = batchIndexer.updateIndex(indexId, siteName, rootFolder, createdFiles, false);
        }
        if (CollectionUtils.isNotEmpty(updatedFiles)) {
            updateCount = batchIndexer.updateIndex(indexId, siteName, rootFolder, updatedFiles, false);
        }
        if (CollectionUtils.isNotEmpty(deletedFiles)) {
            updateCount = batchIndexer.updateIndex(indexId, siteName, rootFolder, deletedFiles, true);
        }

        if (updateCount > 0) {
            searchService.commit();
        }
    }

    protected String getActualSiteId(SiteConfiguration siteConfiguration) {
        return StringUtils.isNotEmpty(siteName)? siteName : siteConfiguration.getSiteId();
    }

    protected String getActualIndexId(String siteName) {
        if (ignoreIndexId) {
            return null;
        } else {
            return StringUtils.isNotEmpty(indexId)? indexId : String.format(defaultIndexIdFormat, siteName);
        }
    }

}
