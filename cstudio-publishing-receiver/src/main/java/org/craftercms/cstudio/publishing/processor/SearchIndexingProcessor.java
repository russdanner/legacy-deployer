package org.craftercms.cstudio.publishing.processor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.lang.UrlUtils;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link PublishingProcessor} that uses a list of {@link BatchIndexer}s to update a Crafter Search index. This
 * processor replaces all previous search processors.
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
    protected List<BatchIndexer> batchIndexers;

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

    public void setBatchIndexer(BatchIndexer batchIndexer) {
        this.batchIndexers = Collections.singletonList(batchIndexer);
    }

    public void setBatchIndexers(List<BatchIndexer> batchIndexers) {
        this.batchIndexers = batchIndexers;
    }

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters,
                          PublishingTarget target) throws PublishingException {
        if (CollectionUtils.isEmpty(batchIndexers)) {
            throw new IllegalStateException("At least one batch indexer should be provided");
        }

        String rootFolder = target.getParameter(FileUploadServlet.CONFIG_ROOT);
        String contentFolder = target.getParameter(FileUploadServlet.CONFIG_CONTENT_FOLDER);
        String siteName = getActualSiteId(parameters);
        String indexId = getActualIndexId(siteName);
        int updateCount = 0;

        rootFolder = UrlUtils.concat(rootFolder, contentFolder);

        if (StringUtils.isNotBlank(siteName)) {
            rootFolder = rootFolder.replaceAll(FileUploadServlet.CONFIG_MULTI_TENANCY_VARIABLE, siteName);
        }

        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();

        if (CollectionUtils.isNotEmpty(createdFiles)) {
            for (BatchIndexer indexer : batchIndexers) {
                updateCount += indexer.updateIndex(indexId, siteName, rootFolder, createdFiles, false);
            }
        }
        if (CollectionUtils.isNotEmpty(updatedFiles)) {
            for (BatchIndexer indexer : batchIndexers) {
                updateCount += indexer.updateIndex(indexId, siteName, rootFolder, updatedFiles, false);
            }
        }
        if (CollectionUtils.isNotEmpty(deletedFiles)) {
            for (BatchIndexer indexer : batchIndexers) {
                updateCount += indexer.updateIndex(indexId, siteName, rootFolder, deletedFiles, true);
            }
        }

        if (updateCount > 0) {
            searchService.commit(indexId);
        }
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
