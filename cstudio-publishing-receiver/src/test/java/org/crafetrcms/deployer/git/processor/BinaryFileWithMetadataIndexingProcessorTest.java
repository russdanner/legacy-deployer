package org.crafetrcms.deployer.git.processor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.deployer.git.config.SiteConfiguration;
import org.craftercms.deployer.git.processor.PublishingProcessor;
import org.craftercms.deployer.git.processor.search.BinaryFileWithMetadataIndexingProcessor;
import org.craftercms.search.service.SearchService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by alfonsovasquez on 26/4/16.
 */
public class BinaryFileWithMetadataIndexingProcessorTest {

    private static final String SITE_NAME = "test";
    private static final String METADATA_FILENAME = "metadata.xml";
    private static final String BINARY_FILENAME = "logo.jpg";
    private static final String DELETE_FILENAME = "oldlogo.jpg";

    private SearchService searchService;
    private PublishingProcessor processor;

    @Before
    public void setUp() throws Exception {
        searchService = getSearchService();
        processor = getPublishingProcessor(searchService);
    }

    @Test
    public void testProcess() throws Exception {
        String root = getLocalRepositoryRoot();

        SiteConfiguration siteConfiguration = mock(SiteConfiguration.class);
        when(siteConfiguration.getLocalRepositoryRoot()).thenReturn(root);
        when(siteConfiguration.getSiteId()).thenReturn(SITE_NAME);

        PublishedChangeSet changeSet = new PublishedChangeSet();
        changeSet.setUpdatedFiles(Arrays.asList(METADATA_FILENAME));
        changeSet.setDeletedFiles(Arrays.asList(DELETE_FILENAME));

        processor.doProcess(siteConfiguration, changeSet);

        String indexId = SITE_NAME + "-default";
        File binaryFile = new File(root, BINARY_FILENAME);

        verify(searchService).updateFile(indexId, SITE_NAME, BINARY_FILENAME, binaryFile, getExpectedMetadata());
        verify(searchService).delete(indexId, SITE_NAME, DELETE_FILENAME);
    }

    protected SearchService getSearchService() throws Exception {
        return mock(SearchService.class);
    }

    protected PublishingProcessor getPublishingProcessor(SearchService searchService) throws Exception {
        BinaryFileWithMetadataIndexingProcessor processor = new BinaryFileWithMetadataIndexingProcessor();
        processor.setMetadataPathPatterns(Arrays.asList(".*\\.xml$"));
        processor.setBinaryPathPatterns(Arrays.asList(".*\\.jpg$"));
        processor.setExcludeMetadataProperties(Arrays.asList("objectId"));
        processor.setReferenceXPaths(Arrays.asList("//attachment"));
        processor.setSearchService(searchService);
        processor.init();

        return processor;
    }

    protected String getLocalRepositoryRoot() throws IOException {
        return new ClassPathResource("/docs").getFile().getAbsolutePath();
    }

    protected Map<String, List<String>> getExpectedMetadata() {
        MultiValueMap<String, String> metadata = new LinkedMultiValueMap<>();
        metadata.add("fileName", "metadata.xml");
        metadata.add("attachmentText_s", "Logo");
        metadata.add("attachment", "logo.jpg");
        metadata.add("attachmentText_t", "Logo");

        return metadata;
    }

}
