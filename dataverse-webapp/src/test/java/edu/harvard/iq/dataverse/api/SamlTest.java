package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.dto.SamlIdentityProviderDTO;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.saml.SamlIdpManagementService;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SamlTest {

    @Mock
    private AuthenticationServiceBean authenticationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    protected SystemConfig systemConfig;

    @Mock
    private SamlIdpManagementService managementService;

    @InjectMocks
    private Saml endpoint;

    private AuthenticatedUser authenticatedUser = new AuthenticatedUser();

    @BeforeEach
    void setUp() {
        String token = "123456";
        endpoint.httpRequest = request;
        endpoint.authSvc = authenticationService;
        endpoint.systemConfig = systemConfig;
        authenticatedUser.setUserIdentifier("user");
        when(request.getParameter("key")).thenReturn(token);
        when(authenticationService.lookupUser(token)).thenReturn(authenticatedUser);
        when(systemConfig.isReadonlyMode()).thenReturn(true);
        authenticatedUser.setSuperuser(true);
    }

    @Test
    void getAllProviders() throws AbstractApiBean.WrappedResponse {
        // given
        List<SamlIdentityProviderDTO> providers = IntStream.rangeClosed(1, 3)
                .mapToObj(i -> new SamlIdentityProviderDTO((long) i, "EID" + i, "Url" + i, "DN" + i))
                .collect(Collectors.toList());
        when(managementService.listAll()).thenReturn(providers);

        // when
        Response response = endpoint.getAllProviders();

        // then
        String result = (String) response.getEntity();
        assertThat(result).containsSubsequence("EID1", "Url2", "DN3");
    }

    @Test
    void getProvider() throws AbstractApiBean.WrappedResponse {
        // given
        when(managementService.listSingle(1L))
                .thenReturn(Optional.of(new SamlIdentityProviderDTO(1L, "EID", "Url", "DN")));

        // when
        Response response = endpoint.getProvider(1L);

        // then
        String result = (String) response.getEntity();
        assertThat(result).containsSubsequence("1", "EID", "Url", "DN");
    }

    @Test
    void add() throws AbstractApiBean.WrappedResponse {
        // given
        SamlIdentityProviderDTO toAdd = new SamlIdentityProviderDTO(321L, "EID", "Url", "Third");
        when(managementService.create(anyList())).thenReturn(Either.right(Collections.singletonList(toAdd)));

        // when
        Response response = endpoint.add(Collections.singletonList(toAdd));

        // then
        String result = (String) response.getEntity();
        assertThat(result).containsSubsequence("Provider(s) added", "321", "EID", "Url", "Third");
    }

    @Test
    void update() throws AbstractApiBean.WrappedResponse {
        // given
        SamlIdentityProviderDTO toUpdate = new SamlIdentityProviderDTO(1L, "EID", "Url", "...");
        when(managementService.update(any(SamlIdentityProviderDTO.class))).thenReturn(Either.right(toUpdate));

        // when
        Response response = endpoint.update(toUpdate);

        // then
        String result = (String) response.getEntity();
        assertThat(result).containsSubsequence("Provider updated", "1", "EID", "Url", "...");
    }

    @Test
    void delete() throws AbstractApiBean.WrappedResponse {
        // given
        SamlIdentityProviderDTO toDelete = new SamlIdentityProviderDTO(1L, "EID", "Url", "Provider");
        when(managementService.delete(1L)).thenReturn(Either.right(toDelete));

        // when
        Response response = endpoint.delete(1L);

        // then
        String result = (String) response.getEntity();
        assertThat(result).containsSubsequence("Provider deleted", "1", "EID", "Url", "Provider");
    }
}