package org.crafetrcms.deployer.git.processor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.utils.xml.DefaultDocumentProcessorChainFactoryBean;
import org.craftercms.cstudio.publishing.utils.xml.DocumentProcessor;
import org.craftercms.deployer.git.config.SiteConfiguration;
import org.craftercms.deployer.git.processor.PublishingProcessor;
import org.craftercms.deployer.git.processor.search.XmlFileIndexingProcessor;
import org.craftercms.search.service.SearchService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by alfonsovasquez on 22/4/16.
 */
public class XmlFileIndexingProcessorTest {

    private static final String SITE_NAME = "test";
    private static final String UPDATE_FILENAME = "test.xml";
    private static final String DELETE_FILENAME = "deleteme.xml";
    private static final String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                               "<page>" +
                                               "<fileName>test.xml</fileName>" +
                                               "<title_s tokenize=\"true\">Test</title_s>" +
                                               "<date>11/10/2015 00:00:00</date>" +
                                               "<title_t tokenize=\"true\">Test</title_t>" +
                                               "</page>";

    private SearchService searchService;
    private PublishingProcessor processor;

    @Before
    public void setUp() throws Exception {
        searchService = getSearchService();
        processor = getPublishingProcessor(searchService);
    }

    @Test
    public void testProcess() throws Exception {
        SiteConfiguration siteConfiguration = mock(SiteConfiguration.class);
        when(siteConfiguration.getLocalRepositoryRoot()).thenReturn(getLocalRepositoryRoot());
        when(siteConfiguration.getSiteId()).thenReturn(getSiteName());

        PublishedChangeSet changeSet = new PublishedChangeSet();
        changeSet.setUpdatedFiles(Arrays.asList(getUpdateFilename()));
        changeSet.setDeletedFiles(Arrays.asList(getDeleteFilename()));

        processor.doProcess(siteConfiguration, changeSet);

        verify(searchService).update(getIndexId(), getSiteName(), getUpdateFilename(), getExpectedXml(), true);
        verify(searchService).delete(getIndexId(), getSiteName(), getDeleteFilename());
    }

    protected DocumentProcessor getDocumentProcessor() throws Exception {
        DefaultDocumentProcessorChainFactoryBean chainFactoryBean = new DefaultDocumentProcessorChainFactoryBean();
        chainFactoryBean.setFieldMappings(Collections.singletonMap("//name", "fileName"));
        chainFactoryBean.afterPropertiesSet();

        return chainFactoryBean.getObject();
    }

    protected SearchService getSearchService() throws Exception {
        return mock(SearchService.class);
    }

    protected PublishingProcessor getPublishingProcessor(SearchService searchService) throws Exception {
        XmlFileIndexingProcessor processor = new XmlFileIndexingProcessor();
        processor.setDocumentProcessor(getDocumentProcessor());
        processor.setSearchService(searchService);
        processor.init();

        return processor;
    }

    protected String getIndexId() {
        return getSiteName() + "-default";
    }

    protected String getSiteName() {
        return SITE_NAME;
    }

    protected String getUpdateFilename() {
        return UPDATE_FILENAME;
    }

    protected String getDeleteFilename() {
        return DELETE_FILENAME;
    }

    protected String getExpectedXml() {
        return EXPECTED_XML;
    }

    protected String getLocalRepositoryRoot() throws IOException {
        return new ClassPathResource("/docs").getFile().getAbsolutePath();
    }

}
