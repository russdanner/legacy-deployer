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
import java.util.List;
import javax.activation.FileTypeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;

/**
 * {@link org.craftercms.deployer.git.processor.PublishingProcessor} that updates/deletes binary or structured
 * document files (PDF, Word, etc.) from a search index, only if their mime types match the supported mime types
 * or if the supported mime types map is empty.
 *
 * @author avasquez
 */
public class BinaryFileIndexingProcessor extends AbstractIndexingProcessor {

    private static final Log logger = LogFactory.getLog(BinaryFileIndexingProcessor.class);

    protected List<String> supportedMimeTypes;

    public void setSupportedMimeTypes(List<String> supportedMimeTypes) {
        this.supportedMimeTypes = supportedMimeTypes;
    }

    @Override
    protected int update(String indexId, String siteName, String root, List<String> fileNames,
                         boolean delete) throws PublishingException {
        FileTypeMap mimeTypesMap = new ConfigurableMimeFileTypeMap();
        int updateCount = 0;

        for (String fileName : fileNames) {
            File file = new File(root, fileName);
            String mimeType = mimeTypesMap.getContentType(fileName);
            boolean doUpdate = false;

            if (CollectionUtils.isNotEmpty(supportedMimeTypes)) {
                if (supportedMimeTypes.contains(mimeType)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("File " + file + " with mime-type '" + mimeType + "' will be indexed...");
                    }

                    doUpdate = true;
                }
            } else {
                doUpdate = true;
            }

            if (doUpdate) {
                if (delete) {
                    doDelete(indexId, siteName, fileName, file);
                    updateCount++;
                } else {
                    doUpdateFile(indexId, siteName, fileName, file);
                    updateCount++;
                }
            }
        }

        return updateCount;
    }

}
