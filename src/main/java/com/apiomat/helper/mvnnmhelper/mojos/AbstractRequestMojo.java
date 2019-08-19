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

import org.apache.http.client.ClientProtocolException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.*;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.StringUtils;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class for all goals that communicate directly with yambas
 *
 * @author thum
 */
public abstract class AbstractRequestMojo extends AbstractModuleMojo
{
	/**
	 * The host to the Yambas instance
	 */
	@Parameter( defaultValue = "http://localhost:8080", property = "host", required = true )
	protected String host;

	/**
	 * the used system
	 */
	@Parameter( defaultValue = "LIVE", property = "system", required = true )
	protected String system;

	/**
	 * the customerName
	 */
	@Parameter( defaultValue = "", property = "customerName" )
	protected String customerName;

	/**
	 * the customerEmail
	 */
	@Parameter( defaultValue = "", property = "customerEmail" )
	protected String customerEmail;

	/**
	 * the customerPassword
	 */
	@Parameter( property = "customerPassword", required = true )
	protected String customerPassword;


	/**
	 * The {@link MavenSession}.
	 */
	@Parameter( defaultValue = "${session}", readonly = true, required = true )
	protected MavenSession session;

	/**
	 * The {@link PluginDescriptor}.
	 */
	@Parameter( defaultValue = "${pluginDescriptor}", readonly = true, required = true )
	protected PluginDescriptor pluginDescriptor;

	/**
	 * The Maven BuildPluginManager component.
	 */
	@Component
	protected BuildPluginManager pluginManager;

	// Create a trust manager that does not validate certificate chains
	final TrustManager[ ] trustAllCerts = new TrustManager[ ] { new X509TrustManager( )
	{
		@Override
		public java.security.cert.X509Certificate[ ] getAcceptedIssuers( )
		{
			return null;
		}

		@Override
		public void checkClientTrusted( final X509Certificate[] certs, final String authType )
		{}

		@Override
		public void checkServerTrusted( final X509Certificate[] certs, final String authType )
		{}
	}
	};

	@Override
	public final void execute( ) throws MojoExecutionException // , MojoFailureException
	{
		/* if execution should be skipped then return directly */
		if ( this.nmSkip )
		{
			getLog( ).info( "Execution skipped" );
			return;
		}
		if ( this.host == null ||
			( StringUtils.isBlank( this.customerName ) && StringUtils.isBlank( this.customerEmail ) ) ||
			this.customerPassword == null )
		{
			throw new MojoExecutionException( "Not all attributes are set!" );
		}

		getLog( ).info( "Connecting to host '" + this.host + "' with customer '" + this.customerName + "' and system " +
			this.system );

		try
		{
			if ( overwriteSSLContext( ) )
			{
				getLog( ).debug( "Setting SSL context..." );

				// Install the all-trusting trust manager
				final SSLContext sc = SSLContext.getInstance( "SSL" );
				sc.init( null, this.trustAllCerts, new java.security.SecureRandom( ) );
				HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory( ) );
				// Create all-trusting host name verifier
				final HostnameVerifier allHostsValid = new HostnameVerifier( )
				{
					@Override
					public boolean verify( final String hostname, final SSLSession session )
					{
						return true;
					}
				};
				HttpsURLConnection.setDefaultHostnameVerifier( allHostsValid );
			}
			else
			{
				getLog( ).debug( "Using existing SSL context..." );
			}
			executeRequest( );
		}
		catch ( final Exception e )
		{
			e.printStackTrace( );
			throw new MojoExecutionException( e.getMessage( ), e );
		}
	}

	/**
	 * Execute the request and the main contents of the concrete task
	 *
	 * @throws MojoExecutionException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	protected abstract void executeRequest( ) throws MojoExecutionException, ClientProtocolException, IOException;

	/**
	 * Executes a goal in this plugin
	 *
	 * @param goal
	 * @throws MojoExecutionException
	 */
	protected void executeGoal( final String goal ) throws MojoExecutionException
	{
		try
		{
			@SuppressWarnings( "rawtypes" )
			final Map pluginCtx = getPluginContext( );

			final PluginDescriptor pluginDescriptor = ( PluginDescriptor ) pluginCtx.get( "pluginDescriptor" );
			final MojoDescriptor mojo = pluginDescriptor.getMojo( goal );
			if ( mojo == null )
			{
				throw new MojoExecutionException( "Could not find goal '" + goal + "' in plugin " ); // +
			}
			final Xpp3Dom mojoConfig = toXpp3Dom( mojo.getMojoConfiguration( ) );
			final Xpp3Dom existingConfig =
				toXpp3Dom( this.mojoExecution.getConfiguration( ), mojo.getParameterMap( ).keySet( ) );
			final Xpp3Dom mergedConfiguration =
				existingConfig != null ? Xpp3DomUtils.mergeXpp3Dom( existingConfig, mojoConfig )
					: mojoConfig;
			final MojoExecution exec = new MojoExecution( mojo, this.mojoExecution.getExecutionId( ) );
			exec.setConfiguration( mergedConfiguration );
			this.pluginManager.executeMojo( this.session, exec );
		}
		catch ( final MojoExecutionException | MojoFailureException
			| PluginConfigurationException | PluginManagerException e )
		{
			throw new MojoExecutionException(
				"Cloud not execute goal, please execute goal " + goal + " by yourself", e );
		}
	}

	/**
	 * Clone existing Xpp3Dom to new Xpp3Dom but only that attributes and values that are in parameterNames list
	 *
	 * @param config
	 * @param parameterNames
	 * @return
	 */
	private static Xpp3Dom toXpp3Dom( final Xpp3Dom config,
		final Set<String> parameterNames )
	{
		Xpp3Dom result = null;
		if ( config != null )
		{
			final int childCount = config.getChildCount( );
			result = new Xpp3Dom( config.getName( ) );
			result.setValue( config.getValue( ) );

			final String[] attributeNames = config.getAttributeNames( );
			for ( final String attributeName : attributeNames )
			{
				if ( parameterNames.contains( attributeName ) )
				{
					result.setAttribute( attributeName, config.getAttribute( attributeName ) );
				}
			}

			for ( int i = 0; i < childCount; i++ )
			{
				final Xpp3Dom child = config.getChild( i );
				if ( parameterNames.contains( child.getName( ) ) )
				{
					result.addChild( new Xpp3Dom( child ) );
				}
			}

		}
		return result;
	}

	private static Xpp3Dom toXpp3Dom( final PlexusConfiguration config )
	{
		final Xpp3Dom result = new Xpp3Dom( config.getName( ) );
		result.setValue( config.getValue( null ) );
		for ( final String name : config.getAttributeNames( ) )
		{
			result.setAttribute( name, config.getAttribute( name ) );
		}
		for ( final PlexusConfiguration child : config.getChildren( ) )
		{
			result.addChild( toXpp3Dom( child ) );
		}
		return result;
	}

	/**
	 * builds something like: http://localhost/yambas/rest/modules/moduleName/v/1.0.0
	 *
	 * @return the base-url to the host with path to this module
	 * @throws MojoExecutionException
	 */
	protected String getBaseUrl( ) throws MojoExecutionException
	{

		final String version = "1".equals( this.project.getVersion( ) ) ? "1.0.0" : this.project.getVersion( );
		final StringBuilder sb = new StringBuilder( );
		sb.append( this.host ).append( "/yambas/rest/modules/" );
		sb.append( getOneModuleName( ) );
		sb.append( "/v/" ).append( version );
		return sb.toString( );
	}

	/**
	 *
	 * @return the value of the authorization header (including the "Basic " prefix)
	 * @throws MojoExecutionException
	 */
	protected String getUserAuthHeaderValue( ) throws MojoExecutionException
	{
		if ( StringUtils.isEmpty( this.customerName ) && StringUtils.isEmpty( this.customerEmail ) )
		{
			throw new MojoExecutionException( "Neither customerName nor customerEmail property set. Failing build." );
		}
		if ( StringUtils.isEmpty( this.customerPassword ) )
		{
			throw new MojoExecutionException( "No customerPassword property set. Failing build." );
		}
		final String userIdentifier = StringUtils.isEmpty( this.customerName ) ? this.customerEmail : this.customerName;
		final String userAuth = userIdentifier + ":" + this.customerPassword;
		final String encoding = new String( Base64.getEncoder( ).encode( userAuth.getBytes( ) ) );
		return "Basic " + encoding;
	}

	/**
	 *
	 * @return false if a keyStore or the property doNotOverwriteSSLContext is set, true otherwise
	 */
	protected static boolean overwriteSSLContext( )
	{
		if ( System.getProperty( "javax.net.ssl.keyStore" ) != null )
		{
			return false;
		}
		if ( Boolean.getBoolean( "doNotOverwriteSSLContext" ) )
		{
			return false;
		}

		return true;
	}
}
