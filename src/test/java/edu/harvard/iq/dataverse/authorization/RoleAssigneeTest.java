package edu.harvard.iq.dataverse.authorization;

import org.junit.Test;

import junit.framework.Assert;

public class RoleAssigneeTest {
	@Test
	public void autocompleteMatchHandlesNulls(){
		RoleAssignee nullAssignee = null;
		RoleAssignee nullDisplayInfo = new RoleAssignee() {
			
			@Override
			public String getIdentifier() {
				return null;
			}
			@Override
			public RoleAssigneeDisplayInfo getDisplayInfo() {
				return null;
			}
		};
		String query = "test";
		Assert.assertFalse("false match on null assignee",RoleAssignee.autocompleteMatch.apply(nullAssignee).test(query));
		Assert.assertFalse("false match on null query",RoleAssignee.autocompleteMatch.apply(nullAssignee).test(query));
		Assert.assertFalse("false match on null identifier",RoleAssignee.autocompleteMatch.apply(nullDisplayInfo).test(query));
	}
	@Test
	public void autocompleteMatchesIdentifier(){
		RoleAssignee ra = new RoleAssignee() {
			
			@Override
			public String getIdentifier() {
				return "testTube";
			}
			@Override
			public RoleAssigneeDisplayInfo getDisplayInfo() {
				return null;
			}
		};
		String query = "test";
		Assert.assertTrue("failed to match identifier",RoleAssignee.autocompleteMatch.apply(ra).test(query));
	}
	@Test
	public void autocompleteDisplayInfo(){
		RoleAssignee ra = new RoleAssignee() {
			
			@Override
			public String getIdentifier() {
				return "blah";
			}
			@Override
			public RoleAssigneeDisplayInfo getDisplayInfo() {
				return new RoleAssigneeDisplayInfo("testTube", "emailAddress");
			}
		};
		String query = "test";
		Assert.assertTrue("failed to match identifier",RoleAssignee.autocompleteMatch.apply(ra).test(query));
	}
	
}
