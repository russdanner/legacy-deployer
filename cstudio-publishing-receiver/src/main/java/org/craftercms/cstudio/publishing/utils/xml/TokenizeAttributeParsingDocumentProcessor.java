package org.craftercms.cstudio.publishing.utils.xml;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

/**
 * {@link DocumentProcessor} that parses elements that have a "tokenize" attribute. For every element with "tokenize",
 * a copy is created but with a slightly different suffix so that Solr understands that it has to be analyzed and
 * tokenized.
 *
 * @author avasquez
 */
public class TokenizeAttributeParsingDocumentProcessor implements DocumentProcessor {

    public static final String DEFAULT_TOKENIZE_ATTRIBUTE = "tokenize";

    private static final Log logger = LogFactory.getLog(FlatteningDocumentProcessor.class);

    protected String tokenizeAttribute;
    protected Map<String, String> tokenizeSubstitutionMap;

    public TokenizeAttributeParsingDocumentProcessor() {
        tokenizeAttribute = DEFAULT_TOKENIZE_ATTRIBUTE;
        tokenizeSubstitutionMap = new HashMap<>(2);

        tokenizeSubstitutionMap.put("_s", "_t");
        tokenizeSubstitutionMap.put("_smv", "_tmv");
    }

    public void setTokenizeAttribute(String tokenizeAttribute) {
        this.tokenizeAttribute = tokenizeAttribute;
    }

    public void setTokenizeSubstitutionMap(Map<String, String> tokenizeSubstitutionMap) {
        this.tokenizeSubstitutionMap = tokenizeSubstitutionMap;
    }

    @Override
    public Document process(Document document, File file, String rootFolder) throws DocumentException {
        if (MapUtils.isNotEmpty(tokenizeSubstitutionMap)) {
            String tokenizeXpath = String.format("//*[@%s=\"true\"]", tokenizeAttribute);
            if (logger.isDebugEnabled()) {
                logger.debug("Performing tokenize parsing with XPath " + tokenizeXpath + " for file " + file + "...");
            }

            List<Element> tokenizeElements = document.selectNodes(tokenizeXpath);
            if (logger.isDebugEnabled()) {
                logger.debug("Number of elements found to perform tokenize parsing: " + tokenizeElements.size());
            }

            if (CollectionUtils.isEmpty(tokenizeElements)) {
                return document;
            }

            for (Element tokenizeElement : tokenizeElements) {
                Element parent = tokenizeElement.getParent();
                String elemName = tokenizeElement.getName();

                if (logger.isDebugEnabled()) {
                    logger.debug("Parsing element " + tokenizeElement.getUniquePath());
                }

                for (String substitutionKey : tokenizeSubstitutionMap.keySet()) {
                    if (elemName.endsWith(substitutionKey)) {
                        String newElementName = elemName.substring(0, elemName.length() - substitutionKey.length()) +
                                                tokenizeSubstitutionMap.get(substitutionKey);

                        Element newElement = tokenizeElement.createCopy(newElementName);
                        parent.add(newElement);

                        if (logger.isDebugEnabled()) {
                            logger.debug("Added new element for tokenized search: " + newElement.getUniquePath());
                        }
                    }
                }
            }
        }

        return document;
    }

}
