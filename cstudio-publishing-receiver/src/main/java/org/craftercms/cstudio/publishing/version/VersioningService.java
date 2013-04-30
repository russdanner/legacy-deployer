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
package org.craftercms.cstudio.publishing.version;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.craftercms.cstudio.publishing.target.TargetManager;

public class VersioningService {
	
	private static final String DEFAULT_VERSION = "0";
	private static final int BUFFER_SIZE = 1024;
	private static Log LOGGER = LogFactory.getLog(VersioningService.class);
	protected String fileName;
	protected TargetManager targetManager;
	protected String charset;

	private Charset ioCharset;

	public void init() {
		ioCharset = Charset.forName(charset);
	}
	
	public void writeNewVersion(String newVersion, String targetName, String site) throws VersionException {
		PublishingTarget target = this.targetManager.getTarget(targetName);
		FileOutputStream fout=null;
		if (target == null) {
			LOGGER.error("Unable to get Target with name " + targetName);
			throw new VersionException("Unable to get Target with name " + targetName);
		}
		try {
			String path = buildContentPath(target, site);
			String finalName = path + File.separator + fileName;
			fout = new FileOutputStream(finalName);
			fout.write(newVersion.getBytes(ioCharset));
			fout.flush();
		} catch (PublishingException ex) {
			LOGGER.error("Unable to calculate Target (" + targetName + ") root dir", ex);
			throw new VersionException("Unable to calculate Target (" + targetName + ") root dir");
		} catch (IOException ioEx) {
			LOGGER.error("Unable to read or write file " + fileName, ioEx);
			throw new VersionException("Unable to read/write File " + fileName);
		} finally {
			try {
				if (fout != null)
					fout.close();
			} catch (IOException ex) {
				LOGGER.error("Unable to IO resources",ex);
			}
		}
	}

	public String readVersion(String targetName, String site) throws VersionException {
		PublishingTarget target = this.targetManager.getTarget(targetName);
		FileInputStream fin = null;
		ByteArrayOutputStream out = null;
		String readedVersion = DEFAULT_VERSION;
		if (target == null) {
			LOGGER.error("Unable to get Target with name " + targetName);
			throw new VersionException("Unable to get Target with name " + targetName);
		}
		try {
			String path = buildContentPath(target, site);
			String finalName = path + File.separator + fileName;
			File f = new File(finalName);
			if (f.exists()) {
				LOGGER.debug("About to read " + finalName);
				fin = new FileInputStream(f);
				out = new ByteArrayOutputStream();
				byte[] buff = new byte[BUFFER_SIZE];
				while (fin.read(buff) >= 0) {
					out.write(buff);
				}
				readedVersion = new String(out.toByteArray(), charset);
				// Version can not be empty File must be corrupt
				if (StringUtils.isEmpty(readedVersion)) {
					readedVersion = DEFAULT_VERSION;
				}
			} else {
				LOGGER.debug("Version File " + finalName + " does not exist returning default value");
			}
		} catch (PublishingException ex) {
			LOGGER.error("Unable to calculate Target (" + targetName + ") root dir", ex);
			throw new VersionException("Unable to calculate Target (" + targetName + ") root dir");
		} catch (IOException ioEx) {
			LOGGER.error("Unable to read or write file " + fileName, ioEx);
			throw new VersionException("Unable to read/write File " + fileName);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				if (fin != null) {
					fin.close();
				}
			} catch (IOException ex) {
				LOGGER.error("Unable to IO resources", ex);
			}
		}
		return readedVersion;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setTargetManager(TargetManager targetManager) {
		this.targetManager = targetManager;
	}

	/**
	 * Build Default Path from targets parameters
	 * 
	 * @param target
	 *            Target to base build of the root
	 * @return Relative to working directory path of where the content should be
	 * @throws PublishingException
	 *             If Working dir can't be calculated
	 */
	protected String buildContentPath(PublishingTarget target, String site) throws PublishingException {
		LOGGER.debug("Building root Path");
		File f = new File(target.getParameter(FileUploadServlet.CONFIG_ROOT));
		String path = "";
		// Exists, is a dir and have rw permitions
		if (f.isAbsolute()) {
			path = target.getParameter(FileUploadServlet.CONFIG_ROOT) + File.separator + site;
		} else {
			File directory = new File(".");
			path = directory.getAbsolutePath().subSequence(0, directory.getAbsolutePath().length() - 2) + File.separator
					+ target.getParameter(FileUploadServlet.CONFIG_ROOT) + File.separator + site;
		}
		File checkFolder = new File(path);
		if (!checkFolder.exists()) {
			LOGGER.debug("Creating path " + path);
			checkFolder.mkdirs();
		}
		return path;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}	
}
