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
 * Created by alfonsovasquez on 4/3/16.
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
                logger.debug("Using tokenize XPath: " + tokenizeXpath);
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
                    logger.debug("Parsing element: " + elemName);
                }

                for (String substitutionKey : tokenizeSubstitutionMap.keySet()) {
                    if (elemName.endsWith(substitutionKey)) {
                        String newElementName = elemName.substring(0, elemName.length() - substitutionKey.length()) +
                                                tokenizeSubstitutionMap.get(substitutionKey);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Adding new element for tokenized search: " + newElementName);
                        }

                        Element newElement = tokenizeElement.createCopy(newElementName);
                        parent.add(newElement);
                    }
                }
            }
        }

        return document;
    }

}
