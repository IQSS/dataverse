package edu.harvard.iq.dataverse.sitemap;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

import com.redfin.sitemapgenerator.W3CDateFormat;
import com.redfin.sitemapgenerator.W3CDateFormat.Pattern;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.settings.ConfigCheckService;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;

public class SiteMapUtil {

    static final String DATE_PATTERN = "yyyy-MM-dd";
    static final String SITEMAP_FILENAME_STAGED = "sitemap.xml.staged";
    /** @see https://www.sitemaps.org/protocol.html#index */
    static final int SITEMAP_LIMIT = 50000;

    private static final Logger logger = Logger.getLogger(SiteMapUtil.class.getCanonicalName());
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);


    public static void updateSiteMap(List<Dataverse> dataverses, List<Dataset> datasets) {

        logger.info("BEGIN updateSiteMap");

        final String dataverseSiteUrl = SystemConfig.getDataverseSiteUrlStatic();
        final String msgErrorFormat = "Problem with %s : %s. The exception is %s";
        final String msgErrorW3CFormat = "%s isn't a valid W3C date time for %s. The exception is %s";
        final String sitemapPathString = getSitemapPathString();
        final String stagedSitemapPathAndFileString = sitemapPathString + File.separator + SITEMAP_FILENAME_STAGED;
        final Path stagedSitemapPath = Paths.get(stagedSitemapPathAndFileString);

        if (Files.exists(stagedSitemapPath)) {
            logger.warning(String.format(
                    "Unable to update sitemap! The staged file from a previous run already existed. Delete %s and try again.",
                    stagedSitemapPathAndFileString));
            return;
        }

        final File directory = new File(sitemapPathString);
        if (!directory.exists()) {
            directory.mkdir();
        }

        // Use DAY pattern (YYYY-MM-DD), local machine timezone
        final W3CDateFormat dateFormat = new W3CDateFormat(Pattern.DAY);
        WebSitemapGenerator wsg = null;
        try {
            // All sitemap files are in "sitemap" folder, see "getSitemapPathString" method.
            // But with pretty-faces configuration, "sitemap.xml" and "sitemap_index.xml" are accessible directly,
            // like "https://demo.dataverse.org/sitemap.xml". So "/sitemap/" need to be added on "WebSitemapGenerator"
            // in order to have valid URL for sitemap location.
            wsg = WebSitemapGenerator.builder(dataverseSiteUrl + "/sitemap/", directory).autoValidate(true).dateFormat(dateFormat)
                    .build();
        } catch (MalformedURLException e) {
            logger.warning(String.format(msgErrorFormat, "Dataverse site URL", dataverseSiteUrl, e.getLocalizedMessage()));
            return;
        }

        for (Dataverse dataverse : dataverses) {
            if (!dataverse.isReleased()) {
                continue;
            }
            final String dvAlias = dataverse.getAlias();
            final String dataverseUrl = dataverseSiteUrl + "/dataverse/" + dvAlias;
            final String lastModDate = getLastModDate(dataverse);
            try {
                final WebSitemapUrl url = new WebSitemapUrl.Options(dataverseUrl).lastMod(lastModDate).build();
                wsg.addUrl(url);
            } catch (MalformedURLException e) {
                logger.fine(String.format(msgErrorFormat, "dataverse URL", dataverseUrl, e.getLocalizedMessage()));
            } catch (ParseException e) {
                logger.fine(String.format(msgErrorW3CFormat, lastModDate, "dataverse alias " + dvAlias, e.getLocalizedMessage()));
            }
        }

        for (Dataset dataset : datasets) {
            // The deaccessioned check is last because it has to iterate through dataset versions.
            if (!dataset.isReleased() || dataset.isHarvested() || dataset.isDeaccessioned()) {
                continue;
            }
            final String datasetPid = dataset.getGlobalId().asString();
            final String datasetUrl = dataverseSiteUrl + "/dataset.xhtml?persistentId=" + datasetPid;
            final String lastModDate = getLastModDate(dataset);
            try {
                final WebSitemapUrl url = new WebSitemapUrl.Options(datasetUrl).lastMod(lastModDate).build();
                wsg.addUrl(url);
            } catch (MalformedURLException e) {
                logger.fine(String.format(msgErrorFormat, "dataset URL", datasetUrl, e.getLocalizedMessage()));
            } catch (ParseException e) {
                logger.fine(String.format(msgErrorW3CFormat, lastModDate, "dataset " + datasetPid, e.getLocalizedMessage()));
            }
        }

        logger.info(String.format("Writing and checking sitemap file into %s", sitemapPathString));
        try {
            wsg.write();
            if (dataverses.size() + datasets.size() > SITEMAP_LIMIT) {
                wsg.writeSitemapsWithIndex();
            }
        } catch (Exception ex) {
            final StringBuffer errorMsg = new StringBuffer("Unable to write or validate sitemap ! The exception is ");
            errorMsg.append(ex.getLocalizedMessage());
            // Add causes messages exception
            Throwable cause = ex.getCause();
            // Fix limit to 5 causes
            final int causeLimit = 5;
            int cpt = 0;
            while (cause != null && cpt < causeLimit) {
                errorMsg.append(" with cause ").append(cause.getLocalizedMessage());
                cause = ex.getCause();
                cpt = cpt + 1;
            }
            logger.warning(errorMsg.toString());
            return;
        }

        logger.info(String.format("Remove staged sitemap %s", stagedSitemapPathAndFileString));
        try {
            Files.deleteIfExists(stagedSitemapPath);
        } catch (IOException ex) {
            logger.warning("Unable to delete sitemap staged file! IOException: " + ex.getLocalizedMessage());
            return;
        }

        logger.info("END updateSiteMap");
    }

    private static String getLastModDate(DvObjectContainer dvObjectContainer) {
        // TODO: Decide if YYYY-MM-DD is enough. https://www.sitemaps.org/protocol.html
        // says "The date of last modification of the file. This date should be in W3C Datetime format.
        // This format allows you to omit the time portion, if desired, and use YYYY-MM-DD."
        return dvObjectContainer.getModificationTime().toLocalDateTime().format(formatter);
    }

    public static boolean stageFileExists() {
        String stagedSitemapPathAndFileString = getSitemapPathString() + File.separator + SITEMAP_FILENAME_STAGED;
        Path stagedPath = Paths.get(stagedSitemapPathAndFileString);
        if (Files.exists(stagedPath)) {
            logger.warning("Unable to update sitemap! The staged file from a previous run already existed. Delete " + stagedSitemapPathAndFileString + " and try again.");
            return true;
        }
        return false;
    }

    /**
     * Lookup the location where to generate the sitemap.
     *
     * Note: the location is checked to be configured, does exist and is writeable in
     * {@link ConfigCheckService#checkSystemDirectories()}
     *
     * @return Sitemap storage location ([docroot]/sitemap)
     */
    private static String getSitemapPathString() {
        return JvmSettings.DOCROOT_DIRECTORY.lookup() + File.separator + "sitemap";
    }

}
