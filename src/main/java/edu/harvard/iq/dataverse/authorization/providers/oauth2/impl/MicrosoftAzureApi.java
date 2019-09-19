package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.ParameterList;
import com.github.scribejava.core.model.Verb;
import java.util.Map;

/**
 *
 * @CIMMYT
 *
 */

public class MicrosoftAzureApi extends DefaultApi20{

    private static class InstanceHolder {
        private static final MicrosoftAzureApi INSTANCE = new MicrosoftAzureApi();
    }

    public static MicrosoftAzureApi instance()
    {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public Verb getAccessTokenVerb()
    {
        return Verb.POST;
    }

    @Override
    public String getAccessTokenEndpoint()
    {
        return "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    }

    @Override
    protected String getAuthorizationBaseUrl()
    {
        return "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config, Map<String, String> additionalParams)
    {
        ParameterList parameters = new ParameterList(additionalParams);

        parameters.add("response_type", config.getResponseType());
        parameters.add("client_id", config.getApiKey());
        String callback = config.getCallback();
        if (callback != null) {
            parameters.add("redirect_uri", callback);
        }
        parameters.add("scope", "user.read");
        //parameters.add("domain_hint", "cgiar.org");

        String state = config.getState();
        if (state != null) {
            parameters.add("state", state);
        }
        return parameters.appendTo(getAuthorizationBaseUrl());
    }

}
