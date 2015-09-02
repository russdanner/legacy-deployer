package org.craftercms.cstudio.publishing.processor;

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
 * {@link PublishingProcessor} decorator that executes the actual processor only if at least one of the created,
 * updated or deleted files matches any of its respective include patterns and doesn't match any of its respective
 * exclude patterns.
 *
 * @author avasquez
 */
public class OnPathMatchPostProcessor implements PublishingProcessor {

    private static final Log logger = LogFactory.getLog(OnPathMatchPostProcessor.class);

    protected PublishingProcessor actualProcessor;
    protected String[] createdFilesIncludePatterns;
    protected String[] createdFilesExcludePatterns;
    protected String[] updatedFilesIncludePatterns;
    protected String[] updatedFilesExcludePatterns;
    protected String[] deletedFilesIncludePatterns;
    protected String[] deletedFilesExcludePatterns;

    @Required
    public void setActualProcessor(PublishingProcessor actualProcessor) {
        this.actualProcessor = actualProcessor;
    }

    public void setCreatedFilesIncludePatterns(String[] createdFilesIncludePatterns) {
        this.createdFilesIncludePatterns = createdFilesIncludePatterns;
    }

    public void setCreatedFilesExcludePatterns(String[] createdFilesExcludePatterns) {
        this.createdFilesExcludePatterns = createdFilesExcludePatterns;
    }

    public void setUpdatedFilesIncludePatterns(String[] updatedFilesIncludePatterns) {
        this.updatedFilesIncludePatterns = updatedFilesIncludePatterns;
    }

    public void setUpdatedFilesExcludePatterns(String[] updatedFilesExcludePatterns) {
        this.updatedFilesExcludePatterns = updatedFilesExcludePatterns;
    }

    public void setDeletedFilesIncludePatterns(String[] deletedFilesIncludePatterns) {
        this.deletedFilesIncludePatterns = deletedFilesIncludePatterns;
    }

    public void setDeletedFilesExcludePatterns(String[] deletedFilesExcludePatterns) {
        this.deletedFilesExcludePatterns = deletedFilesExcludePatterns;
    }

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters,
                          PublishingTarget target) throws PublishingException {
        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();

        if (matchFound(createdFiles, createdFilesIncludePatterns, createdFilesExcludePatterns)) {
            actualProcessor.doProcess(changeSet, parameters, target);
        } else if (matchFound(updatedFiles, updatedFilesIncludePatterns, updatedFilesExcludePatterns)) {
            actualProcessor.doProcess(changeSet, parameters, target);
        } else if (matchFound(deletedFiles, deletedFilesIncludePatterns, deletedFilesExcludePatterns)) {
            actualProcessor.doProcess(changeSet, parameters, target);
        }
    }

    @Override
    public String getName() {
        return OnPathMatchPostProcessor.class.getName();
    }
    
    protected boolean matchFound(List<String> paths, String[] includePatterns, String[] excludePatterns) {
        if (CollectionUtils.isNotEmpty(paths)) {
            for (String path : paths) {
                if (matchesAnyPattern(path, includePatterns) && !matchesAnyPattern(path, excludePatterns)) {
                    return true;
                }
            }
        }

        return false;
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

}
