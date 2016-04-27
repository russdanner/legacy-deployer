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
package org.craftercms.cstudio.publishing;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.StopServiceServlet;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * stop receiver service
 * 
 * @author hyanghee
 *
 */
public class StopServiceMain {
	
	private static Log LOGGER = LogFactory.getLog(StopServiceMain.class);
	
	private static String PROPERTIES_NAME = "cstudioShutdownProperties";
	private static String PROP_URL = "receiver.url";
	private static String PROP_SERVICE_PATH = "servlet.stopService.path";
	private static String PROP_PORT = "receiver.port";
	private static String PROP_PASSWORD = "receiver.password";
	
    public static void main(String[] args) throws Exception {
    	FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext("classpath:spring/shutdown-context.xml");

    	ReadablePropertyPlaceholderConfigurer properties = (ReadablePropertyPlaceholderConfigurer) context.getBean("cstudioShutdownProperties");
    	if (properties != null) {
	    	String url = getProperty(properties, PROP_URL);
	    	String path = getProperty(properties, PROP_SERVICE_PATH);
	    	String port = getProperty(properties, PROP_PORT);
	    	String password = URLEncoder.encode(getProperty(properties, PROP_PASSWORD), "UTF-8");
	    	String target = url + ":" + port + path;
	    	if (LOGGER.isDebugEnabled()) {
	    		LOGGER.debug("Sending a stop request to " + target);
	    	}
	    	target = target + "?" + StopServiceServlet.PARAM_PASSWORD + "=" + password;
	    	URL serviceUrl = new URL(target);
	    	HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection();
	        connection.setRequestMethod("GET");
	        connection.setReadTimeout(10000);
	        connection.connect();
	        try {
	        	connection.getContent();
	        } catch (ConnectException e) {
	        	// ignore this error (server will terminate as soon as the request is sent out)
	        }
    	} else {
        	if (LOGGER.isErrorEnabled()) {
        		LOGGER.error(PROPERTIES_NAME + " is not present in shutdown-context.xml");
        	}
    	}
        context.close();
    }

    /**
     * read a property
     * 
     * @param properties
     * @param name
     * @return
     * @throws IOException
     * @throws PublishingException
     */
	private static String getProperty(ReadablePropertyPlaceholderConfigurer properties, String name) throws IOException, PublishingException {
		String value = properties.getProperty(name);
		if (value == null) {
			throw new PublishingException(name + " is not found in properties.");
		} else {
			return value;
		}
	}
}
