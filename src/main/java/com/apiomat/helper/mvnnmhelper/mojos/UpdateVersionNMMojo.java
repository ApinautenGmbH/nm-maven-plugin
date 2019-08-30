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

import com.apiomat.helper.mvnnmhelper.ModuleUpdateManager;
import com.apiomat.helper.mvnnmhelper.VersionCompareHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;

/**
 * Goal to update the module contents to the specified yambas version
 *
 * @author thum
 */
@Mojo( name = "updateVersion", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class UpdateVersionNMMojo extends AbstractModuleMojo
{

	/**
	 * specifies the version from which the routines should be executed
	 */
	@Parameter( defaultValue = "${project.name}", property = "fromVersion" )
	protected String fromVersion;

	/**
	 * specifies the version to which the routines should be executed
	 */
	@Parameter( defaultValue = "${project.name}", property = "toVersion" )
	protected String toVersion;

	@Override
	public void execute( ) throws MojoExecutionException
	{
		/* if execution should be skipped then return directly */
		if ( this.nmSkip )
		{
			getLog( ).info( "Execution skipped" );
			return;
		}

		/* INTERNAL NOTE: always work with the basePath, when working on the filesystem, as the task may be called
		 * internally in yambas and therefore have another workdir than the specified basepath */
		final File basePath = this.project.getBasedir( );

		int[ ] fromVerArr = null;
		int[ ] toVerArr = null;

		if ( StringUtils.isNotBlank( this.fromVersion ) )
		{
			fromVerArr = VersionCompareHelper.parseNumbers( this.fromVersion );
		}
		if ( StringUtils.isNotBlank( this.toVersion ) )
		{
			toVerArr = VersionCompareHelper.parseNumbers( this.toVersion );
		}
		if ( fromVerArr == null && toVerArr == null )
		{
			/* return if both versions are not set/unparseable */
			throw new MojoExecutionException(
				"Error: You haven't set any version property (or not properly). " +
					"Please add either \"fromVersion\" or \"toVersion\" as parameter. " +
					"The value should be in the form of \"2.5.0\"" );
		}
		if ( fromVerArr == null )
		{ /* if from version is not set, set it to 0,0,0 */
			getLog( )
				.warn( "Warning: No or unparseable \"fromVersion\" property found. " +
					"Will fallback to value \"0.0.0\". Any available update until version " + intToStr( toVerArr ) +
					" will be executed." );
			fromVerArr = new int[ ] { 0, 0, 0 };
		}
		if ( toVerArr == null )
		{
			getLog( ).warn( "Warning: No or unparseable \"toVersion\" property found. " +
				"Will fallback to value \"999.999.999\" Any available update after version " + intToStr( fromVerArr ) +
				" will be executed." );
			/* if to version is not set, set it to 999,999,999 */
			toVerArr = new int[ ] { 999, 999, 999 };
		}
		getLog( ).info( "Updating from: " + intToStr( fromVerArr ) + " toVersionArray: " + intToStr( toVerArr ) );

		final ModuleUpdateManager updateManager = new ModuleUpdateManager( basePath, getOneModuleName( ), getLog( ) );
		if ( checkUpdate( fromVerArr, toVerArr, new int[ ] { 2, 5, 0 } ) )
		{
			printDashLine( );
			getLog( ).info( "Updating to 2.5.0" );
			updateManager.removeStaticClasses250( );
			updateManager.cleanJarsFor250( );
		}
		if ( checkUpdate( fromVerArr, toVerArr, new int[ ] { 3, 3, 0 } ) )
		{
			printDashLine( );
			getLog( ).info( "Updating to 3.3.0" );
			updateManager.cleanJarsFor330( );
			try
			{
				getLog( ).info( "Disable deprecated methods in hook classes" );
				updateManager.cleanHookClassesFor330( );
			}
			catch ( final Exception e )
			{
				getLog( ).info( "Error cleaning Hook Classes for version 3.3.0. Reason: " + e.toString( ) );
			}
		}

	}

	/**
	 * Prints out 80 dashes
	 */
	private void printDashLine( )
	{
		getLog( ).info( "-------------------------------------------------------------------------------" );
	}

	private static String intToStr( final int[ ] input )
	{
		final StringBuilder sb = new StringBuilder( 2 * input.length + 1 );
		for ( int i = 0; i < input.length; i++ )
		{
			sb.append( input[ i ] ).append( '.' );
		}
		return sb.toString( ).substring( 0, 2 * input.length - 1 );
	}

	private static boolean checkUpdate( final int[ ] fromVersionArr, final int[ ] toVersionArr,
		final int[ ] currentVersionCheck )
	{
		/* update if the version from which the update was started is lower than the current version to check the
		 * updates for */
		final boolean isCheckVerNewerThanFrom =
			VersionCompareHelper.isNewer( fromVersionArr[ 0 ], fromVersionArr[ 1 ], fromVersionArr[ 2 ], 0,
				currentVersionCheck[ 0 ], currentVersionCheck[ 1 ], currentVersionCheck[ 2 ], 0 );

		/* and if the version to check currently is equal or older than the version to which the update should be
		 * made */
		final boolean isTargetNewerOrEq =
			VersionCompareHelper.isNewerOrEqual( currentVersionCheck[ 0 ], currentVersionCheck[ 1 ],
				currentVersionCheck[ 2 ], 0, toVersionArr[ 0 ], toVersionArr[ 1 ], toVersionArr[ 2 ], 0 );

		return isCheckVerNewerThanFrom && isTargetNewerOrEq;
	}
}
