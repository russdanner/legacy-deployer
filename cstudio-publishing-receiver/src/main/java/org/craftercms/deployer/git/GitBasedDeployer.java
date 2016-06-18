package org.craftercms.deployer.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.cstudio.publishing.PublishedChangeSet;
import org.craftercms.cstudio.publishing.exception.PublishingException;
import org.craftercms.deployer.git.config.SiteConfiguration;
import org.craftercms.deployer.git.config.SiteConfigurationLoader;
import org.craftercms.deployer.git.processor.PublishingProcessor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitBasedDeployer {

    private final static Logger logger = LoggerFactory.getLogger(GitBasedDeployer.class);

    protected static final ReentrantLock singleWorkerLock = new ReentrantLock();

    public void execute() {
        if (enabled) {
            if (singleWorkerLock.tryLock()) {
                try {
                    logger.debug("Loading configuration");
                    List<String> sites = siteConfigurationLoader.getSitesList();
                    for (String site : sites) {
                        SiteConfiguration siteConfiguration = siteConfigurationLoader.loadSiteConfiguration(site);
                        checkDeploymentUpdatesForSite(siteConfiguration);
                    }
                } catch (Throwable err) {
                    logger.error("unable to execute git deployer job", err);
                } finally {
                    singleWorkerLock.unlock();
                }
            }
        }
    }

    private void checkDeploymentUpdatesForSite(SiteConfiguration siteConfiguration) {
        try {
            Repository repository = getSiteRepository(siteConfiguration);
            ObjectId head = repository.resolve(Constants.HEAD);
            PullResult pullResult = pullFromRemoteRepo(repository);
            if (pullResult != null && pullResult.isSuccessful()) {
                MergeResult mergeResult = pullResult.getMergeResult();
                switch (mergeResult.getMergeStatus()) {
                    case FAST_FORWARD:
                        ObjectId newHead = mergeResult.getNewHead();
                        PublishedChangeSet publishedChangeSet = processPull(repository, head, newHead);
                        doPostProcessing(siteConfiguration, publishedChangeSet);
                        break;

                    case ALREADY_UP_TO_DATE:
                        // already up to date
                        break;

                    default:
                        // Not supported merge result
                        logger.error("Received unsupported merge result after executing pull command \nMerge Result: " + mergeResult.getMergeStatus().name());
                }
            }
        } catch (IOException exc) {
            logger.error("Error while checking for deployment updates for site " + siteConfiguration.getName(), exc);
        } catch (GitAPIException exc) {
            logger.error("Error while opening git repository for site " + siteConfiguration.getName(), exc);
        }
    }

    private Repository getSiteRepository(SiteConfiguration siteConfiguration) throws IOException, GitAPIException {
        Path siteRepoPath = Paths.get(siteConfiguration.getLocalRepositoryRoot());
        Path siteLocalGitRepoPath = Paths.get(siteConfiguration.getLocalRepositoryRoot(), ".git");
        if (Files.exists(siteRepoPath) && Files.exists(siteLocalGitRepoPath)) {
            return openRepository(siteLocalGitRepoPath.normalize().toAbsolutePath().toString());
        } else {
            Files.deleteIfExists(siteRepoPath);
            return cloneRemoteRepository(siteConfiguration.getGitRepositoryUrl(), siteConfiguration.getLocalRepositoryRoot());
        }
    }

    private Repository openRepository(String localGitPath) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder
                .setGitDir(new File(localGitPath))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
        return repository;
    }

    private Repository cloneRemoteRepository(String remoteGitRepositoryUrl, String localRepositoryPath) throws GitAPIException {
        logger.debug("Cloning from " + remoteGitRepositoryUrl + " to " + localRepositoryPath);
        Path localGitRepositoryPath = Paths.get(localRepositoryPath);
        try (Git result = Git.cloneRepository()
                .setURI(remoteGitRepositoryUrl)
                .setDirectory(localGitRepositoryPath.toFile())
                .call()) {
            // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
            logger.debug("Having repository: " + result.getRepository().getDirectory());

            // workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=474093
            return result.getRepository();
        }
    }

    private PullResult pullFromRemoteRepo(Repository repository) {
        PullResult call = null;
        try (Git git = new Git(repository)) {
             call = git.pull().call();

            logger.debug("Pulled from the remote repository: " + call);
        } catch (GitAPIException exc) {
            logger.error("Error while performing pull command for git repository " + repository.toString(), exc);
        }
        return call;
    }

    private PublishedChangeSet processPull(Repository repository, ObjectId oldHead, ObjectId newHead) {
        PublishedChangeSet changeSet = new PublishedChangeSet(new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>());

        try  {
            RevWalk revWalk = new RevWalk(repository);
            RevCommit revCommit = revWalk.parseCommit(oldHead);
            ObjectId oldHeadTree = revCommit.getTree().getId();
            revCommit = revWalk.parseCommit(newHead);
            ObjectId newHeadTree = revCommit.getTree().getId();

            // prepare the two iterators to compute the diff between
            try (ObjectReader reader = repository.newObjectReader()) {
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset(reader, oldHeadTree);
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset(reader, newHeadTree);

                // finally get the list of changed files
                try (Git git = new Git(repository)) {
                    List<DiffEntry> diffs= git.diff()
                            .setNewTree(newTreeIter)
                            .setOldTree(oldTreeIter)
                            .call();
                    for (DiffEntry entry : diffs) {
                        logger.debug("Git Diff Entry: " + entry);
                        switch (entry.getChangeType()) {
                            case ADD:
                                changeSet.getCreatedFiles().add(File.separator + entry.getNewPath());
                                break;

                            case MODIFY:
                                changeSet.getUpdatedFiles().add(File.separator + entry.getNewPath());
                                break;

                            case DELETE:
                                changeSet.getDeletedFiles().add(File.separator + entry.getOldPath());
                                break;

                            case RENAME:
                                changeSet.getDeletedFiles().add(File.separator + entry.getOldPath());
                                changeSet.getCreatedFiles().add(File.separator + entry.getNewPath());
                                break;
                        }
                    }
                }
            }
        } catch (IncorrectObjectTypeException e) {
            e.printStackTrace();
        } catch (AmbiguousObjectException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return changeSet;
    }



    private void doPostProcessing(SiteConfiguration siteConfiguration, PublishedChangeSet changeSet) {
        logger.debug("Change Set:\n" + changeSet.toString());
        List<PublishingProcessor> processors = siteConfiguration.getPostProcessors();
        if (CollectionUtils.isNotEmpty(processors)) {
            Iterator<PublishingProcessor> iterator = processors.iterator();
            while (iterator.hasNext()) {
                PublishingProcessor processor = iterator.next();
                try {
                    processor.doProcess(siteConfiguration, changeSet);
                } catch (PublishingException exc) {
                    logger.error("Error executing processor " + processor.getName() + " for site " + siteConfiguration.getName(), exc);
                }
            }
        }
    }

    public SiteConfigurationLoader getSiteConfigurationLoader() { return siteConfigurationLoader; }
    public void setSiteConfigurationLoader(SiteConfigurationLoader siteConfigurationLoader) { this.siteConfigurationLoader = siteConfigurationLoader; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    private SiteConfigurationLoader siteConfigurationLoader;
    private boolean enabled;
}
