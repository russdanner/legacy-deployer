package org.craftercms.cstudio.publishing.utils.xml;

import org.apache.commons.lang.StringUtils;

/**
 * Spring FactoryBean used to facilitate the creation of a default {@link DocumentProcessorChain} that also includes
 * flattening.
 *
 * @author avasquez
 */
public class FlatteningDocumentProcessorChainFactoryBean extends DefaultDocumentProcessorChainFactoryBean {

    protected String includeElementXPathQuery;
    protected String disableFlatteningElement;

    public void setIncludeElementXPathQuery(String includeElementXPathQuery) {
        this.includeElementXPathQuery = includeElementXPathQuery;
    }

    public void setDisableFlatteningElement(String disableFlatteningElement) {
        this.disableFlatteningElement = disableFlatteningElement;
    }

    @Override
    protected DocumentProcessorChain addProcessors(DocumentProcessorChain chain) {
        FlatteningDocumentProcessor processor = new FlatteningDocumentProcessor();
        if (StringUtils.isNotEmpty(includeElementXPathQuery)) {
            processor.setIncludeElementXPathQuery(includeElementXPathQuery);
        }
        if (StringUtils.isNotEmpty(disableFlatteningElement)) {
            processor.setDisableFlatteningElement(disableFlatteningElement);
        }

        chain.addProcessor(processor);

        return super.addProcessors(chain);
    }

}
