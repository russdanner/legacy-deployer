package org.craftercms.deployer.git.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SiteConfigurationLoader {

    private final static Logger logger = LoggerFactory.getLogger(SiteConfigurationLoader.class);

    public List<String> getSitesList() {
        logger.debug("Loading site list");
        List<String> sites = new ArrayList<String>();

        Path dir = Paths.get(configurationLocation);
        dir.toAbsolutePath().toString();
        logger.debug("Config location " + dir.toAbsolutePath().toString());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir.toAbsolutePath())) {

            for (Path entry: stream) {
                logger.debug("processing file " + entry.normalize().toString());
                Path filename = entry.getFileName();
                String strFilename = filename.toString();
                sites.add(strFilename.replaceAll(".yaml", ""));
            }
        } catch (IOException exc) {
            logger.error("Error while loading list of sites.", exc);
        }

        logger.debug("Found sites:");
        for (String site : sites) {
            logger.debug(site);
        }
        return sites;
    }

    public SiteConfiguration loadSiteConfiguration(String site) {
        SiteConfiguration siteConfiguration = null;

        Path siteConfigurationPath = Paths.get(configurationLocation, site + ".yaml");
        try (InputStream in = Files.newInputStream(siteConfigurationPath)) {
            Yaml yaml = new Yaml();
            siteConfiguration = yaml.loadAs(in, SiteConfiguration.class);

            logger.debug("Configuration loaded for " + site);
            logger.debug(yaml.dump(siteConfiguration));
        } catch (IOException exc) {
            logger.error("Error while loading site configuration from location " + siteConfigurationPath.normalize());
        }

        return siteConfiguration;
    }

    public String getConfigurationLocation() { return configurationLocation; }
    public void setConfigurationLocation(String configurationLocation) { this.configurationLocation = configurationLocation; }

    private String configurationLocation;
}
