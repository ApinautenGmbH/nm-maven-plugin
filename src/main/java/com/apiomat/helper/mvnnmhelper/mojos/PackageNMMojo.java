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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.jar.AbstractJarMojo;
import org.apache.maven.plugins.jar.JarMojo;
import org.apache.maven.shared.utils.StringUtils;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Goal to package the native module as uploadable jar
 *
 * @author thum
 */
@Mojo( name = "package", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true,
	requiresDependencyResolution = ResolutionScope.RUNTIME )
public class PackageNMMojo extends JarMojo
{
	private static final String[ ] DEFAULT_EXCLUDES = new String[ ] { "**/package.html" };
	private static final String[ ] DEFAULT_INCLUDES = new String[ ] { "**/**" };
	private String[ ] myIncludes;

	/**
	 * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents
	 * is being packaged into the JAR.
	 */
	private String[ ] myExcludes;
	/**
	 * Directory containing the generated JAR.
	 */
	private File myOutputDirectory;

	/**
	 * Name of the generated JAR.
	 */
	private String myFinalName;
	/**
	 * Directory containing the source files that should be packaged into the JAR.
	 */
	@Parameter( defaultValue = "${project.build.sourceDirectory}", required = true )
	private File sourceDirectory;

	@Parameter( defaultValue = "false", property = "nmSkip", required = false )
	protected boolean nmSkip;

	/**
	 * The Jar archiver.
	 */
	private JarArchiver myJarArchiver;

	/**
	 * The {@link MavenSession}.
	 */
	private MavenSession mySession;

	/**
	 * ...
	 */
	private boolean myForceCreation;

	/**
	 * The archive configuration to use. See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
	 * Archiver Reference</a>.
	 */
	private MavenArchiveConfiguration myArchive;

	@Override
	public void execute( ) throws MojoExecutionException
	{
		/* if execution should be skipped then return directly */
		if ( this.nmSkip )
		{
			getLog( ).debug( "Execution skipped" );
			return;
		}
		setValuesFromParent( );
		super.execute( );
	}

	/**
	 * a hack to get the field values from the abstract class, as we need the fields here to override the createArchive
	 * method, but they're private. when we just add them as variables with annotations, then the fields of the parent
	 * won't be injected.
	 * Note: this logic may fail (due to the reflection) when updating to a newer version of the jar plugin
	 */
	private void setValuesFromParent( )
	{
		this.myOutputDirectory = ( File ) getFieldValue( "outputDirectory" );
		this.myFinalName = ( String ) getFieldValue( "finalName" );
		this.myJarArchiver = ( JarArchiver ) getFieldValue( "jarArchiver" );
		this.mySession = ( MavenSession ) getFieldValue( "session" );
		// this.myClassifier = ( String ) getFieldValue( "classifier" );
		this.myForceCreation = ( boolean ) getFieldValue( "forceCreation" );
		this.myArchive = ( MavenArchiveConfiguration ) getFieldValue( "archive" );
		this.myIncludes = ( String[ ] ) getFieldValue( "includes" );
		this.myExcludes = ( String[ ] ) getFieldValue( "excludes" );
	}

	private Object getFieldValue( final String fieldName )
	{
		try
		{
			final Field f = AbstractJarMojo.class.getDeclaredField( fieldName );
			f.setAccessible( true );
			return f.get( this );
		}
		catch ( final NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e )
		{
			e.printStackTrace( );
		}
		return null;
	}

	/**
	 * @return the source directory of the project
	 */
	protected File getSourceDirectory( )
	{
		return this.sourceDirectory;
	}

	/**
	 * Generates the JAR.
	 *
	 * @return The instance of File for the created archive file.
	 * @throws MojoExecutionException in case of an error.
	 */
	@Override
	public File createArchive( ) throws MojoExecutionException
	{
		final File jarFile = getJarFile( this.myOutputDirectory, this.myFinalName, getClassifier( ) );

		final MavenArchiver archiver = new MavenArchiver( );

		archiver.setArchiver( this.myJarArchiver );

		archiver.setOutputFile( jarFile );

		this.myArchive.setForced( this.myForceCreation );

		try
		{
			final File contentDirectory = getClassesDirectory( );
			final File sourceDirectory = getSourceDirectory( );
			final File baseDirectory = sourceDirectory.getParentFile( );
			final File libDir = new File( baseDirectory, "lib" );

			if ( contentDirectory.exists( ) == false && sourceDirectory.exists( ) == false &&
				getProject( ).getBasedir( ).exists( ) == false && libDir.exists( ) == false )
			{
				getLog( ).warn( "JAR will be empty - no content was marked for inclusion!" );
			}
			else
			{
				archiver.getArchiver( ).addDirectory( contentDirectory, getIncludes( ), getExcludes( ) );
				/* also add to jar: *.java, *.png, *.svg, and all files under META-INF in src directory */
				final DefaultFileSet sourcefs = new DefaultFileSet( baseDirectory )
					.includeExclude( getSourceIncludes( ), getSourceExcludes( ) );

				final DefaultFileSet resourceFs = new DefaultFileSet( sourceDirectory )
					.includeExclude( getSourceResourceIncludes( ), getSourceResourceExcludes( ) );
				archiver.getArchiver( ).addFileSet( resourceFs );

				archiver.getArchiver( ).addFileSet( sourcefs );
				/* also add the pom.xml */
				archiver.getArchiver( ).addDirectory( baseDirectory, new String[] { "pom.xml" },
					new String[ ] { } );

				/* and the libs */
				archiver.getArchiver( ).addDirectory( libDir, DEFAULT_INCLUDES, new String[ ] { } );
			}

			archiver.createArchive( this.mySession, getProject( ), this.myArchive );

			return jarFile;
		}
		catch ( final Exception e )
		{
			throw new MojoExecutionException( "Error assembling JAR", e );
		}
	}

	/* (non-Javadoc)
	 *
	 * @see org.apache.maven.plugins.jar.JarMojo#getClassifier() */
	@Override
	protected String getClassifier( )
	{
		final String parentClassifier = super.getClassifier( );
		return ( StringUtils.isNotEmpty( parentClassifier ) ? parentClassifier : "" ) + "NM";
	}

	/**
	 * @return the source includes for bundling
	 */
	private static String[ ] getSourceIncludes( )
	{
		return new String[ ] { "src/**/*.java", "src/**/META-INF/**" };
	}

	/**
	 *
	 * @return the source excludes for bundling
	 */
	private static String[ ] getSourceExcludes( )
	{
		return new String[ ] { };
	}

	private static String[ ] getSourceResourceIncludes( )
	{
		return new String[ ] { "**/META-INF/**", "**/*.png", "**/*.svg" };
	}

	private static String[ ] getSourceResourceExcludes( )
	{
		return new String[ ] { };
	}

	private String[ ] getIncludes( )
	{
		if ( this.myIncludes != null && this.myIncludes.length > 0 )
		{
			return this.myIncludes;
		}
		return DEFAULT_INCLUDES;
	}

	private String[ ] getExcludes( )
	{
		if ( this.myExcludes != null && this.myExcludes.length > 0 )
		{
			return this.myExcludes;
		}
		return DEFAULT_EXCLUDES;
	}
}
