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
package org.craftercms.deployer.git.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.craftercms.deployer.git.config.SiteConfiguration;

import java.util.Map;

/**
 * a sample post processor that logs a list of files published 
 * 
 * @author hyanghee
 *
 */
public class SamplePostProcessor implements PublishingProcessor {

	private static Log LOGGER = LogFactory.getLog(SamplePostProcessor.class);
    /*
         * (non-Javadoc)
         * @see org.craftercms.cstudio.publishing.processor.PublishingProcessor#doProcess(org.craftercms.cstudio.publishing.PublishedChangeSet, java.util.Map, org.craftercms.cstudio.publishing.target.PublishingTarget)
         */
	@Override
    public void doProcess(SiteConfiguration siteConfiguration, PublishedChangeSet changeSet) throws PublishingException {
		if (LOGGER.isInfoEnabled()) {
			if (changeSet != null) {
				LOGGER.info("Printing the list of files deployed.");
				if (changeSet.getCreatedFiles() != null) {
					for (String file : changeSet.getCreatedFiles()) {
						LOGGER.info("[CREATED] " + file);
					}
				}
				if (changeSet.getUpdatedFiles() != null) {
					for (String file : changeSet.getUpdatedFiles()) {
						LOGGER.info("[UPATED] " + file);
					}
				}
				if (changeSet.getDeletedFiles() != null) {
					for (String file : changeSet.getDeletedFiles()) {
						LOGGER.info("[DELETED] " + file);
					}
				}
				LOGGER.info("end of the list of files deployed.");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.craftercms.cstudio.publishing.processor.PublishingProcessor#getName()
	 */
	@Override
	public String getName() {
		return "SamplePostProcessor";
	}

    protected int order = Integer.MAX_VALUE;

    @Override
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
