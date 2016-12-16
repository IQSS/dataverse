package edu.harvard.iq.dataverse.authorization.providers.echo;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderFactory;
import edu.harvard.iq.dataverse.authorization.providers.AuthenticationProviderRow;

/**
 *
 * @author michael
 *
 * @deprecated This was a useful example of a non builtin factory but now we
 * have real world factories such as OAuth2AuthenticationProviderFactory and
 * ShibAuthenticationProviderFactory so we could consider deleting this example
 * from the code base.
 */
@Deprecated
public class EchoAuthenticationProviderFactory implements AuthenticationProviderFactory {

    @Override
    public String getAlias() {
        return "Echo";
    }

    @Override
    public String getInfo() {
        return "A proof-of-concept provider that approves everyone.";
    }

    @Override
    public AuthenticationProvider buildProvider(AuthenticationProviderRow aRow) throws AuthorizationSetupException {
        String rawData = aRow.getFactoryData();
        String[] data = {"",""};
        if ( rawData != null ) {
            data = aRow.getFactoryData().split(",",-1);
        }
        try {
        return new EchoAuthenticationProvider(aRow.getId(),
                    data[0], data[1],
                    new AuthenticationProviderDisplayInfo(aRow.getId(), aRow.getTitle(), aRow.getSubtitle()));
        } catch (ArrayIndexOutOfBoundsException e ) {
            throw new AuthorizationSetupException("Can't create Echo prov. Raw data: '" + rawData +"'", e);
        }
    }
    
}
