package org.craftercms.cstudio.publishing.processor;

import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;

/**
 * {@link PublishingProcessor} decorator that executes a different processor per site, or if there's no processor
 * mapping, the default processor is used.
 *
 * @author avasquez
 */
public class PerSiteConditionalProcessor extends AbstractPublishingProcessor {

    private static final Log logger = LogFactory.getLog(PerSiteConditionalProcessor.class);

    protected Map<String, PublishingProcessor> processorMappings;
    protected PublishingProcessor defaultProcessor;

    public Map<String, PublishingProcessor> getProcessorMappings() {
        return processorMappings;
    }

    public void setProcessorMappings(Map<String, PublishingProcessor> processorMappings) {
        this.processorMappings = processorMappings;
    }

    public PublishingProcessor getDefaultProcessor() {
        return defaultProcessor;
    }

    public void setDefaultProcessor(PublishingProcessor defaultProcessor) {
        this.defaultProcessor = defaultProcessor;
    }

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters,
                          PublishingTarget target) throws PublishingException {
        String siteId = parameters.get(FileUploadServlet.PARAM_SITE);
        PublishingProcessor processor = null;

        if (MapUtils.isNotEmpty(processorMappings)) {
            processor = processorMappings.get(siteId);
        }

        if (processor == null) {
            processor = defaultProcessor;
        }

        if (processor != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Executing publishing processor " + processor.getName() + " for site " + siteId);
            }

            processor.doProcess(changeSet, parameters, target);
        }
    }

    @Override
    public String getName() {
        return PerSiteConditionalProcessor.class.getSimpleName();
    }

}
