/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.shacl.manifest;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/**
 * A SHACL constraint test suite, created by reading in a W3C working-group style manifest.
 * 
 * @author James Leigh
 */
public abstract class AbstractSHACLTest extends TestCase {

	/*-----------*
	 * Constants *
	 *-----------*/

	// Logger for non-static tests, so these results can be isolated based on
	// where they are run
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	protected final String testURI;

	protected final Model shapesGraph;

	protected final Model dataGraph;

	protected final boolean failure;

	protected final boolean conforms;

	/*-----------*
	 * Variables *
	 *-----------*/

	protected Repository dataRep;
	protected SailRepository shapesRep;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public AbstractSHACLTest(String testURI, String label, Model shapesGraph, Model dataGraph,
			boolean failure, boolean conforms)
	{
		super(label.replaceAll("\\(", " ").replaceAll("\\)", " "));

		this.testURI = testURI;
		this.shapesGraph = shapesGraph;
		this.dataGraph = dataGraph;
		this.failure = failure;
		this.conforms = conforms;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setUp()
		throws Exception
	{
		shapesRep = createShapesRepository();
		if (shapesGraph != null) {
			try {
				upload(shapesRep, shapesGraph);
			}
			catch (Exception exc) {
				try {
					shapesRep.shutDown();
					shapesRep = null;
				}
				catch (Exception e2) {
					logger.error(e2.toString(), e2);
				}
				throw exc;
			}
		}
		dataRep = createDataRepository(shapesRep);
	}

	protected SailRepository createShapesRepository()
		throws Exception
	{
		SailRepository repo = new SailRepository(newSail());
		repo.initialize();
		RepositoryConnection con = repo.getConnection();
		try {
			con.clear();
			con.clearNamespaces();
		}
		finally {
			con.close();
		}
		return repo;
	}

	protected Repository createDataRepository(SailRepository shapesRep)
		throws Exception
	{
		Repository repo = new SailRepository(newShaclSail(shapesRep));
		repo.initialize();
		RepositoryConnection con = repo.getConnection();
		try {
			con.clear();
			con.clearNamespaces();
		}
		finally {
			con.close();
		}
		return repo;
	}

	protected abstract Sail newSail();

	protected abstract Sail newShaclSail(SailRepository shapesRep);

	@Override
	public void tearDown()
		throws Exception
	{
		if (dataRep != null) {
			dataRep.shutDown();
			dataRep = null;
		}
	}

	@Override
	public void runTest()
		throws Exception
	{
		try {
			upload(dataRep, dataGraph);
			assertTrue(conforms);
		}
		catch (RepositoryException exc) {
			if (conforms || !(exc.getCause() instanceof SailException)) {
				throw exc;
			}
		}
	}

	protected void upload(Repository rep, Model dataGraph) {
		RepositoryConnection con = rep.getConnection();

		try {
			con.begin();
			con.add(dataGraph);
			con.commit();
		}
		catch (Exception e) {
			if (con.isActive()) {
				con.rollback();
			}
			throw e;
		}
		finally {
			con.close();
		}
	}
}
