package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DataCitation;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DOIDataCiteRegisterService;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.bagit.BagGenerator;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.DigestInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RequiredPermissions(Permission.PublishDataset)
public abstract class AbstractSubmitToArchiveCommand extends AbstractCommand<DatasetVersion> {

    private final DatasetVersion version;
    private final Map<String, String> requestedSettings = new HashMap<String, String>();
    protected boolean success=false;
    private static final Logger logger = Logger.getLogger(AbstractSubmitToArchiveCommand.class.getName());
    private static final int MAX_ZIP_WAIT = 20000;
    private static final int DEFAULT_THREADS = 2;
    
    public AbstractSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        super(aRequest, version.getDataset());
        this.version = version;
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {

        String settings = ctxt.settings().getValueForKey(SettingsServiceBean.Key.ArchiverSettings);
        String[] settingsArray = settings.split(",");
        for (String setting : settingsArray) {
            setting = setting.trim();
            if (!setting.startsWith(":")) {
                logger.warning("Invalid Archiver Setting: " + setting);
            } else {
                requestedSettings.put(setting, ctxt.settings().get(setting));
            }
        }
        
        AuthenticatedUser user = getRequest().getAuthenticatedUser();
        ApiToken token = ctxt.authentication().findApiTokenByUser(user);
        if (token == null) {
            //No un-expired token
            token = ctxt.authentication().generateApiTokenForUser(user);
        }
        performArchiveSubmission(version, token, requestedSettings);
        return ctxt.em().merge(version);
    }

    /**
     * This method is the only one that should be overwritten by other classes. Note
     * that this method may be called from the execute method above OR from a
     * workflow in which execute() is never called and therefore in which all
     * variables must be sent as method parameters. (Nominally version is set in the
     * constructor and could be dropped from the parameter list.)
     * 
     * @param version - the DatasetVersion to archive
     * @param token - an API Token for the user performing this action
     * @param requestedSettings - a map of the names/values for settings required by this archiver (sent because this class is not part of the EJB context (by design) and has no direct access to service beans).
     */
    abstract public WorkflowStepResult performArchiveSubmission(DatasetVersion version, ApiToken token, Map<String, String> requestedSetttings);

    protected int getNumberOfBagGeneratorThreads() {
        if (requestedSettings.get(BagGenerator.BAG_GENERATOR_THREADS) != null) {
            try {
                return Integer.valueOf(requestedSettings.get(BagGenerator.BAG_GENERATOR_THREADS));
            } catch (NumberFormatException nfe) {
                logger.warning("Can't parse the value of setting " + BagGenerator.BAG_GENERATOR_THREADS
                        + " as an integer - using default:" + DEFAULT_THREADS);
            }
        }
        return DEFAULT_THREADS;
    }

    @Override
    public String describe() {
        return super.describe() + "DatasetVersion: [" + version.getId() + " (v"
                + version.getFriendlyVersionNumber()+")]";
    }
    
    String getDataCiteXml(DatasetVersion dv) {
        DataCitation dc = new DataCitation(dv);
        Map<String, String> metadata = dc.getDataCiteMetadata();
        return DOIDataCiteRegisterService.getMetadataFromDvObject(dv.getDataset().getGlobalId().asString(), metadata,
                dv.getDataset());
    }

    public Thread startBagThread(DatasetVersion dv, PipedInputStream in, DigestInputStream digestInputStream2,
            String dataciteXml, ApiToken token) throws IOException, InterruptedException {
        Thread bagThread = new Thread(new Runnable() {
            public void run() {
                try (PipedOutputStream out = new PipedOutputStream(in)) {
                    // Generate bag
                    BagGenerator bagger = new BagGenerator(new OREMap(dv, false), dataciteXml);
                    bagger.setNumConnections(getNumberOfBagGeneratorThreads());
                    bagger.setAuthenticationKey(token.getTokenString());
                    bagger.generateBag(out);
                    success = true;
                } catch (Exception e) {
                    logger.severe("Error creating bag: " + e.getMessage());
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    try {
                        digestInputStream2.close();
                    } catch (Exception ex) {
                        logger.warning(ex.getLocalizedMessage());
                }
                    throw new RuntimeException("Error creating bag: " + e.getMessage());
            }
            }
        });
        bagThread.start();
        /*
         * The following loop handles two issues. First, with no delay, the
         * bucket.create() call below can get started before the piped streams are set
         * up, causing a failure (seen when triggered in a PostPublishDataset workflow).
         * A minimal initial wait, e.g. until some bytes are available, would address
         * this. Second, the BagGenerator class, due to it's use of parallel streaming
         * creation of the zip file, has the characteristic that it makes a few bytes
         * available - from setting up the directory structure for the zip file -
         * significantly earlier than it is ready to stream file content (e.g. for
         * thousands of files and GB of content). If, for these large datasets,
         * the transfer is started as soon as bytes are available, the call can
         * timeout before the bytes for all the zipped files are available. To manage
         * this, the loop waits until 90K bytes are available, larger than any expected
         * dir structure for the zip and implying that the main zipped content is
         * available, or until the thread terminates, with all of its content written to
         * the pipe. (Note the PipedInputStream buffer is set at 100K above - I didn't
         * want to test whether that means that exactly 100K bytes will be available()
         * for large datasets or not, so the test below is at 90K.)
         * 
         * An additional sanity check limits the wait to 20K (MAX_ZIP_WAIT) seconds. The BagGenerator
         * has been used to archive >120K files, 2K directories, and ~600GB files on the
         * SEAD project (streaming content to disk rather than over an internet
         * connection) which would take longer than 20K seconds (even 10+ hours) and might
         * produce an initial set of bytes for directories > 90K. If Dataverse ever
         * needs to support datasets of this size, the numbers here would need to be
         * increased, and/or a change in how archives are sent to google (e.g. as
         * multiple blobs that get aggregated) would be required.
         */
        int i = 0;
        while (digestInputStream2.available() <= 90000 && i < MAX_ZIP_WAIT && bagThread.isAlive()) {
            Thread.sleep(1000);
            logger.fine("avail: " + digestInputStream2.available() + " : " + bagThread.getState().toString());
            i++;
        }
        logger.fine("Bag: transfer started, i=" + i + ", avail = " + digestInputStream2.available());
        if(i==MAX_ZIP_WAIT) {
            throw new IOException("Stream not available");
        }
        return bagThread;
    }

    public static boolean isArchivable(Dataset dataset, SettingsWrapper settingsWrapper) {
        return true;
   }
   
   //Check if the chosen archiver imposes single-version-only archiving - in a View context
   public static boolean isSingleVersion(SettingsWrapper settingsWrapper) {
       return false;
  }
 
   //Check if the chosen archiver imposes single-version-only archiving - in the API
   public static boolean isSingleVersion(SettingsServiceBean settingsService) {
       return false;
  }
}
