package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.DOIServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

@Stateless
public class FakePidProviderServiceBean extends DOIServiceBean {

    private static final Logger logger = Logger.getLogger(FakePidProviderServiceBean.class.getCanonicalName());

    @PostConstruct
    private void init() {
        String doiProvider = settingsService.getValueForKey(Key.DoiProvider, "");
        if("FAKE".equals(doiProvider)) {
            isConfigured=true;
        }
    }
    
    //Only need to check locally
    public boolean isGlobalIdUnique(GlobalId globalId) {
        try {
            return ! alreadyExists(globalId);
        } catch (Exception e){
            //we can live with failure - means identifier not found remotely
        }
        return true;
    }
    
    @Override
    public boolean alreadyExists(GlobalId globalId) throws Exception {
        return ! dvObjectService.isGlobalIdLocallyUnique(globalId);
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
    public boolean publicizeIdentifier(DvObject studyIn) {
        return true;
    }

}
