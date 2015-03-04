package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

@SessionScoped
@Named("SuperUserPage")
public class SuperUserPage implements java.io.Serializable {

    @Inject
    DataverseSession session;

    @EJB
    IndexServiceBean indexService;

    private String indexAllStatus = "No status available";

    private Future<String> indexAllFuture;

    // modeled off http://docs.oracle.com/javaee/7/tutorial/ejb-async002.htm
    public String getIndexAllStatus() {
        if (indexAllFuture != null) {
            if (indexAllFuture.isDone()) {
                try {
                    indexAllStatus = indexAllFuture.get();
                } catch (ExecutionException | CancellationException | InterruptedException ex) {
                    indexAllStatus = ex.getCause().toString();
                }
            } else {
                indexAllStatus = "Index all is running...";
            }
        }
        return indexAllStatus;
    }

    public void startIndexAll() {
        User user = session.getUser();
        if (user.isSuperuser()) {
            indexAllFuture = indexService.indexAll();
            indexAllStatus = "Index all started...";
        } else {
            indexAllStatus = "Only a superuser can run index all";
        }
    }

    public void updateIndexAllStatus() {
        getIndexAllStatus();
    }

}
