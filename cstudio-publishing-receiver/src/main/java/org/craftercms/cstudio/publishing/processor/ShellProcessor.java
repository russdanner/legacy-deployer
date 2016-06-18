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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;

public class ShellProcessor extends AbstractPublishingProcessor {

    private static final String INCLUDE_FILTER_PARAM = "includeFilter";
    private static final String INCLUDE_FILES_ARG = "files";
    private static Log LOGGER = LogFactory.getLog(ShellProcessor.class);
    private String workingDir;
    private String command;
    private Map<String, String> arguments;
    private Map<String, String> enviroment;
    private String sourceFiles;
    private boolean asSingleCommand;

    @Override
    public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters,
                          PublishingTarget target) throws PublishingException {
        checkConfiguration(parameters, target);
        LOGGER.debug("Starting Shell Processor");
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(getWorkingDir(workingDir, parameters.get(FileUploadServlet.PARAM_SITE)));
        LOGGER.debug("Working directory is " + workingDir);
        HashMap<String, String> argumentsMap = buildArgumentsMap(getFileList(parameters, changeSet));
        if (asSingleCommand) {
            StrSubstitutor substitutor = new StrSubstitutor(argumentsMap, "%{", "}");
            String execComand = substitutor.replace(command);
            LOGGER.debug("Command to be Executed is " + execComand);
            builder.command("/bin/bash", "-c", execComand);

        } else {
            Set<String> keys = argumentsMap.keySet();
            ArrayList<String> commandAsList = new ArrayList<String>();
            commandAsList.add(command.trim());
            for (String key : keys) {
                if (!key.equalsIgnoreCase(INCLUDE_FILTER_PARAM)) {
                    commandAsList.add(argumentsMap.get(key));
                }
            }
            LOGGER.debug("Command to be Executed is " + StringUtils.join(commandAsList, " "));
            builder.command(commandAsList);
        }

        builder.environment().putAll(enviroment);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String str;
            while ((str = reader.readLine()) != null) {
                LOGGER.info("PROCESS OUTPUT :" + str);
            }
            reader.close();
            LOGGER.info("Process Finish with Exit Code " + process.exitValue());
            LOGGER.debug("Process Output ");
        } catch (IOException ex) {
            LOGGER.error("Error ", ex);
        } catch (InterruptedException e) {
            LOGGER.error("Error ", e);
        } finally {
            LOGGER.debug("End of Shell Processor");
        }
    }

    private File getWorkingDir(final String workingDir, final String siteId) {
        String actualWorkingDir = workingDir;
        if (actualWorkingDir.contains("{siteId}")) {
            actualWorkingDir = actualWorkingDir.replaceAll(FileUploadServlet.CONFIG_MULTI_TENANCY_VARIABLE, siteId);
        }
        return new File(actualWorkingDir);
    }

    /**
     * Generates a unmodifiableList list with the merge of the changeset files<br>
     * If {@link FILES_SOURCE} :
     * <ul>
     * <li>Null will merge delete, create and updated files</li>
     * <li>If "UPDATED" only updated files</li>
     * <li>If "NEW" only created files</li>
     * <li>If "EXISTENT" created and updated files</li>
     * <li>If "DELETED" only delete files</li>
     * </ul>
     *
     * @param parameters Map to get {@link FILES_SOURCE}
     * @param changeSet  Changeset where the deleted,created and updated files are.
     * @return A unmodifiableList depending on {@link FILES_SOURCE} <br/>
     * <b> Empty</b> if {@link FILES_SOURCE} is no valid
     */
    @SuppressWarnings("unchecked")
    private List<String> getFileList(Map<String, String> parameters, PublishedChangeSet changeSet) {
        List<String> result = new ArrayList<String>();
        if (sourceFiles == null) {
            result = mergeList(changeSet.getCreatedFiles(), changeSet.getDeletedFiles(), changeSet.getUpdatedFiles());

        } else if (sourceFiles.equalsIgnoreCase("UPDATED")) {
            result = mergeList(changeSet.getUpdatedFiles());

        } else if (sourceFiles.equalsIgnoreCase("NEW")) {
            result = mergeList(changeSet.getCreatedFiles());
        } else if (sourceFiles.equalsIgnoreCase("EXISTENT")) {
            result = mergeList(changeSet.getCreatedFiles(), changeSet.getUpdatedFiles());
        } else if (sourceFiles.equalsIgnoreCase("DELETED")) {
            result = mergeList(changeSet.getDeletedFiles());
        } else {
            LOGGER.debug("Parameter " + sourceFiles + " has a non valid value, valid values (UPDATED,NEW,EXISTENT," +
                "DELETE)");
            LOGGER.debug("Due non valid " + sourceFiles + " value returning empty list");
        }
        for (int i = 0; i < result.size(); i++) {
            result.set(i, result.get(i).substring(1));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Checks that all configuration is ok and set defaults if necessary
     *
     * @param parameters Parameters of the Processors
     * @param target     Target in which the processor is running
     * @throws PublishingException If some of the given configuration is wrong
     */
    private void checkConfiguration(Map<String, String> parameters, PublishingTarget target) throws
        PublishingException {
        if (workingDir == null) {
            // Set a default one
            workingDir = buildContentPath(target);
            LOGGER.debug("working Dir is not set, using " + workingDir + " as default");
        } else {
            if (!getWorkingDir(workingDir, parameters.get(FileUploadServlet.PARAM_SITE)).exists()) {
                LOGGER.error("The path " + workingDir + " does not exist");
                throw new PublishingException("The path " + workingDir + " does not exist");
            }
        }
        if (command == null) {
            LOGGER.error("Command can't be Null");
            throw new PublishingException("Command can't be Null");
        }
        if (enviroment == null) {
            LOGGER.debug("There are non Enviroment Variables Overrides");
            enviroment = new HashMap<String, String>();
        } else {
            LOGGER.debug("Enviroment Overrides are " + enviroment.toString());
        }
        if (sourceFiles == null) {
            LOGGER.debug("Using all files to apply " + INCLUDE_FILTER_PARAM);
        }
    }

    /**
     * Build Default Path from targets parameters
     *
     * @param target Target to base build of the root
     * @return Relative to working directory path of where the content should be
     * @throws PublishingException If Working dir can't be calculated
     */
    protected String buildContentPath(PublishingTarget target) throws PublishingException {
        LOGGER.debug("Building root Path");
        File directory = new File(".");
        String path = "";
        if (new File(target.getParameter(FileUploadServlet.CONFIG_ROOT)).exists()) {
            path = target.getParameter(FileUploadServlet.CONFIG_ROOT) + File.separator + target.getParameter
                (FileUploadServlet.CONFIG_CONTENT_FOLDER);
        } else {
            path = directory.getAbsolutePath().subSequence(0, directory.getAbsolutePath().length() - 2) + File
                .separator + target.getParameter(FileUploadServlet.CONFIG_ROOT) + File.separator + target
                .getParameter(FileUploadServlet.CONFIG_CONTENT_FOLDER);
        }
        LOGGER.debug("Build path is " + path);
        return path;
    }

    protected HashMap<String, String> buildArgumentsMap(List<String> files) {
        HashMap<String, String> result = new HashMap<String, String>();
        if (arguments != null) {
            result.putAll(arguments);
        }
        String filter = result.get(INCLUDE_FILTER_PARAM);
        if (filter != null) {
            List<String> filterFiles = new ArrayList<String>();
            Pattern patter = Pattern.compile(filter);
            for (String file : files) {
                if (patter.matcher(file).matches()) {
                    LOGGER.debug("File " + file + " Match " + filter);
                    filterFiles.add(file);
                } else {
                    LOGGER.debug("File Don't " + file + " don't match " + filter);
                }
            }
            result.put(INCLUDE_FILES_ARG, StringUtils.join(filterFiles, " "));
        }
        return result;
    }

    public String getWorkingDir() { return workingDir; }
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public String getCommand() { return command; }
    public void setCommand(String command) {
        this.command = command;
    }

    public Map<String, String> getArguments() { return arguments; }
    public void setArguments(Map<String, String> arguments) {
        this.arguments = arguments;
    }

    public Map<String, String> getEnviroment() { return enviroment; }
    public void setEnviroment(Map<String, String> enviroment) {
        this.enviroment = enviroment;
    }

    public String getSourceFiles() { return sourceFiles; }
    public void setSourceFiles(String sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public boolean isAsSingleCommand() { return asSingleCommand; }
    public void setAsSingleCommand(boolean asSingleCommand) {
        this.asSingleCommand = asSingleCommand;
    }


    private <T> List<T> mergeList(List<T>... mergeWith) {

        ArrayList<T> mergeTo = new ArrayList<T>();

        for (List<T> toMerge : mergeWith) {
            if (toMerge != null) {
                mergeTo.addAll(toMerge);
            }
        }
        return mergeTo;
    }

}
