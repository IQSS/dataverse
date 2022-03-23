package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.AbstractGlobalIdServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;

import java.lang.StackWalker.StackFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import jakarta.ejb.Stateless;

@Stateless
public class FakePidProviderServiceBean extends AbstractGlobalIdServiceBean {

    private static final Logger logger = Logger.getLogger(FakePidProviderServiceBean.class.getCanonicalName());

    @Override
    public boolean alreadyExists(DvObject dvo) throws Exception {
        /*
         * This method is called in cases where the 'right' answer can be true or false:
         * 
         * When called via CreateNewDatasetCommand (direct upload case), we expect
         * 'false' as the response, whereas when called from ImportDatasetCommand or
         * DeleteDataFileCommand, we expect 'true' as a confirmation that the expected
         * PID exists.
         * 
         * This method now checks the stack and can send true/false as expected by the
         * calling command as the right default/normal case.
         *
         * Alternately, this method could check the database as is done in
         * DatasetServiceBean.isIdentifierLocallyUnique() (needs a similar method for
         * DataFiles and could be refactored to only have one query for both).
         */
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        if (walker.walk(this::getCallingClass)) {
            logger.fine("Called from CreateNewDatasetCommand");
            return false;
        }
        return true;
    }

    private boolean getCallingClass(Stream<StackFrame> stackFrameStream) {
        /*
         * If/when other cases require a true response from the alreadyExists method,
         * add those class names to the test below.
         */
        return stackFrameStream
                .filter(frame -> frame.getDeclaringClass().getSimpleName()
                        .equals(CreateNewDatasetCommand.class.getSimpleName()))
                .findFirst().map(f -> true).orElse(false);
    }
    
    @Override
    public boolean alreadyExists(GlobalId globalId) throws Exception {
        //Could use the same method as above to return false if/when needed.
        return true;
    }

    @Override
    public boolean registerWhenPublished() {
        return false;
    }

    @Override
    public List<String> getProviderInformation() {
        ArrayList<String> providerInfo = new ArrayList<>();
        String providerName = "FAKE";
        String providerLink = "http://dataverse.org";
        providerInfo.add(providerName);
        providerInfo.add(providerLink);
        return providerInfo;
    }

    @Override
    public String createIdentifier(DvObject dvo) throws Throwable {
        return "fakeIdentifier";
    }

    @Override
    public Map<String, String> getIdentifierMetadata(DvObject dvo) {
        Map<String, String> map = new HashMap<>();
        return map;
    }

    @Override
    public String modifyIdentifierTargetURL(DvObject dvo) throws Exception {
        return "fakeModifyIdentifierTargetURL";
    }

    @Override
    public void deleteIdentifier(DvObject dvo) throws Exception {
        // no-op
    }

    @Override
    public Map<String, String> lookupMetadataFromIdentifier(String protocol, String authority, String identifier) {
        Map<String, String> map = new HashMap<>();
        return map;
    }

    @Override
    public boolean publicizeIdentifier(DvObject studyIn) {
        return true;
    }

}
