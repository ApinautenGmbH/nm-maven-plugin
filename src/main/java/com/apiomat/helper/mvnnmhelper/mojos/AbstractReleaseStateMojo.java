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

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.StringUtils;
import org.json.JSONObject;

/**
 * Abstract goal to release/unrelease a module
 *
 * @author thum
 */
public abstract class AbstractReleaseStateMojo extends AbstractRequestMojo
{

	/**
	 * Name of the generated JAR.
	 */
	@Parameter( defaultValue = "${project.build.finalName}", readonly = true )
	private String finalName;

	@Override
	protected void executeRequest( ) throws MojoExecutionException, ClientProtocolException, IOException
	{
		final boolean isRelease = isRelease( );
		final JSONObject releaseObj = new JSONObject( );
		releaseObj.put( "releaseState", isRelease ? "RELEASED" : "UNRELEASED" );

		final Request request = Request.Put( getBaseUrl( ) )
			.addHeader( "Authorization", getUserAuthHeaderValue( ) )
			.bodyString( releaseObj.toString( ), ContentType.APPLICATION_JSON );
		if ( StringUtils.isNotBlank( this.system ) )
		{
			request.addHeader( "x-apiomat-system", this.system );
		}

		final Response response = request.execute( );
		final HttpResponse httpResponse = response.returnResponse( );
		final StatusLine status = httpResponse.getStatusLine( );
		if ( status.getStatusCode( ) >= 200 && status.getStatusCode( ) <= 299 )
		{
			final String responseMessage =
				"Successfully " + ( isRelease ? "released" : "unreleased" ) + " module " + getOneModuleName( );
			getLog( ).info( responseMessage );
		}
		else
		{
			final String message = StringUtils.isNotBlank( status.getReasonPhrase( ) ) ? status.getReasonPhrase( )
				: EntityUtils.toString( httpResponse.getEntity( ) );
			throw new MojoExecutionException( "Failed to set release state for module with status " +
				status.getStatusCode( ) + ":" + message );
		}
	}

	/**
	 * indicates whether this is a task to release a module or unrelease it
	 *
	 * @return true for release, false for unrelease
	 */
	protected abstract boolean isRelease( );

}
