package edu.harvard.iq.dataverse.engine;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import javax.persistence.EntityManager;

/**
 * A base CommandContext for tests. Provides no-op implementations. Should probably be
 * overridden for actual tests.
 * 
 * @author michael
 */
public class TestCommandContext implements CommandContext {

	@Override
	public DatasetServiceBean datasets() {
		return null;
	}

	@Override
	public DataverseServiceBean dataverses() {
		return null;
	}

	@Override
	public DataverseRoleServiceBean roles() {
		return null;
	}

	@Override
	public BuiltinUserServiceBean users() {
		return null;
	}

	@Override
	public IndexServiceBean index() {
		return null;
	}

	@Override
	public SearchServiceBean search() {
		return null;
	}

	@Override
	public PermissionServiceBean permissions() {
		return null;
	}

	@Override
	public DvObjectServiceBean dvObjects() {
		return null;
	}

	@Override
	public EntityManager em() {
		return null;
	}

	@Override
	public DataverseFacetServiceBean facets() {
		return null;
	}
	
        @Override
	public FeaturedDataverseServiceBean featuredDataverses() {
		return null;
	}
        
        @Override
	public TemplateServiceBean templates() {
		return null;
	}
        
	@Override
	public DataverseEngine engine() { return new TestDataverseEngine(this); }

    @Override
    public DataFileServiceBean files() {
        return null;
    }
	
}
