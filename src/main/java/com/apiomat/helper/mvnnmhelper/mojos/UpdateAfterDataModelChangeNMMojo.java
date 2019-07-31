/* Copyright (c) 2011 - 2019 All Rights Reserved, http://www.apiomat.com/
 *
 * This source is property of apiomat.com. You are not allowed to use or distribute this code without a contract
 * explicitly giving you these permissions. Usage of this code includes but is not limited to running it on a server or
 * copying parts from it.
 *
 * Apinauten GmbH, Hainstrasse 4, 04109 Leipzig, Germany
 *
 * Feb 25, 2019
 * thum */
package com.apiomat.helper.mvnnmhelper.mojos;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

/**
 * Goal to upload a native module artifact
 *
 * @author thum
 */
@Mojo( name = "updateAfterDatamodelChange", defaultPhase = LifecyclePhase.NONE )
public class UpdateAfterDataModelChangeNMMojo extends AbstractRequestMojo
{
	@Override
	public void executeRequest( ) throws MojoExecutionException
	{
		executeGoal( "download" );

		/* use maven invoker here to execute the clean and package phases */
		final List<String> goalList = new LinkedList<>( );
		goalList.add( "clean" );
		goalList.add( "package" );
		final InvocationRequest request = new DefaultInvocationRequest( );
		request.setPomFile( new File( this.baseDirectory, "pom.xml" ) ).setGoals( goalList ).setInteractive( true );
		final Invoker invoker = new DefaultInvoker( );
		try
		{
			final InvocationResult invResult = invoker.execute( request );
			if ( invResult.getExitCode( ) != 0 )
			{
				throw new MojoExecutionException( "Clean and/or Package failed", invResult.getExecutionException( ) );
			}
		}
		catch ( final MavenInvocationException e )
		{
			throw new MojoExecutionException( "Could not execute clean and/or package phase", e );
		}

		executeGoal( "upload" );
	}

}
