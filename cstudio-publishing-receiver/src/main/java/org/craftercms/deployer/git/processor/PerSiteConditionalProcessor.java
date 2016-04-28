package org.craftercms.deployer.git.processor;

import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.deployer.git.config.SiteConfiguration;

/**
 * {@link PublishingProcessor} decorator that executes a different processor per site, or if there's no processor
 * mapping, the default processor is used.
 *
 * @author avasquez
 */
public class PerSiteConditionalProcessor implements PublishingProcessor {

    private static final Log logger = LogFactory.getLog(PerSiteConditionalProcessor.class);

    protected Map<String, PublishingProcessor> processorMappings;
    protected PublishingProcessor defaultProcessor;
    protected int order = Integer.MAX_VALUE;

    @Override
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public Map<String, PublishingProcessor> getProcessorMappings() { return processorMappings; }
    public void setProcessorMappings(Map<String, PublishingProcessor> processorMappings) {
        this.processorMappings = processorMappings;
    }

    public PublishingProcessor getDefaultProcessor() { return defaultProcessor; }
    public void setDefaultProcessor(PublishingProcessor defaultProcessor) {
        this.defaultProcessor = defaultProcessor;
    }

    @Override
    public void doProcess(SiteConfiguration siteConfiguration, PublishedChangeSet changeSet) throws PublishingException {
        String siteId = siteConfiguration.getSiteId();
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

            processor.doProcess(siteConfiguration, changeSet);
        }
    }

    @Override
    public String getName() {
        return PerSiteConditionalProcessor.class.getSimpleName();
    }

}
