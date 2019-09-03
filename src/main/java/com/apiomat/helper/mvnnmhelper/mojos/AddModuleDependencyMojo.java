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
