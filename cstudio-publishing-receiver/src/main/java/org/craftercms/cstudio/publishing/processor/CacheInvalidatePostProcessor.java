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

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.target.PublishingTarget;

/**
 * Post processor that invalidates crafter cache
 *
 * @author Dejan Brkic
 */
@Deprecated
public class CacheInvalidatePostProcessor implements PublishingProcessor {

    private static final Log logger = LogFactory.getLog(CacheInvalidatePostProcessor.class);

    protected String cacheInvalidateUrl;
    protected int order;

    public String getCacheInvalidateUrl() {
        return cacheInvalidateUrl;
    }

    public void setCacheInvalidateUrl(String cacheInvalidateUrl) {
        this.cacheInvalidateUrl = cacheInvalidateUrl;
    }

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters,
                          PublishingTarget target) throws PublishingException {
        HttpMethod cacheInvalidateGetMethod = new GetMethod(cacheInvalidateUrl);
        HttpClient client = new HttpClient();
        try {
            int status = client.executeMethod(cacheInvalidateGetMethod);
            if (status != HttpServletResponse.SC_OK) {
                throw new PublishingException("Unable to invalidate cache: URL " + cacheInvalidateUrl +
                                              " returned status '" + cacheInvalidateGetMethod.getStatusText() +
                                              "' with body \n" + cacheInvalidateGetMethod.getResponseBodyAsString());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cache invalidated: URL " + cacheInvalidateUrl + " returned status '" +
                                 cacheInvalidateGetMethod.getStatusText() + "'");
                }
            }
        } catch (IOException e) {
            throw new PublishingException(e);
        } finally {
            cacheInvalidateGetMethod.releaseConnection();
        }
    }

    @Override
    public String getName() {
        return CacheInvalidatePostProcessor.class.getName();
    }

    @Override
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
