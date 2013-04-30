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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.craftercms.cstudio.publishing.version.VersionException;
import org.craftercms.cstudio.publishing.version.VersioningService;

public class DeployVersionServlet extends HttpServlet {

	private VersioningService versioningService;
	/**
	 * 
	 */
	private static final long serialVersionUID = 4952629503662163667L;
	public static final String PARAM_NEW_VERSION = "version";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter serverOut = response.getWriter();
		try {
			String target = request.getParameter(FileUploadServlet.PARAM_TARGET);
			String site = request.getParameter(FileUploadServlet.PARAM_SITE);
			if (StringUtils.isEmpty(target)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				serverOut.write("Parameter \"" + FileUploadServlet.PARAM_TARGET + "\" is need");
			} else if (StringUtils.isEmpty(site)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				serverOut.write("Parameter \"" + FileUploadServlet.PARAM_SITE + "\" is need");
			} else {
				String currerntVersion = versioningService.readVersion(target,site);
				response.setStatus(HttpServletResponse.SC_OK);
				serverOut.write(currerntVersion);
			}
		} catch (VersionException ex) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			serverOut.write(ex.getMessage());
		}
		setDefaultHeaders(response);
		serverOut.flush();
		response.flushBuffer();
	}

	/**
	 * Prevents Caching
	 * 
	 * @param response
	 */
	private void setDefaultHeaders(HttpServletResponse response) {
		// Set to expire far in the past.
		response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
		// Set standard HTTP/1.1 no-cache headers.
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
		response.addHeader("Cache-Control", "post-check=0, pre-check=0");
		// Set standard HTTP/1.0 no-cache header.
		response.setHeader("Pragma", "no-cache");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter serverOut = response.getWriter();
		try {
			String target = request.getParameter(FileUploadServlet.PARAM_TARGET);
			String site = request.getParameter(FileUploadServlet.PARAM_SITE);
			String version = request.getParameter(PARAM_NEW_VERSION);
			if (StringUtils.isEmpty(target)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				serverOut.write("Parameter \"" + FileUploadServlet.PARAM_TARGET + "\" is need and can not be empty");
			} else if (StringUtils.isEmpty(version)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				serverOut.write("Parameter \"" + PARAM_NEW_VERSION + "\" is need and can not be empty");
			} else if (StringUtils.isEmpty(site)) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				serverOut.write("Parameter \"" + PARAM_NEW_VERSION + "\" is need and can not be empty");
		}else {
				versioningService.writeNewVersion(version, target, site);
				response.setStatus(HttpServletResponse.SC_OK);
				serverOut.write(version);
			}
		} catch (VersionException ex) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			serverOut.write(ex.getMessage());
		}
		setDefaultHeaders(response);
		serverOut.flush();
		response.flushBuffer();
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	public void setVersioningService(VersioningService versioningService) {
		this.versioningService = versioningService;
	}
}
