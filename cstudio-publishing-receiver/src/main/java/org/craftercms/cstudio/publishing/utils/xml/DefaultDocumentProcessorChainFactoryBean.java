package org.craftercms.cstudio.publishing.utils.xml;

import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Spring FactoryBean used to facilitate the creation of the default {@link DocumentProcessorChain}, that includes
 * field renaming an tokenize attribute parsing.
 *
 * @author avasquez
 */
public class DefaultDocumentProcessorChainFactoryBean extends AbstractFactoryBean<DocumentProcessorChain> {

    protected Map<String, String> fieldMappings;
    protected String tokenizeAttribute;
    protected Map<String, String> tokenizeSubstitutionMap;

    public void setFieldMappings(Map<String, String> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    public void setTokenizeAttribute(String tokenizeAttribute) {
        this.tokenizeAttribute = tokenizeAttribute;
    }

    public void setTokenizeSubstitutionMap(Map<String, String> tokenizeSubstitutionMap) {
        this.tokenizeSubstitutionMap = tokenizeSubstitutionMap;
    }

    @Override
    public Class<?> getObjectType() {
        return DocumentProcessorChain.class;
    }

    @Override
    protected DocumentProcessorChain createInstance() throws Exception {
        return addProcessors(new DocumentProcessorChain());
    }

    protected DocumentProcessorChain addProcessors(DocumentProcessorChain chain) {
        FieldRenamingDocumentProcessor fieldRenamingProcessor = new FieldRenamingDocumentProcessor();
        if (MapUtils.isNotEmpty(fieldMappings)) {
            fieldRenamingProcessor.setFieldMappings(fieldMappings);
        }

        TokenizeAttributeParsingDocumentProcessor tokenizeProcessor = new TokenizeAttributeParsingDocumentProcessor();
        if (StringUtils.isNotEmpty(tokenizeAttribute)) {
            tokenizeProcessor.setTokenizeAttribute(tokenizeAttribute);
        }
        if (MapUtils.isNotEmpty(tokenizeSubstitutionMap)) {
            tokenizeProcessor.setTokenizeSubstitutionMap(tokenizeSubstitutionMap);
        }

        chain.addProcessor(fieldRenamingProcessor);
        chain.addProcessor(tokenizeProcessor);

        return chain;
    }

}
