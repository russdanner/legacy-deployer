package org.craftercms.cstudio.publishing.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link PublishingProcessor} decorator that maps file path patterns to post processors, so if files of the change
 * set match a pattern, the corresponding post processor is called for those files
 *
 * @author avasquez
 */
public class OnPathMatchConditionalProcessor implements PublishingProcessor {

    private static final Log logger = LogFactory.getLog(OnPathMatchConditionalProcessor.class);

    protected Map<String[], PublishingProcessor> processorMappings;

    @Required
    public void setProcessorMappings(Map<String[], PublishingProcessor> processorMappings) {
        this.processorMappings = processorMappings;
    }

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters,
                          PublishingTarget target) throws PublishingException {
        List<String> createdFiles = copyFileList(changeSet.getCreatedFiles());
        List<String> updatedFiles = copyFileList(changeSet.getUpdatedFiles());
        List<String> deletedFiles = copyFileList(changeSet.getDeletedFiles());

        for (Map.Entry<String[], PublishingProcessor> entry : processorMappings.entrySet()) {
            String[] patterns = entry.getKey();
            PublishingProcessor processor = entry.getValue();
            List<String> matchedCreatedFiles = new ArrayList<>();
            List<String> matchedUpdatedFiles = new ArrayList<>();
            List<String> matchedDeletedFiles = new ArrayList<>();

            for (Iterator<String> iter = createdFiles.iterator(); iter.hasNext(); ) {
                String path = iter.next();
                if (matchesAnyPattern(path, patterns)) {
                    matchedCreatedFiles.add(path);
                    iter.remove();
                }
            }
            for (Iterator<String> iter = updatedFiles.iterator(); iter.hasNext(); ) {
                String path = iter.next();
                if (matchesAnyPattern(path, patterns)) {
                    matchedUpdatedFiles.add(path);
                    iter.remove();
                }
            }
            for (Iterator<String> iter = deletedFiles.iterator(); iter.hasNext(); ) {
                String path = iter.next();
                if (matchesAnyPattern(path, patterns)) {
                    matchedDeletedFiles.add(path);
                    iter.remove();
                }
            }

            if (CollectionUtils.isNotEmpty(matchedCreatedFiles) ||
                CollectionUtils.isNotEmpty(matchedUpdatedFiles) ||
                CollectionUtils.isNotEmpty(matchedDeletedFiles)) {
                PublishedChangeSet newChangeSet = new PublishedChangeSet();
                newChangeSet.setCreatedFiles(matchedCreatedFiles);
                newChangeSet.setUpdatedFiles(matchedUpdatedFiles);
                newChangeSet.setDeletedFiles(deletedFiles);

                if (logger.isDebugEnabled()) {
                    logger.debug("Executing publishing processor " + processor.getName() + " for " + newChangeSet);
                }

                processor.doProcess(newChangeSet, parameters, target);
            }
        }
    }

    @Override
    public String getName() {
        return OnPathMatchConditionalProcessor.class.getSimpleName();
    }

    protected boolean matchesAnyPattern(String path, String[] patterns) {
        if (StringUtils.isNotEmpty(path) && ArrayUtils.isNotEmpty(patterns)) {
            for (String pattern : patterns) {
                if (path.matches(pattern)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(path + " matched " + pattern);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    protected List<String> copyFileList(List<String> files) {
        return CollectionUtils.isNotEmpty(files)? new ArrayList<>(files) : Collections.<String>emptyList();
    }

}
