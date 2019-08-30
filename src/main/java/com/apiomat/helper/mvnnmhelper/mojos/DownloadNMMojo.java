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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Goal to download the native module.
 *
 * @author thum
 */
@Mojo( name = "download", defaultPhase = LifecyclePhase.NONE )
public class DownloadNMMojo extends AbstractRequestMojo
{
	/**
	 * Property to indicate whether the generated parts and the previously uploaded jar should be merged, or only the
	 * generated parts should be returned
	 */
	@Parameter( defaultValue = "true", property = "merge" )
	protected String merge;

	/**
	 * Property to indicate whether the download should contain an eclipse project
	 */
	@Parameter( defaultValue = "true", property = "eclipse" )
	protected String eclipse;

	/**
	 * Property whether to overwrite the local hook files with the remote contents
	 */
	@Parameter( defaultValue = "false", property = "overwriteHooks" )
	protected String overwriteHooks;

	@Override
	public void executeRequest( ) throws MojoExecutionException, ClientProtocolException, IOException
	{
		getLog( ).info( "Downloading updated native module" );
		final File destinationFile = new File( this.project.getBasedir( ), "nm.zip" );
		final Response response =
			Request.Get( buildHostUrl( ) ).addHeader( "Authorization", getUserAuthHeaderValue( ) ).execute( );
		response.saveContent( destinationFile );

		final File tmpDir = new File( this.project.getBasedir( ), "tmp" );
		FileUtils.forceMkdir( tmpDir );
		copyFilesToTemp( this.project.getBasedir( ), tmpDir, Boolean.parseBoolean( this.overwriteHooks ) );
		unzipFile( destinationFile, this.project.getBasedir( ) );
		copyFilesFromTemp( this.project.getBasedir( ), tmpDir );
		FileUtils.deleteQuietly( tmpDir );
		FileUtils.deleteQuietly( destinationFile );

	}

	/**
	 * Copies all existing module files from baseDir to the tmpDir
	 *
	 * @param baseDir
	 * @param tmpDir
	 * @param overwriteHooks
	 * @throws IOException
	 */
	protected static void copyFilesToTemp( final File baseDir, final File tmpDir, final boolean overwriteHooks )
		throws IOException
	{
		final File sdkProps = new File( baseDir, "sdk.properties" );
		if ( sdkProps.exists( ) )
		{
			FileUtils.copyFileToDirectory( sdkProps, tmpDir );
		}

		final File readmeMd = new File( baseDir, "readme.md" );
		if ( readmeMd.exists( ) )
		{
			FileUtils.copyFileToDirectory( readmeMd, tmpDir );
		}
		final File gitignore = new File( baseDir, ".gitignore" );
		if ( gitignore.exists( ) )
		{
			FileUtils.copyFileToDirectory( gitignore, tmpDir );
		}
		final List<IOFileFilter> fileFilter = new ArrayList<>( );
		if ( overwriteHooks == false )
		{
			fileFilter.add( FileFilterUtils.suffixFileFilter( "HooksTransient.java" ) );
			fileFilter.add( FileFilterUtils.suffixFileFilter( "HooksNonTransient.java" ) );
			fileFilter.add( FileFilterUtils.suffixFileFilter( "Hooks.java" ) );
		}
		fileFilter.add( FileFilterUtils.nameFileFilter( "RestClass.java" ) );
		final IOFileFilter allHooksFilter = FileFilterUtils.or( fileFilter.stream( ).toArray( IOFileFilter[ ]::new ) );
		final IOFileFilter allHooksFiles = FileFilterUtils.and( FileFileFilter.FILE, allHooksFilter );

		/* Create a filter for either directories or hook files */
		final FileFilter filter = FileFilterUtils.or( DirectoryFileFilter.DIRECTORY, allHooksFiles );

		FileUtils.copyDirectory( new File( baseDir, "src" ), tmpDir, filter );
	}

	/**
	 * Copies all existing module files from tmpDir to baseDir
	 *
	 * @param baseDir
	 * @param tmpDir
	 * @throws IOException
	 */
	protected static void copyFilesFromTemp( final File baseDir, final File tmpDir ) throws IOException
	{
		final File sdkProps = new File( tmpDir, "sdk.properties" );
		if ( sdkProps.exists( ) )
		{
			FileUtils.copyFileToDirectory( sdkProps, baseDir );
		}

		final File readmeMd = new File( tmpDir, "readme.md" );
		if ( readmeMd.exists( ) )
		{
			FileUtils.copyFileToDirectory( readmeMd, baseDir );
		}
		final File gitignore = new File( tmpDir, ".gitignore" );
		if ( gitignore.exists( ) )
		{
			FileUtils.copyFileToDirectory( gitignore, baseDir );
		}
		final List<IOFileFilter> fileFilter = new ArrayList<>( );
		fileFilter.add( FileFilterUtils.suffixFileFilter( "HooksTransient.java" ) );
		fileFilter.add( FileFilterUtils.suffixFileFilter( "HooksNonTransient.java" ) );
		fileFilter.add( FileFilterUtils.suffixFileFilter( "Hooks.java" ) );
		fileFilter.add( FileFilterUtils.nameFileFilter( "RestClass.java" ) );
		final IOFileFilter allHooksFilter = FileFilterUtils.or( fileFilter.stream( ).toArray( IOFileFilter[ ]::new ) );
		final IOFileFilter allHooksFiles = FileFilterUtils.and( FileFileFilter.FILE, allHooksFilter );
		// Create a filter for either directories or hook files
		final FileFilter filter = FileFilterUtils.or( DirectoryFileFilter.DIRECTORY, allHooksFiles );

		FileUtils.copyDirectory( tmpDir, new File( baseDir, "src" ), filter );
	}

	private URI buildHostUrl( ) throws MojoExecutionException
	{
		try
		{
			final StringBuilder sb = new StringBuilder( );
			sb.append( getBaseUrl( ) ).append( "/asset" );

			final URIBuilder bldr = new URIBuilder( sb.toString( ) );
			if ( StringUtils.isNotBlank( this.system ) )
			{
				bldr.addParameter( "usedSystem", this.system );
			}
			if ( StringUtils.isNotBlank( this.merge ) )
			{
				bldr.addParameter( "merge", this.merge );
			}
			if ( StringUtils.isNotBlank( this.eclipse ) )
			{
				bldr.addParameter( "eclipse", this.eclipse );
			}
			return bldr.build( );
		}
		catch ( final URISyntaxException e )
		{
			throw new MojoExecutionException( "Failed to create the Url", e );
		}
	}

	/**
	 * unzips a given file to the given output directory
	 *
	 * @param file
	 *        the zip file (or jar) to unzip
	 * @param outputDir
	 *        the output-directory to unzip the files to
	 * @throws ZipException
	 * @throws IOException
	 */
	protected static void unzipFile( final File file, final File outputDir ) throws ZipException, IOException
	{
		try (final ZipFile zipFile = new ZipFile( file ))
		{
			final Enumeration<? extends ZipEntry> entries = zipFile.entries( );
			while ( entries.hasMoreElements( ) )
			{
				final ZipEntry entry = entries.nextElement( );
				final File entryDestination = new File( outputDir, entry.getName( ) );
				if ( entry.isDirectory( ) )
				{
					entryDestination.mkdirs( );
				}
				else
				{
					entryDestination.getParentFile( ).mkdirs( );
					try (final InputStream in = zipFile.getInputStream( entry );
						final OutputStream out = new FileOutputStream( entryDestination ))
					{
						IOUtils.copy( in, out );
					}
				}
			}
		}
	}
}
