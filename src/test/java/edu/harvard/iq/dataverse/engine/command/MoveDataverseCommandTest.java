/*
 *  (C) Michael Bar-Sinai
 *//*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.engine.command;

import edu.harvard.iq.dataverse.engine.command.impl.MoveDataverseCommand;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.engine.DataverseEngine;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.TestDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class MoveDataverseCommandTest {
	
	Dataverse root, childA, childB, grandchildAA;
	DataverseEngine testEngine;
	
	@Before
	public void setUp() {
		root = new Dataverse();
		root.setName("root");
		root.setId(1l);
		childA = new Dataverse();
		childA.setName( "childA" );
		childA.setId(2l);
		childB = new Dataverse();
		childB.setName( "childB" );
		childB.setId(3l);
		grandchildAA = new Dataverse();
		grandchildAA.setName( "grandchildAA" );
		grandchildAA.setId(4l);
		
		childA.setOwner( root );
		childB.setOwner( root );
		grandchildAA.setOwner( childA );
		
		
		testEngine = new TestDataverseEngine( new TestCommandContext(){
			@Override
			public DataverseServiceBean dataverses() {
				return new DataverseServiceBean(){
					@Override
					public Dataverse save(Dataverse dataverse) {
						// no-op. The superclass accesses databases which we don't have.
						return dataverse;
			}};}});
	}
	
	/**
	 * Moving ChildB to ChildA
	 * @throws Exception - should not throw an exception
	 */
	@Test
	public void testValidMove() throws Exception {
		testEngine.submit(
				new MoveDataverseCommand(null, childB, childA));
		
		assertEquals( childA, childB.getOwner() );
		assertEquals( Arrays.asList(root, childA), childB.getOwners() );
	}
	
	/**
	 * Moving ChildA to its child (illegal).
	 */
	@Test( expected=IllegalCommandException.class )
	public void testInvalidMove() throws Exception {
		testEngine.submit(
				new MoveDataverseCommand(null, childA, grandchildAA));
		fail();
	}
	
}
