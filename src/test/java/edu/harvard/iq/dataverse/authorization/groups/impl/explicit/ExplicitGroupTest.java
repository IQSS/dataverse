/*
 *  (C) Michael Bar-Sinai
 */
package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.authorization.groups.GroupException;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressRange;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MockRoleAssigneeServiceBean;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author michael
 */
public class ExplicitGroupTest {
    
    
    MockRoleAssigneeServiceBean roleAssigneeSvc = new MockRoleAssigneeServiceBean();
    ExplicitGroupProvider prv = new ExplicitGroupProvider(null, roleAssigneeSvc);
    
    public ExplicitGroupTest() {
    }
    
    @Test
    public void addGroupToSelf() throws Exception {
        ExplicitGroup sut = new ExplicitGroup();
        sut.setDisplayName("a group");
        assertThrows(GroupException.class, () -> sut.add( sut ), "A group cannot be added to itself.");
    }
    
    @Test
    public void addGroupToDescendant() throws GroupException{
        Dataverse dv = makeDataverse();
        ExplicitGroup root = new ExplicitGroup(prv);
        root.setId( nextId() );
        root.setGroupAliasInOwner("top");
        ExplicitGroup sub = new ExplicitGroup(prv);
        sub.setGroupAliasInOwner("sub");
        sub.setId( nextId() );
        ExplicitGroup subSub = new ExplicitGroup(prv);
        subSub.setGroupAliasInOwner("subSub");
        subSub.setId( nextId() );
        root.setOwner(dv);
        sub.setOwner(dv);
        subSub.setOwner(dv);
        
        sub.add( subSub );
        root.add( sub );
        assertThrows(GroupException.class, () -> subSub.add(root), "A group cannot contain its parent");
    }
    
    @Test
    public void addGroupToUnrealtedGroup() throws GroupException {
        Dataverse dv1 = makeDataverse();
        Dataverse dv2 = makeDataverse();
        ExplicitGroup g1 = new ExplicitGroup(prv);
        ExplicitGroup g2 = new ExplicitGroup(prv);
        g1.setOwner(dv1);
        g2.setOwner(dv2);
        
        assertThrows(GroupException.class, () -> g1.add(g2), "An explicit group cannot contain an" +
            "explicit group defined in a dataverse that's not an ancestor of that group's owner dataverse.");
        
    }
    
    @Test
    public void addGroup() throws GroupException {
        Dataverse dvParent = makeDataverse();
        Dataverse dvSub = makeDataverse();
        dvSub.setOwner(dvParent);
        
        ExplicitGroup g1 = new ExplicitGroup(prv);
        ExplicitGroup g2 = new ExplicitGroup(prv);
        g1.setOwner(dvSub);
        g2.setOwner(dvParent);
        
        g1.add(g2);
        assertTrue( g1.structuralContains(g2) );
    }
    
    @Test
    public void adds() throws GroupException {
        Dataverse dvParent = makeDataverse();
        ExplicitGroup g1 = new ExplicitGroup(prv);
        g1.setOwner(dvParent);
        
        AuthenticatedUser au1 = makeAuthenticatedUser("Lauren", "Ipsum");
        g1.add(au1);
        g1.add( GuestUser.get() );
        
        assertTrue( g1.structuralContains(GuestUser.get()) );
        assertTrue( g1.structuralContains(au1) );
        assertFalse( g1.structuralContains(makeAuthenticatedUser("Sima", "Kneidle")) );
        assertFalse( g1.structuralContains(AllUsers.get()) );
    }
    
    
    @Test
    public void recursiveStructuralContainment() throws GroupException {
        Dataverse dvParent = makeDataverse();
        ExplicitGroup parentGroup     = roleAssigneeSvc.add(makeExplicitGroup(prv));
        ExplicitGroup childGroup      = roleAssigneeSvc.add(makeExplicitGroup(prv));
        ExplicitGroup grandChildGroup = roleAssigneeSvc.add(makeExplicitGroup(prv));
        parentGroup.setOwner(dvParent);
        childGroup.setOwner(dvParent);
        grandChildGroup.setOwner(dvParent);
        
        childGroup.add(grandChildGroup);
        parentGroup.add(childGroup);
        
        AuthenticatedUser au = roleAssigneeSvc.add(makeAuthenticatedUser("Jane", "Doe"));
        grandChildGroup.add( au );
        childGroup.add( GuestUser.get() );
        
        assertTrue( grandChildGroup.structuralContains(au) );
        assertTrue( childGroup.structuralContains(au) );
        assertTrue( parentGroup.structuralContains(au) );
        
        assertTrue( childGroup.structuralContains(GuestUser.get()) );
        assertTrue( parentGroup.structuralContains(GuestUser.get()) );
        
        grandChildGroup.remove(au);
        
        assertFalse( grandChildGroup.structuralContains(au) );
        assertFalse( childGroup.structuralContains(au) );
        assertFalse( parentGroup.structuralContains(au) );
        
        childGroup.add( AuthenticatedUsers.get() );
        
        assertFalse( grandChildGroup.structuralContains(au) );
        assertFalse( childGroup.structuralContains(au) );
        assertFalse( parentGroup.structuralContains(au) );
        assertTrue( childGroup.structuralContains(AuthenticatedUsers.get()) );

        final IpGroup ipGroup = new IpGroup(new IpGroupProvider(null));
        grandChildGroup.add(ipGroup);
        ipGroup.add( IpAddressRange.make(IpAddress.valueOf("0.0.1.1"), IpAddress.valueOf("0.0.255.255")) );
        
        assertTrue( grandChildGroup.structuralContains(ipGroup) );
        assertTrue( childGroup.structuralContains(ipGroup) );
        assertTrue( parentGroup.structuralContains(ipGroup) );
    }
    
    @Test
    public void recursiveLogicalContainment() throws GroupException {
        Dataverse dvParent = makeDataverse();
        ExplicitGroup parentGroup     = roleAssigneeSvc.add(makeExplicitGroup("parent", prv));
        ExplicitGroup childGroup      = roleAssigneeSvc.add(makeExplicitGroup("child", prv));
        ExplicitGroup grandChildGroup = roleAssigneeSvc.add(makeExplicitGroup("grandChild", prv));
        parentGroup.setOwner(dvParent);
        childGroup.setOwner(dvParent);
        grandChildGroup.setOwner(dvParent);
        
        childGroup.add(grandChildGroup);
        parentGroup.add(childGroup);
        
        AuthenticatedUser au = roleAssigneeSvc.add(makeAuthenticatedUser("Jane", "Doe"));
        grandChildGroup.add( au );
        childGroup.add( GuestUser.get() );
        DataverseRequest auReq = makeRequest(au);
        DataverseRequest guestReq = makeRequest();
        
        assertTrue( grandChildGroup.contains(auReq) );
        assertTrue( childGroup.contains(auReq) );
        assertTrue( parentGroup.contains(auReq) );
        
        assertTrue( childGroup.contains(guestReq) );
        assertTrue( parentGroup.contains(guestReq) );
        
        grandChildGroup.remove(au);
        
        assertFalse( grandChildGroup.contains(auReq) );
        assertFalse( childGroup.contains(auReq) );
        assertFalse( parentGroup.contains(auReq) );
        
        childGroup.add( AuthenticatedUsers.get() );
        
        assertFalse( grandChildGroup.contains(auReq) );
        assertTrue( childGroup.contains(auReq) );
        assertTrue( parentGroup.contains(auReq) );

        final IpGroup ipGroup = roleAssigneeSvc.add( new IpGroup(new IpGroupProvider(null)) );
        grandChildGroup.add(ipGroup);
        ipGroup.add( IpAddressRange.make(IpAddress.valueOf("0.0.1.1"), IpAddress.valueOf("0.0.255.255")) );
        final IpAddress ip = IpAddress.valueOf("0.0.128.128");
        final DataverseRequest request = new DataverseRequest(GuestUser.get(), ip);
        
        assertTrue( ipGroup.contains(request) ); 
        assertTrue( grandChildGroup.contains(request) );
        assertTrue( parentGroup.contains(request) );
        
        childGroup.add( GuestUser.get() );
        assertTrue( childGroup.contains(guestReq) );
        assertTrue( parentGroup.contains(guestReq) );
        assertFalse( grandChildGroup.contains(guestReq) );
        
    }
}


