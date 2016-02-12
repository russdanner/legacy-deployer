package org.craftercms.deployer.git.config;

import org.craftercms.deployer.git.processor.PublishingProcessor;

import java.util.List;

public class SiteConfiguration {

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }

    public String getLocalRepositoryRoot() { return localRepositoryRoot; }
    public void setLocalRepositoryRoot(String localRepositoryRoot) { this.localRepositoryRoot = localRepositoryRoot; }

    public String getGitRepositoryUrl() { return gitRepositoryUrl; }
    public void setGitRepositoryUrl(String gitRepositoryUrl) { this.gitRepositoryUrl = gitRepositoryUrl; }

    public String getLiveSiteUrl() { return liveSiteUrl; }
    public void setLiveSiteUrl(String liveSiteUrl) { this.liveSiteUrl = liveSiteUrl; }

    public String getSearchServiceUrl() { return searchServiceUrl; }
    public void setSearchServiceUrl(String searchServiceUrl) { this.searchServiceUrl = searchServiceUrl; }

    public List<PublishingProcessor> getPostProcessors() { return postProcessors; }
    public void setPostProcessors(List<PublishingProcessor> postProcessors) { this.postProcessors = postProcessors; }

    private String name;
    private String siteId;
    private String localRepositoryRoot;
    private String gitRepositoryUrl;
    private String liveSiteUrl;
    private String searchServiceUrl;
    private List<PublishingProcessor> postProcessors;
}
