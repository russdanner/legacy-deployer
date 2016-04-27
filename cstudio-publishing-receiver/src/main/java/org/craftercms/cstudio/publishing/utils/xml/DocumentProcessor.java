package org.craftercms.cstudio.publishing.utils.xml;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 * Processes an XML DOM to modify or enhance it.
 *
 * @author avasquez
 */
public interface DocumentProcessor {

    /**
     * Processes the specified XML DOM.
     *
     * @param document      the DOM
     * @param file          the XML file
     * @param rootFolder    the root folder where this file is located
     *
     * @return the processed DOM
     */
    Document process(Document document, File file, String rootFolder) throws DocumentException;

}
