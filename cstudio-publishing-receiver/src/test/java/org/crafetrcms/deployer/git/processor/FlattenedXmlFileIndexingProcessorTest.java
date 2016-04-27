package org.crafetrcms.deployer.git.processor;

import java.util.Collections;

import org.craftercms.cstudio.publishing.utils.xml.DocumentProcessor;
import org.craftercms.cstudio.publishing.utils.xml.FlatteningDocumentProcessorChainFactoryBean;
import org.craftercms.deployer.git.processor.PublishingProcessor;
import org.craftercms.deployer.git.processor.search.FlattenedXmlFileIndexingProcessor;
import org.craftercms.search.service.SearchService;

/**
 * Created by alfonsovasquez on 25/4/16.
 */
public class FlattenedXmlFileIndexingProcessorTest extends XmlFileIndexingProcessorTest {

    private static final String UPDATE_FILENAME = "test2.xml";
    private static final String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                               "<page>" +
                                               "<fileName>test2.xml</fileName>" +
                                               "<title_s tokenize=\"true\">Test</title_s>" +
                                               "<date>11/10/2015 00:00:00</date>" +
                                               "<component>" +
                                               "<fileName>component.xml</fileName>" +
                                               "<title_s tokenize=\"true\">Test</title_s>" +
                                               "<date>11/11/2015 10:00:00</date>" +
                                               "<title_t tokenize=\"true\">Test</title_t>" +
                                               "</component>" +
                                               "<title_t tokenize=\"true\">Test</title_t>" +
                                               "</page>";

    protected DocumentProcessor getDocumentProcessor() throws Exception {
        FlatteningDocumentProcessorChainFactoryBean chainFactoryBean = new FlatteningDocumentProcessorChainFactoryBean();
        chainFactoryBean.setFieldMappings(Collections.singletonMap("//name", "fileName"));
        chainFactoryBean.afterPropertiesSet();

        return chainFactoryBean.getObject();
    }

    @Override
    protected PublishingProcessor getPublishingProcessor(SearchService searchService) throws Exception {
        FlattenedXmlFileIndexingProcessor processor = new FlattenedXmlFileIndexingProcessor();
        processor.setDocumentProcessor(getDocumentProcessor());
        processor.setSearchService(searchService);
        processor.init();

        return processor;
    }

    @Override
    protected String getUpdateFilename() {
        return UPDATE_FILENAME;
    }

    @Override
    protected String getExpectedXml() {
        return EXPECTED_XML;
    }

}
