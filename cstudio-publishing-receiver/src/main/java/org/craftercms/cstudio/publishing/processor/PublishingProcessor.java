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

import java.util.Map;

import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.target.PublishingTarget;

/**
 * processor that runs before or after publishing
 * 
 * @author hyanghee
 *
 */
@Deprecated
public interface PublishingProcessor {

    /**
     * backward compatibility with old preview target configuration
     */
    final static String SITE_NAME_PREVIEW = "preview";

	/**
	 * process files published
	 * 
	 * @param changeSet
	 * @param parameters 
	 * @param target
	 * @throws PublishingException
	 */
	public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters, PublishingTarget target) throws PublishingException;
	
	/**
	 * get the process's name
	 * 
	 * @return
	 */
	public String getName();

    /**
     * get the order value for ordering in the list of processors.
     *
     * @return order value
     */
    int getOrder();
}
