package org.craftercms.cstudio.publishing.utils.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.utils.XmlUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;

/**
 * Created by alfonsovasquez on 4/3/16.
 */
public class FlatteningDocumentProcessor implements DocumentProcessor {

    public static final String DEFAULT_CHAR_ENCODING = CharEncoding.UTF_8;
    public static final String DEFAULT_INCLUDE_ELEMENT_XPATH_QUERY = "//include";
    public static final String DEFAULT_DISABLE_FLATTENING_ELEMENT  = "disabledFlattening";
    private static final String PAGE_CONTENT_NAME = "page";

    private static final Log logger = LogFactory.getLog(FlatteningDocumentProcessor.class);


    protected String charEncoding;
    protected String includeElementXPathQuery;
    protected String disableFlatteningElement;
    protected boolean disableNestedPageFlattening;

    public FlatteningDocumentProcessor() {
        charEncoding = DEFAULT_CHAR_ENCODING;
        includeElementXPathQuery = DEFAULT_INCLUDE_ELEMENT_XPATH_QUERY;
        disableFlatteningElement = DEFAULT_DISABLE_FLATTENING_ELEMENT;
    }

    public void setIncludeElementXPathQuery(String includeElementXPathQuery) {
        this.includeElementXPathQuery = includeElementXPathQuery;
    }

    public void setDisableFlatteningElement(String disableFlatteningElement) {
        this.disableFlatteningElement = disableFlatteningElement;
    }

    public void setCharEncoding(String charEncoding) {
        this.charEncoding = charEncoding;
    }

    @Override
    public Document process(Document document, File file, String rootFolder) throws DocumentException {
        flattenXml(document, file, rootFolder, new ArrayList<File>());

        return document;
    }

    protected Document flattenXml(Document document, File file, String rootFolder,
                                  List<File> flattenedFiles) throws DocumentException {
        flattenedFiles.add(file);

        List<Element> includeElements = document.selectNodes(includeElementXPathQuery);

        if (CollectionUtils.isEmpty(includeElements)) {
            return document;
        }

        for (Element includeElement : includeElements) {
            boolean flatteningDisabled = false;
            Element parent = includeElement.getParent();
            Element disableFlatteningNode = parent.element(disableFlatteningElement);

            if (disableFlatteningNode != null) {
                String disableFlattening = disableFlatteningNode.getText();
                flatteningDisabled = Boolean.parseBoolean(disableFlattening);
            }

            if (!flatteningDisabled) {
                String includeSrcPath = rootFolder + File.separatorChar + includeElement.getTextTrim();
                if (StringUtils.isEmpty(includeSrcPath)) {
                    continue;
                }

                File includeFile = new File(includeSrcPath);
                if (includeFile.exists()) {
                    if (!flattenedFiles.contains(includeFile)) {
                        Document includeDocument = flattenXml(XmlUtils.readXml(includeFile, charEncoding),
                                                              includeFile, rootFolder, flattenedFiles);

                        if (logger.isDebugEnabled()) {
                            logger.debug("Include found in " + file.getAbsolutePath() + ": " + includeSrcPath);
                        }

                        if(disableNestedPageFlattening){
                            if(!isPage(includeDocument.getRootElement())) {
                                doInclude(includeElement, includeDocument);
                            }
                        }else{
                            doInclude(includeElement, includeDocument);
                        }
                    } else {
                        logger.warn("Circular inclusion detected. File " + includeFile + " already included");
                    }
                } else {
                    logger.warn("No file found for include at " + includeFile);
                }
            }
        }

        return document;
    }

    private boolean isPage(final Node rootElement) {

        return rootElement.getName().equalsIgnoreCase(PAGE_CONTENT_NAME);
    }

    protected void doInclude(Element includeElement, Document includeSrc) {
        List<Node> includeElementParentChildren = includeElement.getParent().content();
        int includeElementIdx = includeElementParentChildren.indexOf(includeElement);
        Element includeSrcRootElement = includeSrc.getRootElement().createCopy();

        // Remove the <include> element
        includeElementParentChildren.remove(includeElementIdx);

        // Add the src root element
        includeElementParentChildren.add(includeElementIdx, includeSrcRootElement);
    }

    public void setDisableNestedPageFlattening(final boolean disableNestedPageFlattening) {
        this.disableNestedPageFlattening = disableNestedPageFlattening;
    }
}
