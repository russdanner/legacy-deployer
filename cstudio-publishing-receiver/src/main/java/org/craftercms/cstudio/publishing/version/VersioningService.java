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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.servlet.FileUploadServlet;
import org.craftercms.cstudio.publishing.target.PublishingTarget;
import org.craftercms.cstudio.publishing.target.TargetManager;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.locks.ReentrantLock;

public class VersioningService {

    protected static ReentrantLock fileLock = new ReentrantLock();

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
	
	public void writeNewVersion(String newVersion, String targetName) throws VersionException {
		PublishingTarget target = this.targetManager.getTarget(targetName);
		FileOutputStream fout=null;
		if (target == null) {
			LOGGER.error("Unable to get Target with name " + targetName);
			throw new VersionException("Unable to get Target with name " + targetName);
		}
        fileLock.lock();
		try {
			String path = target.getParameter(FileUploadServlet.CONFIG_ROOT);
			String finalName = path + File.separator + fileName;
			fout = new FileOutputStream(finalName);
			fout.write(newVersion.getBytes(ioCharset));
			fout.flush();
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
            fileLock.unlock();
		}
	}

	public String readVersion(String targetName) throws VersionException {
		PublishingTarget target = this.targetManager.getTarget(targetName);
		FileInputStream fin = null;
		ByteArrayOutputStream out = null;
		String readVersion = DEFAULT_VERSION;
		if (target == null) {
			LOGGER.error("Unable to get Target with name " + targetName);
			throw new VersionException("Unable to get Target with name " + targetName);
		}
        fileLock.lock();
		try {
			String path = target.getParameter(FileUploadServlet.CONFIG_ROOT);
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
				readVersion = new String(out.toByteArray(), charset);
				// Version can not be empty File must be corrupt
				if (StringUtils.isEmpty(readVersion)) {
					readVersion = DEFAULT_VERSION;
				}
			} else {
				LOGGER.debug("Version File " + finalName + " does not exist returning default value");
			}
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
            fileLock.unlock();
		}
		return readVersion;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setTargetManager(TargetManager targetManager) {
		this.targetManager = targetManager;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}	
}
