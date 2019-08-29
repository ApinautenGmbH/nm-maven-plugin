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

import org.apache.http.client.utils.URIBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Goal to upload a native module artifact
 *
 * @author thum
 */
@Mojo( name = "upload", defaultPhase = LifecyclePhase.DEPLOY )
public class UploadNMMojo extends AbstractRequestMojo
{
	/**
	 * the path where the jar should be stored (build directory)
	 */
	@Parameter( defaultValue = "${project.build.directory}", property = "moduleJarPath", required = true )
	protected File moduleJarPath;

	/**
	 * whether to update or to overwrite the changes on yambas instance
	 */
	@Parameter( defaultValue = "overwrite", property = "update", required = true )
	protected String update;

	/**
	 * whether to update or to overwrite the changes on yambas instance
	 */
	@Parameter( defaultValue = "false", property = "noDownload" )
	protected boolean noDownload;

	/**
	 * Directory containing the generated JAR.
	 */
	@Parameter( defaultValue = "${project.build.directory}", required = true )
	private File outputDirectory;

	/**
	 * Name of the generated JAR.
	 */
	@Parameter( defaultValue = "${project.build.finalName}" )
	private String finalName;

	@Override
	public void executeRequest( ) throws MojoExecutionException, IOException
	{

		final URI hostUrl = buildHostUrl( );
		final URL url = hostUrl.toURL( );

		final HttpURLConnection connection = ( HttpURLConnection ) url.openConnection( );
		connection.setDoOutput( true );
		connection.setRequestProperty( "Content-Type", "application/octet-stream" );
		connection.setRequestMethod( "POST" );
		if ( this.system != null )
		{
			connection.addRequestProperty( "X-apiomat-system", this.system );
		}
		// Allow Outputs
		connection.setDoOutput( true );

		// Don't use a cached copy.
		connection.setUseCaches( false );

		connection.setRequestProperty( "Authorization", getUserAuthHeaderValue( ) );

		final File jarFile = getJarFile( this.moduleJarPath, this.finalName, "NM" );
		if ( jarFile.exists( ) == false )
		{
			throw new MojoExecutionException( "Can't find module jar in " + jarFile.getAbsolutePath( ) );
		}
		final OutputStream writer = connection.getOutputStream( );
		final FileInputStream fis = new FileInputStream( jarFile );
		final byte[] buf = new byte[ 1024 ];
		System.out.print( "Writing bytes " );
		for ( int c = fis.read( buf ); c != -1; c = fis.read( buf ) )
		{
			writer.write( buf, 0, c );
			System.out.print( "." );
		}
		writer.flush( );
		writer.close( );
		fis.close( );
		System.out.println( "" );
		final int responseCode = connection.getResponseCode( );
		if ( responseCode != HttpURLConnection.HTTP_CREATED )
		{
			try (final InputStream is =
				( 200 <= responseCode && responseCode <= 299 ) ? connection.getInputStream( )
					: connection.getErrorStream( ))
			{
				String reason = "";
				if ( is != null )
				{
					final StringBuilder inputStringBuilder = new StringBuilder( );
					final BufferedReader bufferedReader =
						new BufferedReader( new InputStreamReader( is, "UTF-8" ) );
					String line = bufferedReader.readLine( );
					while ( line != null )
					{
						inputStringBuilder.append( line );
						inputStringBuilder.append( '\n' );
						line = bufferedReader.readLine( );
					}
					reason = inputStringBuilder.toString( );
					bufferedReader.close( );
				}
				throw new MojoExecutionException(
					"Return code did not match 201: " + connection.getResponseMessage( ) + "(" +
						connection.getResponseCode( ) + ") Reason: " + reason );
			}
		}
		else if ( this.noDownload == false )
		{
			executeGoal( "download" );
		}
	}

	private URI buildHostUrl( )
	{
		try
		{
			final StringBuilder sb = new StringBuilder( );
			sb.append( this.host ).append( "/yambas/rest/modules/asset" );
			final URIBuilder bldr = new URIBuilder( sb.toString( ) );
			if ( StringUtils.isNotBlank( this.system ) )
			{
				bldr.addParameter( "usedSystem", this.system );
			}
			if ( StringUtils.isNotBlank( this.update ) )
			{
				bldr.addParameter( "update", this.update );
			}
			return bldr.build( );
		}
		catch ( final Exception e )
		{
			getLog( ).error( "Error building url", e );
		}
		return null;
	}

	/**
	 * Returns the Jar file to generate, based on an optional classifier.
	 *
	 * @param basedir         the output directory
	 * @param resultFinalName the name of the ear file
	 * @param classifier      an optional classifier
	 * @return the file to generate
	 */
	protected static File getJarFile( final File basedir, final String resultFinalName, final String classifier )
	{
		if ( basedir == null )
		{
			throw new IllegalArgumentException( "basedir is not allowed to be null" );
		}
		if ( resultFinalName == null )
		{
			throw new IllegalArgumentException( "finalName is not allowed to be null" );
		}

		final StringBuilder fileName = new StringBuilder( resultFinalName );
		fileName.append( "-" ).append( classifier );
		fileName.append( ".jar" );

		return new File( basedir, fileName.toString( ) );
	}
}
