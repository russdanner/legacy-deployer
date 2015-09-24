/*
 * Copyright (C) 2007-2013 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.cstudio.publishing.processor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.utils.XmlUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;

public class SearchUpdateFlattenXmlProcessor extends SearchUpdateProcessor {

    private static final Log logger = LogFactory.getLog(SearchUpdateFlattenXmlProcessor.class);

    protected String includeElementXPathQuery = "//include";
    protected String disableFlatteningElement = "disableFlattening";

    public void setIncludeElementXPathQuery(String includeElementXPathQuery) {
        this.includeElementXPathQuery = includeElementXPathQuery;
    }

    public void setDisableFlatteningElement(String disableFlatteningElement) {
        this.disableFlatteningElement = disableFlatteningElement;
    }

    @Override
    protected Document processDocument(String root, File file, Document document) throws DocumentException {
        document = flattenXml(root, file, document, new ArrayList<File>());
        document = super.processDocument(root, file, document);

        return document;
    }

    protected Document flattenXml(String root, File file, Document document,
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
                String includeSrcPath = root + File.separatorChar + includeElement.getTextTrim();
                if (StringUtils.isEmpty(includeSrcPath)) {
                    continue;
                }

                File includeFile = new File(includeSrcPath);
                if (includeFile.exists()) {
                    if (!flattenedFiles.contains(includeFile)) {
                        Document includeDocument = flattenXml(root, includeFile,
                                                              XmlUtils.readXml(includeFile, charEncoding),
                                                              flattenedFiles);

                        if (logger.isDebugEnabled()) {
                            logger.debug("Include found in " + file.getAbsolutePath() + ": " + includeSrcPath);
                        }

                        doInclude(includeElement, includeDocument);
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

    private void doInclude(Element includeElement, Document includeSrc) {
        List<Node> includeElementParentChildren = includeElement.getParent().content();
        int includeElementIdx = includeElementParentChildren.indexOf(includeElement);
        Element includeSrcRootElement = includeSrc.getRootElement().createCopy();

        // Remove the <include> element
        includeElementParentChildren.remove(includeElementIdx);

        // Add the src root element
        includeElementParentChildren.add(includeElementIdx, includeSrcRootElement);
    }

    @Override
    public String getName() {
        return SearchUpdateFlattenXmlProcessor.class.getSimpleName();
    }

}
