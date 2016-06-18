package org.craftercms.deployer.git.processor;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.deployer.git.config.SiteConfiguration;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link PublishingProcessor} that does an HTTP method call to a specified URL. Any occurrences of {siteName} are
 * replaced by the current site name, or the one specified as a property.
 *
 * @author avasquez
 */
public class HttpMethodCallPostProcessor extends AbstractPublishingProcessor {

    private static final Log logger = LogFactory.getLog(HttpMethodCallPostProcessor.class);

    protected Method method;
    protected String url;
    protected String siteName;

    public Method getMethod() {
        return method;
    }

    @Required
    public void setMethod(Method method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    @Required
    public void setUrl(String url) {
        this.url = url;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    @Override
    public void doProcess(SiteConfiguration siteConfiguration, PublishedChangeSet changeSet) throws PublishingException {
        String url = this.url;
        String siteId = StringUtils.isNotEmpty(siteName)? siteName: siteConfiguration.getSiteId();

        if (StringUtils.isNotEmpty(siteId)) {
            url = url.replaceAll(FileUploadServlet.CONFIG_MULTI_TENANCY_VARIABLE, siteId);
        }

        HttpMethod httpMethod = createHttpMethod(url);
        HttpClient client = new HttpClient();

        try {
            int status = client.executeMethod(httpMethod);
            String msg = method + " " + url + " response: status = " + status + ", body = " +
                         httpMethod.getResponseBodyAsString();

            if (status == HttpServletResponse.SC_OK) {
                logger.info(msg);
            } else {
                logger.error(msg);
            }
        } catch (IOException e) {
            throw new PublishingException(e);
        } finally {
            httpMethod.releaseConnection();
        }
    }

    protected HttpMethod createHttpMethod(String url) {
        switch (method) {
            case GET:
                return new GetMethod(url);
            case POST:
                return new PostMethod(url);
            case HEAD:
                return new HeadMethod(url);
            case OPTIONS:
                return new OptionsMethod(url);
            case PUT:
                return new PutMethod(url);
            case DELETE:
                return new DeleteMethod(url);
            default:
                return new TraceMethod(url);
        }
    }

    public enum Method {

        GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE;

    }

}
