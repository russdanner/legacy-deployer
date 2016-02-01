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

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.eclipse.jetty.server.Server;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * TO Be used only with Apache Deamon !!
 * @author Carlos Ortiz
 */
public class PublishingReceiverMainDeamon  implements Daemon {
    Server server ;
    FileSystemXmlApplicationContext context;
    public static void main(String[] args) throws Exception {
    }

	private  Server initializeContext() throws IOException {
        System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");
    	 context =
    		new FileSystemXmlApplicationContext("classpath:spring/application-context.xml");
    	return (Server)context.getBean("Server");
    }

    @Override
    public void init(final DaemonContext daemonContext) throws DaemonInitException, Exception {
        server=initializeContext();
    }

    @Override
    public void start() throws Exception {
        server.start();
        server.join();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public void destroy() {
        context.destroy();
        server=null;
    }
}
