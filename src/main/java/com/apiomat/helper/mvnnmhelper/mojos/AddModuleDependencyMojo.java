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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.net.URI;

/**
 * Goal to upload a native module artifact
 *
 * @author thum
 */
@Mojo( name = "addDependency", defaultPhase = LifecyclePhase.NONE )
public class AddModuleDependencyMojo extends AbstractRequestMojo
{
	/**
	 * whether to update or to overwrite the changes on yambas instance
	 */
	@Parameter( defaultValue = "false", property = "noDownload" )
	protected boolean noDownload;

	/**
	 * whether to update or to overwrite the changes on yambas instance
	 */
	@Parameter( property = "usedModuleName", required = true )
	protected String usedModuleName;
	/**
	 * whether to update or to overwrite the changes on yambas instance
	 */
	@Parameter( property = "usedModuleVersion", required = true )
	protected String usedModuleVersion;

	@Override
	public void executeRequest( ) throws MojoExecutionException, IOException
	{
		getLog( ).info( "Adding dependend module " );

		if ( StringUtils.isBlank( this.usedModuleName ) || StringUtils.isBlank( this.usedModuleVersion ) )
		{
			throw new MojoExecutionException( "usedModuleName or usedModuleVersion was not set. Please set both." );
		}
		final NameValuePair moduleNameParam = new BasicNameValuePair( "parentModuleName", this.usedModuleName );
		final NameValuePair moduleVersionParam =
			new BasicNameValuePair( "parentModuleVersion", this.usedModuleVersion );

		final Response response =
			Request.Post( buildHostUrl( ) )
				.addHeader( "X-apiomat-system", this.system )
				.addHeader( "Authorization", getUserAuthHeaderValue( ) )
				.bodyForm( moduleNameParam, moduleVersionParam )
				.execute( );
		final HttpResponse httpResponse = response.returnResponse( );
		final StatusLine status = httpResponse.getStatusLine( );
		if ( status.getStatusCode( ) >= 200 && status.getStatusCode( ) <= 299 )
		{
			getLog( ).info( "Successfully added dependency" );
		}
		else
		{
			final String message = StringUtils.isNotBlank( status.getReasonPhrase( ) ) ? status.getReasonPhrase( )
				: EntityUtils.toString( httpResponse.getEntity( ) );
			throw new MojoExecutionException( "Failed to add dependency for module with status " +
				status.getStatusCode( ) + ":" + message );
		}
		if ( this.noDownload == false )
		{
			executeGoal( "download" );
		}
	}

	private URI buildHostUrl( )
	{
		try
		{
			final StringBuilder sb = new StringBuilder( );
			sb.append( getBaseUrl( ) ).append( "/parent" );
			return new URI( sb.toString( ) );
		}
		catch ( final Exception e )
		{
			getLog( ).error( "Error building url", e );
		}
		return null;
	}
}
