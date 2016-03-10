package org.craftercms.cstudio.publishing.utils.xml;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 * Created by alfonsovasquez on 4/3/16.
 */
public interface DocumentProcessor {

    Document process(Document document, File file, String rootFolder) throws DocumentException;

}
