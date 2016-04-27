/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.deployer.git.processor.search;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.utils.XmlUtils;
import org.craftercms.cstudio.publishing.utils.xml.DefaultDocumentProcessorChainFactoryBean;
import org.craftercms.cstudio.publishing.utils.xml.DocumentProcessor;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * {@link org.craftercms.deployer.git.processor.PublishingProcessor} that updates/deletes XML files from a
 * search index. The XML files are first processed by the default document processor chain.
 *
 * @author avasquez
 */
public class XmlFileIndexingProcessor extends AbstractIndexingProcessor {

    private static final Log logger = LogFactory.getLog(XmlFileIndexingProcessor.class);

    protected DocumentProcessor documentProcessor;
    protected String charEncoding;

    public XmlFileIndexingProcessor() {
        charEncoding = "UTF-8";
    }

    public void setDocumentProcessor(DocumentProcessor documentProcessor) {
        this.documentProcessor = documentProcessor;
    }

    public void setCharEncoding(String charEncoding) {
        this.charEncoding = charEncoding;
    }

    @PostConstruct
    public void init() throws Exception {
        if (documentProcessor == null) {
            DefaultDocumentProcessorChainFactoryBean factoryBean = new DefaultDocumentProcessorChainFactoryBean();
            factoryBean.afterPropertiesSet();

            documentProcessor = factoryBean.getObject();
        }
    }

    @Override
    public String getName() {
        return XmlFileIndexingProcessor.class.getSimpleName();
    }

    @Override
    protected int update(String indexId, String siteName, String root, List<String> fileNames,
                         boolean delete) throws PublishingException {
        int updateCount = 0;

        for (String fileName : fileNames) {
            if (fileName.endsWith(".xml")) {
                File file = new File(root, fileName);

                try {
                    if (delete) {
                        doDelete(indexId, siteName, fileName, file);
                        updateCount++;
                    } else {
                        try {
                            String xml = processXml(root, file);
                            doUpdate(indexId, siteName, fileName, file, xml);
                            updateCount++;
                        } catch (DocumentException e) {
                            logger.warn("Cannot process XML file " + file + ". Continuing index update...", e);
                        }
                    }
                } catch (Exception e) {
                    throw new PublishingException(e);
                }
            }
        }

        return updateCount;
    }

    protected String processXml(String root, File file) throws DocumentException {
        Document document = processDocument(XmlUtils.readXml(file, charEncoding), file, root);
        String xml = documentToString(document);

        if (logger.isDebugEnabled()) {
            logger.debug("Processed XML file " + file + ":");
            logger.debug(xml);
        }

        return xml;
    }

    protected Document processDocument(Document document, File file, String root) throws DocumentException {
        return documentProcessor.process(document, file, root);
    }

    protected String documentToString(Document document) {
        StringWriter stringWriter = new StringWriter();
        OutputFormat format = OutputFormat.createCompactFormat();
        XMLWriter xmlWriter = new XMLWriter(stringWriter, format);

        try {
            xmlWriter.write(document);
        } catch (IOException e) {
            // Ignore, shouldn't happen.
        }

        return stringWriter.toString();
    }

}
