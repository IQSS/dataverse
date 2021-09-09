package edu.harvard.iq.dataverse.api.errorhandlers;

import edu.harvard.iq.dataverse.api.AbstractApiBean;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WrappedResponseExceptionHandler implements ExceptionMapper<AbstractApiBean.WrappedResponse> {

    @Override
    public Response toResponse(AbstractApiBean.WrappedResponse wrappedResponse) {
        return wrappedResponse.getResponse();
    }

}
