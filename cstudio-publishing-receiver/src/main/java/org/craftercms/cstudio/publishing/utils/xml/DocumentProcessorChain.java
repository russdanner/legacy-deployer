package org.craftercms.cstudio.publishing.utils.xml;

import java.io.File;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 * Created by alfonsovasquez on 4/3/16.
 */
public class DocumentProcessorChain implements DocumentProcessor {

    protected List<DocumentProcessor> processors;

    public DocumentProcessorChain() {
    }

    public DocumentProcessorChain(List<DocumentProcessor> processors) {
        this.processors = processors;
    }

    public void setProcessors(List<DocumentProcessor> processors) {
        this.processors = processors;
    }

    @Override
    public Document process(Document document, File file, String rootFolder) throws DocumentException {
        if (CollectionUtils.isNotEmpty(processors)) {
            for (DocumentProcessor processor : processors) {
                document = processor.process(document, file, rootFolder);
            }
        }

        return document;
    }

}
