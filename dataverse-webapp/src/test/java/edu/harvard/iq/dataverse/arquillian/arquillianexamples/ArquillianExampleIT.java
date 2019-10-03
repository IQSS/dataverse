package edu.harvard.iq.dataverse.arquillian.arquillianexamples;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class ArquillianExampleIT extends WebappArquillianDeployment {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    private BuiltinUserServiceBean builtinUserServiceBean;

    @Test
    public void should_create_greeting() {
        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName("MACIEK");
        builtinUser.setPasswordEncryptionVersion(1);
        builtinUserServiceBean.save(builtinUser);

        List list = em.createNativeQuery("SELECT * FROM builtinuser").getResultList();
        list.forEach(System.out::println);
    }
}
