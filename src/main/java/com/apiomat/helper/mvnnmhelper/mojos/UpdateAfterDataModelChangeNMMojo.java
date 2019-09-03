/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.apiomat.helper.mvnnmhelper.mojos;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

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
