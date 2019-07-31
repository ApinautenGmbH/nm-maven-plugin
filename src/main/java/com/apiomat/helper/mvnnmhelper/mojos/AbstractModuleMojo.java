/* Copyright (c) 2011 - 2019 All Rights Reserved, http://www.apiomat.com/
 *
 * This source is property of apiomat.com. You are not allowed to use or distribute this code without a contract
 * explicitly giving you these permissions. Usage of this code includes but is not limited to running it on a server or
 * copying parts from it.
 *
 * Apinauten GmbH, Hainstrasse 4, 04109 Leipzig, Germany
 *
 * Feb 26, 2019
 * thum */
package com.apiomat.helper.mvnnmhelper.mojos;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.StringUtils;

/**
 * AbstractModuleMojo class for common module related fields and methods (like the moduleName or the project)
 *
 * @author thum
 */
public abstract class AbstractModuleMojo extends AbstractMojo
{
	/**
	 * The moduleName
	 */
	@Parameter( defaultValue = "${project.model.name}", property = "moduleName" )
	protected String moduleName;

	/**
	 * The {@link MavenProject}.
	 */
	@Parameter( defaultValue = "${project}", readonly = true, required = true )
	protected MavenProject project;

	/**
	 * Directory containing the source files that should be packaged into the JAR.
	 */
	@Parameter( defaultValue = "${project.build.sourceDirectory}", required = true )
	protected File sourceDirectory;

	/**
	 * Directory containing the base directory of the packages module
	 */
	@Parameter( defaultValue = "${project.basedir}", required = true )
	protected File baseDirectory;

	/**
	 * @return either the module name from property, from pom artifact name or the artifactId
	 */
	protected String getOneModuleName( )
	{
		String oneModuleName = this.moduleName;
		/* first check whether set from property or in pom */
		if ( StringUtils.isEmpty( this.moduleName ) )
		{
			/* second, fallback: try to load moduleName from sdk.properties file if exist */
			final File sdkPropsFile = new File( this.project.getBasedir( ), "sdk.properties" );
			if ( sdkPropsFile.exists( ) )
			{
				try
				{
					final Properties props = new Properties( );
					props.load( FileUtils.openInputStream( sdkPropsFile ) );
					oneModuleName = props.getProperty( "moduleName" );
					this.getLog( ).info( "Found moduleName over sdk.properties: " + oneModuleName );
				}
				catch ( final IOException e )
				{
					// e.printStackTrace( );
				}
			}
			if ( StringUtils.isEmpty( oneModuleName ) )
			{
				/* third, fallback: try to find on FS */
				final String lcArtifactId = this.project.getArtifactId( ).toLowerCase( );
				final String lcArtifactIdDotJava = ( lcArtifactId + ".java" ).toLowerCase( );
				if ( StringUtils.isEmpty( lcArtifactId ) == false )
				{
					final File nmModDir = new File(
						new File( new File( new File( this.sourceDirectory, "com" ), "apiomat" ), "nativemodule" ),
						lcArtifactId );
					if ( nmModDir.exists( ) )
					{
						/* iterate over classes and try to find something that looks like the module main class */
						final File[ ] filesInDir = nmModDir.listFiles( );
						for ( final File fileInDir : filesInDir )
						{
							if ( fileInDir.isDirectory( ) )
							{
								continue;
							}

							if ( fileInDir.getName( ).toLowerCase( ).equals( lcArtifactIdDotJava ) )
							{

								final String fName = fileInDir.getName( );
								oneModuleName = fName.substring( 0, fName.length( ) - ".java".length( ) );
								this.getLog( ).info( "Found moduleName over File: " + oneModuleName );
								break;
							}
						}
					}
				}
			}
			if ( StringUtils.isEmpty( oneModuleName ) )
			{
				/* no name in pom, no property specified, no sdk properties, no module main, very last option: try the
				 * artifactId (may misbehave, but better than nothing) */
				oneModuleName = this.project.getArtifactId( );
			}
		}
		return oneModuleName;
	}

	/**
	 * @return the source directory of the project
	 */
	protected File getSourceDirectory( )
	{
		return this.sourceDirectory;
	}
}
