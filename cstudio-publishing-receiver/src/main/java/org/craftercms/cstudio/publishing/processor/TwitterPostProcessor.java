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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.cstudio.publishing.target.PublishingTarget;

import java.io.File;
import java.io.*;
import java.util.*;
import org.dom4j.*;
import org.dom4j.io.*;

import twitter4j.*;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.media.ImageUpload;
import twitter4j.media.ImageUploadFactory;

/**
 * post content to twitter
 * process publishes a specific content type to a specific authenticated twitter feed
 * content type name: /component/tweet
 * expected fields: tweetBody (text/text area) & image (path to image bits in static assets)
 *
 * Optonally: can publish on update of content (false by default)
 * Currently does nothing on delete
 *
 * @author rdanner
 */
public class TwitterPostProcessor extends AbstractPublishingProcessor {

	private static Log LOGGER = LogFactory.getLog(TwitterPostProcessor.class);
	
	/*
	 * (non-Javadoc)
	 * @see org.craftercms.cstudio.publishing.processor.PublishingProcessor#doProcess(org.craftercms.cstudio.publishing.PublishedChangeSet, java.util.Map, org.craftercms.cstudio.publishing.target.PublishingTarget)
	 */
	@Override
	public void doProcess(PublishedChangeSet changeSet, Map<String, String> parameters, PublishingTarget target) throws PublishingException {
		
		if (changeSet != null) {
			for (String path : changeSet.getCreatedFiles()) {

				try {
					if (path.endsWith(".xml")) {
						TwitterPost post = buildPostFromDescriptor(path);

						if(post != null) {
							postToTwitter(post);
						}
					}
				}
				catch(Throwable err) {
					LOGGER.error("Error posting to twitter ["+err+"]", err);
				}
			}

			if(publishOnUpdate) {
				for (String path : changeSet.getUpdatedFiles()) {

					try {
						if (path.endsWith(".xml")) {
							TwitterPost post = buildPostFromDescriptor(path);

							if (post != null) {
								postToTwitter(post);
							}
						}
					} catch (Throwable err) {
						LOGGER.error("Error posting to twitter [" + err + "]", err);
					}
				}
			}
		}
	}

	/**
	 * Given a predefined set of fields in a descriptor, build a twitter post object
	 * @param path
	 * @return
	 */
	protected TwitterPost buildPostFromDescriptor(String path)
	throws Exception {
		TwitterPost retPost = null;

		File descriptorFile = new File(rootPath + path);

		SAXReader saxReader = new SAXReader();
		Document document = saxReader.read(descriptorFile);

		Element rootElement = document.getRootElement();

		String type = rootElement.selectSingleNode("content-type").getText();

		if("/component/tweet".equals(type)) {
			retPost = new TwitterPost();
			String message = rootElement.selectSingleNode("tweetBody").getText();
			String imagePath = rootElement.selectSingleNode("image").getText();

			retPost.message = (message != null) ? message : "";
			retPost.image = (imagePath != null) ? new File(rootPath + imagePath) : null;
		}
		else {
			LOGGER.debug("Skipping type : " + type);
		}

		return retPost;
	}

	/**
	 * perform API call to post to twiiter
	 * @throws Exception
	 */
	protected void postToTwitter(TwitterPost post)
	throws Exception {

		String statusMessage = "";
		
		if(post.image != null) {
			ImageUpload uploader = getTwitterUploaderInstance();
			statusMessage = uploader.upload(post.image, post.message);
		}
		else {
			Twitter twitter = getTwitterInstance();
			Status status = twitter.updateStatus(post.message);
			statusMessage = status.getText();
		}

		LOGGER.info("Successfully updated the status to [" + statusMessage + "].");
	}

	/**
	 * factory method for twitter upload instance. Override if you want to authenticate differently
	 * @return twitter uploader instance
	 */
	protected ImageUpload getTwitterUploaderInstance() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setDebugEnabled(true)
				.setOAuthConsumerKey(consumerKeyStr)
				.setOAuthConsumerSecret(consumerSecretStr)
				.setOAuthAccessToken(accessTokenStr)
				.setOAuthAccessTokenSecret(accessTokenSecretStr);

		Configuration configuration = builder.build();
		OAuthAuthorization auth = new OAuthAuthorization(configuration);
		ImageUpload uploader = new ImageUploadFactory(configuration).getInstance(auth);

		return uploader;
	}

	/**
	 * factory method for twitter instance. Override if you want to authenticate differently
	 * @return twitter instance
	 */
	protected Twitter getTwitterInstance() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setDebugEnabled(true)
				.setOAuthConsumerKey(consumerKeyStr)
				.setOAuthConsumerSecret(consumerSecretStr)
				.setOAuthAccessToken(accessTokenStr)
				.setOAuthAccessTokenSecret(accessTokenSecretStr);

		Configuration configuration = builder.build();
		TwitterFactory factory = new TwitterFactory(configuration);
		Twitter twitter = factory.getInstance();

		return twitter;
	}

	protected class TwitterPost {
		public String message;
		public File image;
	}

	private String consumerKeyStr = "";
	private String consumerSecretStr = "";
	private String accessTokenStr = "";
	private String accessTokenSecretStr = "";
	private String rootPath = ".";
	private boolean publishOnUpdate = false;

	public String getConsumerKey() { return consumerKeyStr; }
	public void setConsumerKey(String key) { consumerKeyStr = key; }

	public String getConsumerSecret() { return consumerSecretStr; }
	public void setConsumerSecret(String key) { consumerSecretStr = key; }

	public String getAccessToken() { return accessTokenStr; }
	public void setAccessToken(String key) { accessTokenStr = key; }

	public String getAccessTokenSecret() { return accessTokenSecretStr; }
	public void setAccessTokenSecret(String key) { accessTokenSecretStr = key; }

	public String getRootPath() { return rootPath; }
	public void setRootPath(String path) { rootPath = path; }

	public boolean getPublishOnUpdate() { return publishOnUpdate; }
	public void setPublishOnUpdate(boolean flag) { publishOnUpdate = flag; }


}