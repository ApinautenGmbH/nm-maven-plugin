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

import java.io.IOException;

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
