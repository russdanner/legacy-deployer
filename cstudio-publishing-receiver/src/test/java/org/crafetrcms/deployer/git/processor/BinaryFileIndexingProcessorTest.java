package org.crafetrcms.deployer.git.processor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.deployer.git.config.SiteConfiguration;
import org.craftercms.deployer.git.processor.PublishingProcessor;
import org.craftercms.deployer.git.processor.search.BinaryFileIndexingProcessor;
import org.craftercms.search.service.SearchService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.mockito.Mockito.*;

/**
 * Created by alfonsovasquez on 26/4/16.
 */
public class BinaryFileIndexingProcessorTest {

    private static final String SITE_NAME = "test";
    private static final String SUPPORTED_FILENAME = "document.pdf";
    private static final String NON_SUPPORTED_FILENAME = "image.jpg";

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
        changeSet.setUpdatedFiles(Arrays.asList(SUPPORTED_FILENAME));
        changeSet.setDeletedFiles(Arrays.asList(NON_SUPPORTED_FILENAME));

        processor.doProcess(siteConfiguration, changeSet);

        String indexId = SITE_NAME + "-default";
        File supportedFile = new File(root, SUPPORTED_FILENAME);

        verify(searchService).updateFile(indexId, SITE_NAME, SUPPORTED_FILENAME, supportedFile);
        verify(searchService, never()).delete(indexId, SITE_NAME, NON_SUPPORTED_FILENAME);
    }

    protected SearchService getSearchService() throws Exception {
        return mock(SearchService.class);
    }

    protected PublishingProcessor getPublishingProcessor(SearchService searchService) throws Exception {
        BinaryFileIndexingProcessor processor = new BinaryFileIndexingProcessor();
        processor.setSupportedMimeTypes(Arrays.asList("application/pdf"));
        processor.setSearchService(searchService);

        return processor;
    }

    protected String getLocalRepositoryRoot() throws IOException {
        return new ClassPathResource("/docs").getFile().getAbsolutePath();
    }

}
