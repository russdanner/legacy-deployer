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

import java.util.List;

/**
 * published change set
 * 
 * @author hyanghee
 *
 */
public class PublishedChangeSet {

	List<String> createdFiles = null;
	List<String> updatedFiles = null;
	List<String> deletedFiles = null;

	/**
	 * default constructor
	 */
	public PublishedChangeSet() {}
	
	/**
	 * constructor to set lists of files
	 * 
	 * @param createdFiles
	 * @param updatedFiles
	 * @param deletedFiles
	 */
	public PublishedChangeSet(List<String> createdFiles, List<String> updatedFiles, List<String> deletedFiles) {
		this.createdFiles = createdFiles;
		this.updatedFiles = updatedFiles;
		this.deletedFiles = deletedFiles;
	}
	
	/**
	 * @return the createdFiles
	 */
	public List<String> getCreatedFiles() {
		return createdFiles;
	}

	/**
	 * @param createdFiles
	 *            the createdFiles to set
	 */
	public void setCreatedFiles(List<String> createdFiles) {
		this.createdFiles = createdFiles;
	}

	/**
	 * @return the updatedFiles
	 */
	public List<String> getUpdatedFiles() {
		return updatedFiles;
	}

	/**
	 * @param updatedFiles
	 *            the updatedFiles to set
	 */
	public void setUpdatedFiles(List<String> updatedFiles) {
		this.updatedFiles = updatedFiles;
	}

	/**
	 * @return the deletedFiles
	 */
	public List<String> getDeletedFiles() {
		return deletedFiles;
	}

	/**
	 * @param deletedFiles
	 *            the deletedFiles to set
	 */
	public void setDeletedFiles(List<String> deletedFiles) {
		this.deletedFiles = deletedFiles;
	}
}
