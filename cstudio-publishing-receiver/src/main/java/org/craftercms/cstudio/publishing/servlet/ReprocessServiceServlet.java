package org.craftercms.cstudio.publishing.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.processor.PublishingProcessor;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.craftercms.cstudio.publishing.target.TargetManager;
import org.springframework.util.MimeTypeUtils;

/**
 * <p>Reindex the target site assuming that target index is clean</p>
 */
public class ReprocessServiceServlet extends HttpServlet {

    private static final Log LOGGER = LogFactory.getLog(ReprocessServiceServlet.class);

    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_PROCESSOR = "processor";
    public static final String PARAM_TARGET = "target";

    /**
     * <p>Deployer target manager</p>
     */
    protected TargetManager targetManager;

    /**
     * <p>Deployer password</p>
     */
    private String password;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, String> parameters = this.getParameters(request);
        String password = parameters.get(PARAM_PASSWORD);
        PrintWriter responseWriter = response.getWriter();

        response.setContentType(MimeTypeUtils.TEXT_PLAIN_VALUE);

        String paramTarget = parameters.get(PARAM_TARGET);
        // authenticate the request
        if (password != null && password.equalsIgnoreCase(this.password)) {
            // find the target
            PublishingTarget target = this.targetManager.getTarget(paramTarget);
            if (target != null) {
                // find the processor
                PublishingProcessor targetProcessor = null;
                String processorName = parameters.get(PARAM_PROCESSOR);
                if (!StringUtils.isEmpty(processorName)) {
                    List<PublishingProcessor> processors = target.getPostProcessors();
                    for (PublishingProcessor processor : processors) {
                        if (processorName.equalsIgnoreCase(processor.getName())) {
                            targetProcessor = processor;
                            break;
                        }
                    }
                }
                if (targetProcessor != null) {
                    // if found, reprocess the target
                    int status = reprocess(target, parameters, targetProcessor);

                    response.setStatus(status);
                    if (status == HttpServletResponse.SC_OK) {
                        responseWriter.println("Reprocess finished successfully");

                        LOGGER.info("Reprocess finished successfully");
                    } else {
                        responseWriter.println("Reprocess failed");

                        LOGGER.error("Reprocess failed");
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    responseWriter.println("No processor found by name: " + processorName);

                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("No processor found by name: " + processorName);
                    }
                }
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                responseWriter.println("No configuration exists for " + paramTarget);

                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("No configuration exists for " + paramTarget);
                }
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            responseWriter.println("Illegal publish request received. Password parameter does not match configured password for deployer.");

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Illegal publish request received. Password parameter does not match configured password for deployer.");
            }
        }

        responseWriter.flush();
    }

    /**
     * <p>Reprocess the site content by providing the list as created files</p>
     *  @param target
     *          the target site
     * @param parameters
     *          a list of parameters
     * @param processor
     *          a processor to reprocess the site content
     * @return response status
     */
    private int reprocess(PublishingTarget target, Map<String, String> parameters, PublishingProcessor processor) {
        PublishedChangeSet changeSet = new PublishedChangeSet();
        StringBuilder sbFullPath = new StringBuilder(target.getParameter(FileUploadServlet.CONFIG_ROOT));
        sbFullPath.append(File.separator);
        sbFullPath.append(target.getParameter(FileUploadServlet.CONFIG_CONTENT_FOLDER));
        String fileRoot = sbFullPath.toString();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Root location: " + sbFullPath.toString());
        }
        List<String> createdFiles = new ArrayList<String>();
        this.addToList(fileRoot, "", createdFiles);
        changeSet.setCreatedFiles(createdFiles);
        try {
            processor.doProcess(changeSet, parameters, target);
            return HttpServletResponse.SC_OK;
        } catch (PublishingException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to reprocess target: " + target.getName(), e);

            }
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * get a parameter map from the request
     *
     * @param request
     *          the request to get parameters from
     * @return a map of parameters
     */
    private Map<String, String> getParameters(HttpServletRequest request) {
        Enumeration paramNames = request.getParameterNames();
        Map<String, String> parameters = new HashMap<String, String>();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            parameters.put(paramName, paramValue);
        }
        return parameters;
    }

    /**
     * add the current path to the change list
     *
     * @param fullPath
     *          the current path
     * @param createdFiles
     *          a list to add the current path
     */
    private void addToList(String fullPath, String sitePath, List<String> createdFiles) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Processing " + fullPath);
        }
        URL resourceUrl = getResourceUrl(fullPath);
        if (resourceUrl != null) {
            String resourcePath = resourceUrl.getFile();
            File file = new File(resourcePath);
            if (file.isDirectory()) {
                String[] children = file.list();
                if (children != null && children.length > 0) {
                    for (String childName : children) {
                        addToList(fullPath + "/" + childName, sitePath + "/" + childName, createdFiles);
                    }
                }
            } else {
                createdFiles.add(sitePath);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Added " + sitePath);
                }
            }
        }
    }

    /**
     * get the resource url for import
     *
     * @param filePath
     *          a resource path
     * @return URL
     */
    private URL getResourceUrl(String filePath) {
        try {
            return new File(filePath).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Not able to find " + filePath);
        }
    }

    /**
     * <p>Set the deployer password </p>
     * @param password
     *          a password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * <p>Set publishing target manager</p>
     * @param targetManager
     *          a target manager to set
     */
    public void setTargetManager(TargetManager targetManager) { this.targetManager = targetManager; }

}
