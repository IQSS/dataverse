package edu.harvard.iq.dataverse.arquillian.facesmock;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class FacesContextMocker extends FacesContext {

    private static final Release RELEASE = new Release();

    public static FacesContext mockServletRequest() {
        FacesContext context = mock(FacesContext.class);
        setCurrentInstance(context);
        Mockito.doAnswer(RELEASE)
                .when(context)
                .release();

        Map<String, Object> session = new HashMap<>();
        ExternalContext ext = mock(ExternalContext.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(ext.getSessionMap()).thenReturn(session);
        when(context.getExternalContext()).thenReturn(ext);
        when(context.getExternalContext().getRequestLocale()).thenReturn(Locale.ENGLISH);
        when(ext.getRequest()).thenReturn(request);
        when(ext.isUserInRole(anyString())).thenReturn(true);
        return context;
    }

    private static class Release implements Answer<Void> {

        @Override
        public Void answer(InvocationOnMock invocation) {
            setCurrentInstance(null);
            return null;
        }
    }
}
