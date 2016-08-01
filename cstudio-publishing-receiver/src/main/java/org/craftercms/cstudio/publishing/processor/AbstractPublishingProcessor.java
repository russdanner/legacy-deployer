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
package org.craftercms.cstudio.publishing.processor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanNameAware;

/**
 * Base class for {@link PublishingProcessor}s.
 *
 * @author avasquez
 */
public abstract class AbstractPublishingProcessor implements PublishingProcessor, BeanNameAware {

    protected String name;
    protected int order;

    public AbstractPublishingProcessor() {
        order = Integer.MAX_VALUE;
    }

    @Override
    public String getName() {
        return StringUtils.isNotEmpty(name)? name : getClass().getSimpleName();
    }

    @Override
    public void setBeanName(String name) {
        this.name = name;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

}
