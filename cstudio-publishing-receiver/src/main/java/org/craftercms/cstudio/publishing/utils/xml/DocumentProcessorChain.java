package org.craftercms.cstudio.publishing.utils.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 * Represents a chain of {@link DocumentProcessor}.
 *
 * @author avasquez
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

    public void addProcessor(DocumentProcessor processor) {
        if (processors == null) {
            processors = new ArrayList<>();
        }

        processors.add(processor);
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
