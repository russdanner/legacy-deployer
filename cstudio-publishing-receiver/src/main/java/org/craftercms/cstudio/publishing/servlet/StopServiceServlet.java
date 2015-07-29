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
package org.craftercms.cstudio.publishing.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * stops the current service running
 * 
 * @author hyanghee
 * 
 */
public class StopServiceServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log LOGGER = LogFactory.getLog(StopServiceServlet.class);
	
	public static final String PARAM_PASSWORD = "password";
	
	private String password;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		String password = request.getParameter(PARAM_PASSWORD);
		if (password != null && password.equalsIgnoreCase(this.password)) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Stopping the server.");
			}
			System.exit(0);
		} else {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Illegal stop service request receivced.");
			}
		}
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
}
