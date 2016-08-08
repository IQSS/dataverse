/*
 *  (C) Michael Bar-Sinai
 */
package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.groups.GroupException;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class ExplicitGroupTest {
    
    
    ExplicitGroupProvider prv = new ExplicitGroupProvider(null, null);
    
    public ExplicitGroupTest() {
    }
    
    @Test( expected=GroupException.class )
    public void addGroupToSelf() throws Exception {
        ExplicitGroup sut = new ExplicitGroup();
        sut.setDisplayName("a group");
        sut.add( sut );
        fail("A group cannot be added to itself.");
    }
    
    @Test( expected=GroupException.class )
    public void addGroupToDescendant() throws GroupException{
        Dataverse dv = MocksFactory.makeDataverse();
        ExplicitGroup root = new ExplicitGroup(prv);
        root.setId( MocksFactory.nextId() );
        root.setGroupAliasInOwner("top");
        ExplicitGroup sub = new ExplicitGroup(prv);
        sub.setGroupAliasInOwner("sub");
        sub.setId( MocksFactory.nextId() );
        ExplicitGroup subSub = new ExplicitGroup(prv);
        subSub.setGroupAliasInOwner("subSub");
        subSub.setId( MocksFactory.nextId() );
        root.setOwner(dv);
        sub.setOwner(dv);
        subSub.setOwner(dv);
        
        sub.add( subSub );
        root.add( sub );
        subSub.add(root);
        fail("A group cannot contain its parent");
    }
    
    @Test( expected=GroupException.class )
    public void addGroupToUnrealtedGroup() throws GroupException {
        Dataverse dv1 = MocksFactory.makeDataverse();
        Dataverse dv2 = MocksFactory.makeDataverse();
        ExplicitGroup g1 = new ExplicitGroup(prv);
        ExplicitGroup g2 = new ExplicitGroup(prv);
        g1.setOwner(dv1);
        g2.setOwner(dv2);
        
        g1.add(g2);
        fail("An explicit group cannot contain an explicit group defined in "
                + "a dataverse that's not an ancestor of that group's owner dataverse.");
        
    }
    
    @Test
    public void addGroup() throws GroupException {
        Dataverse dvParent = MocksFactory.makeDataverse();
        Dataverse dvSub = MocksFactory.makeDataverse();
        dvSub.setOwner(dvParent);
        
        ExplicitGroup g1 = new ExplicitGroup(prv);
        ExplicitGroup g2 = new ExplicitGroup(prv);
        g1.setOwner(dvSub);
        g2.setOwner(dvParent);
        
        g1.add(g2);
        assertTrue( g1.contains(g2) );
    }
}


