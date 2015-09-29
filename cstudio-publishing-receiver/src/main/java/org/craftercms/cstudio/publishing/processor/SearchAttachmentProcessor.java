/*
 * Copyright (C) 2007-2013 Crafter Software Corporation.
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
package org.craftercms.cstudio.publishing.processor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;

public class SearchAttachmentProcessor implements PublishingProcessor {

    private static final Log logger = LogFactory.getLog(SearchAttachmentProcessor.class);

    private String siteName;
    private SearchService searchService;
    private List<String> supportedMimeTypes;

    /**
     * set a sitename to override in index
     *
     * @param siteName an override siteName in index
     */
    public void setSiteName(String siteName) {
        if (!StringUtils.isEmpty(siteName)) {
            // check if it is preview for backward compatibility
            if (!SITE_NAME_PREVIEW.equalsIgnoreCase(siteName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Overriding site name in index with " + siteName);
                }
                this.siteName = siteName;
            }
        }
    }

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    @Required
    public void setSupportedMimeTypes(List<String> supportedMimeTypes) {
        this.supportedMimeTypes = supportedMimeTypes;
    }

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters,
                          PublishingTarget target) throws PublishingException {
        String root = target.getParameter(FileUploadServlet.CONFIG_ROOT);
        String contentFolder = target.getParameter(FileUploadServlet.CONFIG_CONTENT_FOLDER);
        String siteId = (!StringUtils.isEmpty(siteName))? siteName: parameters.get(FileUploadServlet.PARAM_SITE);

        root += "/" + contentFolder;
        if (org.springframework.util.StringUtils.hasText(siteId)) {
            root = root.replaceAll(FileUploadServlet.CONFIG_MULTI_TENANCY_VARIABLE, siteId);
        }

        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();

        try {
            if (CollectionUtils.isNotEmpty(createdFiles)) {
                update(siteId, root, createdFiles, false);
            }
            if (CollectionUtils.isNotEmpty(updatedFiles)) {
                update(siteId, root, updatedFiles, false);
            }
            if (CollectionUtils.isNotEmpty(deletedFiles)) {
                update(siteId, root, deletedFiles, true);
            }
        } catch (Exception exc) {
            int x = 0;
        }
    }

    private void update(String siteId, String root, List<String> fileList, boolean isDelete) throws IOException {
        for (String fileName : fileList) {
            String mimeType = null;
            File file = new File(root + fileName);
            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
            mimeType = mimeTypesMap.getContentType(fileName);
            if (supportedMimeTypes.contains(mimeType) && !isDelete) {
                searchService.updateDocument(siteId, fileName, file);
            } else if (isDelete) {
                searchService.delete(siteId, fileName);
            }
        }
    }

    @Override
    public String getName() {
        return SearchAttachmentProcessor.class.getSimpleName();
    }
}
