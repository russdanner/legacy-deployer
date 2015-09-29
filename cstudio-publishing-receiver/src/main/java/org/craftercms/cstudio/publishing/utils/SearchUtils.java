/*
 * Copyright (C) 2007-2015 Crafter Software Corporation.
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
package org.craftercms.cstudio.publishing.utils;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;

public class SearchUtils {

    private static final Log logger = LogFactory.getLog(SearchUtils.class);

    private SearchUtils() {
    }

    public static Document renameFields(Document document, Map<String, String> fieldMappings) {
        if (MapUtils.isNotEmpty(fieldMappings)) {
            for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
                String xpath = entry.getKey();
                String newName = entry.getValue();

                if (logger.isDebugEnabled()) {
                    logger.debug("Renaming elements that match XPath " + xpath + " to '" + newName + "'");
                }

                List<Element> elements = document.selectNodes(xpath);
                if (CollectionUtils.isNotEmpty(elements)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Number of elements found to rename: " + elements.size());
                    }

                    for (Element element : elements) {
                        element.setName(newName);
                    }
                }
            }
        }

        return document;
    }

    public static Document processTokenizeAttributes(Document document, String tokenizeAttribute,
                                                     Map<String, String> tokenizeSubstitutionMap) {
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
