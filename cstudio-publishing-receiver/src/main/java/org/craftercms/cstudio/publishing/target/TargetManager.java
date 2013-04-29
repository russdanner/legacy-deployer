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
package org.craftercms.cstudio.publishing.target;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * deployment target manager
 * 
 * @author hyanghee
 *
 */
public class TargetManager {
	
	private static Log LOGGER = LogFactory.getLog(TargetManager.class);

	private Map<String, PublishingTarget> targets = new HashMap<String, PublishingTarget>();
	
	public PublishingTarget getTarget(String key) {
		if (this.targets != null) {
			return this.targets.get(key);
		} else {
			return null;
		}
	}
	
	/**
	 * register a target 
	 * 
	 * @param key
	 * @param target
	 */
	public void register(String key, PublishingTarget target) {
		if (this.targets == null) {
			this.targets = new HashMap<String, PublishingTarget>();
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("registering target: " + key);
		}
		this.targets.put(key, target);
	}


	/**
	 * @return the targets
	 */
	public Map<String, PublishingTarget> getTargets() {
		return targets;
	}


	/**
	 * @param targets the targets to set
	 */
	public void setTargets(Map<String, PublishingTarget> targets) {
		this.targets = targets;
	}
	
}
