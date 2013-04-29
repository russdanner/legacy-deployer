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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.target.PublishingTarget;

import java.io.IOException;
import java.util.Map;

/**
 * Post processor that invalidates crafter cache
 *
 * @author Dejan Brkic
 */
public class CacheInvalidatePostProcessor implements PublishingProcessor {

    private static final Log log = LogFactory.getLog(CacheInvalidatePostProcessor.class);

    protected String _cacheInvalidateUrl;
    public void setCacheInvalidateUrl(String cacheInvalidateUrl) {
        this._cacheInvalidateUrl = cacheInvalidateUrl;
    }

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters, PublishingTarget target) throws PublishingException {
        HttpMethod cacheInvalidateGetMethod = new GetMethod(_cacheInvalidateUrl);
        HttpClient client = new HttpClient();
        try {
            client.executeMethod(cacheInvalidateGetMethod);
        } catch (IOException e) {
            throw new PublishingException(e);
        } finally {
            cacheInvalidateGetMethod.releaseConnection();
            cacheInvalidateGetMethod = null;
            client = null;
        }
    }

    @Override
    public String getName() {
        return CacheInvalidatePostProcessor.class.getName();
    }
}
