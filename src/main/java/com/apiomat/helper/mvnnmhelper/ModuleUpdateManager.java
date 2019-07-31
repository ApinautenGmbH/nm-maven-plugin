/* Copyright (c) 2011 - 2017 All Rights Reserved, http://www.apiomat.com/
 *
 * This source is property of apiomat.com. You are not allowed to use or distribute this code without a contract
 * explicitly giving you these permissions. Usage of this code includes but is not limited to running it on a server or
 * copying parts from it.
 *
 * Apinauten GmbH, Hainstrasse 4, 04109 Leipzig, Germany
 *
 * Mar 27, 2017
 * thum */
package com.apiomat.helper.mvnnmhelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Handles all Module updates
 *
 * @author thum
 */
@SuppressWarnings( { "PMD.SystemPrintln", "PMD.AvoidPrintStackTrace" } )
public class ModuleUpdateManager
{
	/* AOM-5650 matches all beforeGet-Methods with Override annotation except the ones with String as type of the first
	 * parameter */
	private final static Pattern PTTRN_OLD_BEFORE_GET =
		Pattern.compile( "(@Override[\\s]+public void beforeGet\\()(?![\\s]+(String|java.lang.String)[\\s]+)" );

	private final File basePath;
	private final String oneModuleName;
	private final Log log;

	final List<Dependency> dependencies330 = new LinkedList<>( );
	final String[ ] jarsToRemove330 =
		new String[ ] { "anttask-1.0.0.jar", "jersey-core-1.19.jar", "jsr311-api-1.1.1.jar",
			"swagger-annotations-1.5.12.jar", "validation-api-2.0.1.Final.jar" };

	/**
	 *
	 * @param basePath the basePath to work on
	 * @param oneModuleName either the module name from property, from pom artifact name or the artifactId
	 * @param log the log instance
	 */
	public ModuleUpdateManager( final File basePath, final String oneModuleName, final Log log )
	{
		this.basePath = Objects.requireNonNull( basePath );
		this.log = Objects.requireNonNull( log );
		this.oneModuleName = oneModuleName;

		this.dependencies330.add( createProvidedDependency( "com.sun.jersey", "jersey-core", "1.19" ) );
		this.dependencies330.add( createProvidedDependency( "javax.ws.rs", "jsr311-api", "1.1.1" ) );
		this.dependencies330.add( createProvidedDependency( "io.swagger", "swagger-annotations", "1.5.12" ) );
		this.dependencies330.add( createProvidedDependency( "javax.validation", "validation-api", "2.0.1.Final" ) );
	}

	/**
	 * Remove the existing static classes, as they're now packaged into a jar
	 * <b>Since</b> 2.5.0
	 *
	 */
	public void removeStaticClasses250( )
	{
		final String[ ] filesToDelete25 = new String[ ] { "AbstractClientDataModel.java",
			"AbstractRestResource.java", "AuthState.java", "CustomerRole.java", "DynamicAttribute.java",
			"EmbeddedDocument.java",
			"IApplicationConfigProxy.java", "IModel.java", "IModelHooks.java", "IModelHooksCommon.java",
			"IModelHooksNonTransient.java", "IModelHooksTransient.java", "IModelMethods.java", "IModule.java",
			"IResourceMethods.java", "IStaticMethods.java", "Mandatory.java", "Model.java", "Module.java",
			"NativeModuleConfig.java", "Request.java", "SecurityPermission.java", "StaticData.java", "UserRole.java",
			"interfaces" + File.separator + "dmap" + File.separator + "ADistributedMapHandler.java",
			"interfaces" + File.separator + "dmap" + File.separator + "IDistributedMap.java",
			"interfaces" + File.separator + "dmap" + File.separator + "IDistributedMapEntryEvent.java",
			"interfaces" + File.separator + "dmap" + File.separator + "IDistributedMapHandler.java",
			"interfaces" + File.separator + "dmap" + File.separator + "IDistributedMapListener.java",
			"interfaces" + File.separator + "dmap" + File.separator + "package-info.java" };
		final File nmDir =
			new File( this.basePath,
				"src" + File.separator + "com" + File.separator + "apiomat" + File.separator + "nativemodule" );
		/* delete the static file classes */
		deleteFiles( this.log, nmDir, filesToDelete25 );

		/* remove the remaining directories */
		final File dmapDir = new File( nmDir, "interfaces" + File.separator + "dmap" );
		if ( dmapDir.exists( ) && dmapDir.list( ).length == 0 )
		{
			this.log.info( "Deleting directory " + dmapDir.getAbsolutePath( ) + " as its empty" );
			dmapDir.delete( );

			final File interfacesDir = new File( nmDir, "interfaces" );
			if ( interfacesDir.exists( ) && interfacesDir.list( ).length == 0 )
			{
				this.log.info( "Deleting directory " + interfacesDir.getAbsolutePath( ) + " as its empty" );
				interfacesDir.delete( );
			}
		}
	}

	/**
	 * Replaces the jars which had been changed for 2.5.0, updates the .classpath and pom.xml
	 * <b>Since</b> 2.5.0
	 *
	 */
	public void cleanJarsFor250( )
	{
		final File jarDir = new File( this.basePath, "lib" );
		final String[ ] jarsToRemove =
			new String[ ] { "jersey-core-1.16.jar", "swagger-annotations-1.5.10.jar", "anttask.jar" };
		final String[ ] jarsToAdd = new String[ ] { "swagger-annotations-1.5.12.jar", "jersey-core-1.19.jar",
			"jsr311-api-1.1.1.jar", "nativemodule-base.jar" };
		deleteFiles( this.log, jarDir, jarsToRemove );
		updateClassPath( jarsToRemove, jarsToAdd );
		updatePomXml250( jarsToRemove, jarsToAdd );
	}

	/**
	 * Replaces the jars which had been changed for 3.3.0, updates the .classpath and pom.xml
	 * <b>Since</b> 3.3.0
	 *
	 * @throws MojoExecutionException
	 *
	 */
	public void cleanJarsFor330( ) throws MojoExecutionException
	{
		final File libDir = new File( this.basePath, "lib" );
		deleteFiles( this.log, libDir, this.jarsToRemove330 );
		updateClassPath( this.jarsToRemove330, null );
		final List<File> usedLibs = getUsedModuleLibs( this.log, libDir );
		try
		{
			/* build up the dependency list */
			final Map<File, File> oldNewFiles =
				createUpdated330PomFile( this.dependencies330, this.jarsToRemove330, usedLibs );
			for ( final Entry<File, File> oldNewEntry : oldNewFiles.entrySet( ) )
			{
				if ( oldNewEntry.getKey( ).exists( ) )
				{
					if ( oldNewEntry.getValue( ).exists( ) )
					{
						this.log.info( "Removing file " + oldNewEntry.getKey( ).getName( ) + ", as new library " +
							oldNewEntry.getValue( ).getName( ) + " already exists." );
						FileUtils.deleteQuietly( oldNewEntry.getKey( ) );
					}
					else
					{
						this.log.info( "Rename file " + oldNewEntry.getKey( ).getName( ) + " to " +
							oldNewEntry.getValue( ).getName( ) );
						FileUtils.moveFile( oldNewEntry.getKey( ), oldNewEntry.getValue( ) );
					}
				}
			}
		}
		catch ( final Exception e )
		{
			throw new MojoExecutionException( "Could not create updated pom.xml file ", e );
		}
	}

	/**
	 * Cleans the Hook methods from deprecated code
	 *
	 * @throws IOException
	 */
	public void cleanHookClassesFor330( ) throws IOException
	{
		final File nmDir = new File( this.basePath, "src" + File.separator + "com" + File.separator + "apiomat" +
			File.separator + "nativemodule" );

		final Collection<File> listFiles = FileUtils.listFiles( nmDir, new String[ ] { "java" }, true );
		for ( final File file : listFiles )
		{
			if ( file.getName( ).contains( "HooksNonTransient" ) )
			{
				final String content = FileUtils.readFileToString( file, Charset.forName( "UTF-8" ) );
				final String newContent = checkAndRefixBeforeGetBackwardCompatibility( content );

				if ( content.equals( newContent ) == false )
				{
					FileUtils.writeStringToFile( file, newContent, Charset.forName( "UTF-8" ) );
				}
			}
		}
	}

	/** Fixes the deprecated beforeGet hook method. This code was taken from Yambas RegisterMetaModelTask */
	private static String checkAndRefixBeforeGetBackwardCompatibility( final String classContent )
	{
		if ( StringUtils.isBlank( classContent ) )
		{
			return classContent;
		}
		final Matcher beforeGetMatcher = PTTRN_OLD_BEFORE_GET.matcher( classContent );
		final boolean contNoOverride = beforeGetMatcher.find( );
		String returnString = classContent;
		if ( contNoOverride == false )
		{
			return classContent;
		}
		/* replace it */
		if ( contNoOverride )
		{
			returnString = beforeGetMatcher.replaceAll( beforeGetMatcher.group( ).replaceAll( "@Override",
				"/** Removed deprecated beforeGet from overridden hook methods since Yambas 3.3.\n    " +
					"Use beforeGet( String id, com.apiomat.nativemodule.Request r ) */" ) );
		}

		return returnString;
	}

	private static Dependency createProvidedDependency( final String groupId, final String artifactId,
		final String version )
	{
		final Dependency dep = new Dependency( );
		dep.setGroupId( groupId );
		dep.setArtifactId( artifactId );
		dep.setVersion( version );
		dep.setScope( "provided" );
		return dep;
	}

	private static List<File> getUsedModuleLibs( final Log log, final File baseDir )
	{
		final List<File> usedLibFiles = new LinkedList<>( );
		if ( baseDir.exists( ) == false )
		{
			log.error( "Directory " + baseDir.getAbsolutePath( ) + " does not exist" );
			return usedLibFiles;
		}
		final File[ ] containedFiles = baseDir.listFiles( );
		for ( final File containedFile : containedFiles )
		{
			if ( containedFile.isDirectory( ) )
			{
				usedLibFiles.addAll( getUsedModuleLibs( log, containedFile ) );
			}
			else
			{
				if ( containedFile.getName( ).endsWith( ".jar" ) )
				{
					if ( isUsedModuleLib( containedFile ) )
					{
						usedLibFiles.add( containedFile );
					}
				}
			}
		}

		return usedLibFiles;
	}

	/**
	 * AOM-5541 determines whether a jar is a used module lib or not
	 *
	 * @param file
	 * @return
	 */
	private static boolean isUsedModuleLib( final File file )
	{
		try (final JarFile jar = new JarFile( file ))
		{
			final Manifest mf = jar.getManifest( );
			if ( mf != null )
			{
				final Attributes attrs = mf.getMainAttributes( );
				if ( attrs != null )
				{
					String aomValue = attrs.getValue( "Aom-LastModified" );
					if ( aomValue == null )
					{
						aomValue = attrs.getValue( "Aom-IsUsedModuleLib" );
					}
					if ( aomValue != null )
					{
						return true;
					}
				}
			}
		}
		catch ( final IOException e )
		{
			/* Ignore */
		}
		return false;
	}

	/**
	 * Deletes a single file if it exists
	 *
	 * @param baseDir the base directory path to work on
	 * @param fileName the file name
	 */
	private static void deleteFile( final Log log, final File baseDir, final String fileName )
	{
		final File fileToDelete = new File( baseDir, fileName );
		if ( fileToDelete.exists( ) )
		{
			fileToDelete.delete( );
			log.info( "Deleted file: " + fileToDelete.getAbsolutePath( ) );
		}
	}

	/**
	 * Deletes multiple files which are provided in an array of filenames
	 *
	 * @param baseDir the base directory path to work on
	 * @param fileNames the file name
	 */
	private static void deleteFiles( final Log log, final File baseDir, final String[ ] fileNames )
	{
		for ( final String fileToDelete : fileNames )
		{
			deleteFile( log, baseDir, fileToDelete );
		}
	}

	/**
	 * updates the classpath file
	 *
	 * @param libsToRemove the library-names to remove from the classpath (may be null to remove nothing)
	 * @param libsToAdd the library-names to add to the classpath (may be null to add nothing)
	 */
	private void updateClassPath( final String[ ] libsToRemove, final String[ ] libsToAdd )
	{
		final File classPathFile = new File( this.basePath, ".classpath" );
		if ( classPathFile.exists( ) )
		{
			final String libStringToFormat = "<classpathentry kind=\"lib\" path=\"lib/%s\"/>";
			final String endClassPath = "</classpath>";
			String content = null;
			try
			{
				content = FileUtils.readFileToString( classPathFile, Charset.forName( "UTF-8" ) );
			}
			catch ( final IOException e )
			{
				e.printStackTrace( );
			}
			if ( content == null )
			{
				this.log.error( "No content was given, aborting." );
				return;
			}

			if ( libsToRemove != null )
			{
				for ( final String libToRemove : libsToRemove )
				{
					this.log.info( "Removing classpath-entry for lib: " + libToRemove );
					final String formatString = String.format( libStringToFormat, libToRemove );
					content = content.replace( formatString, "" );
				}
			}
			if ( libsToAdd != null )
			{
				for ( final String libToAdd : libsToAdd )
				{
					final String formattedLibString = String.format( libStringToFormat, libToAdd );
					if ( content.contains( formattedLibString ) == false )
					{
						this.log.info( "Adding classpath-entry for lib: " + libToAdd );
						content = content.replace( endClassPath, formattedLibString + endClassPath );
					}
				}
			}
			try (FileOutputStream fos = new FileOutputStream( classPathFile ))
			{
				fos.write( content.getBytes( ) );
				fos.flush( );
			}
			catch ( final IOException e )
			{
				e.printStackTrace( ); // NOPMD
			}
		}
	}

	/**
	 * updates the pom.xml and adds/removes the given libraries
	 *
	 * @param libsToRemove the libraries to remove from the dependencies of the pom.xml
	 * @param libsToAdd the libraries to add to the dependencies of the pom.xml
	 */
	private void updatePomXml250( final String[ ] libsToRemove, final String[ ] libsToAdd )
	{
		final File pomxmlFile = new File( this.basePath, "pom.xml" );
		if ( pomxmlFile.exists( ) )
		{
			final String depStart = "<dependency>";
			final String depEnd = "</dependency>";
			final String depIdentStringToFormat = "<systemPath>${project.basedir}/lib/%s</systemPath>";
			final String depFullStringToFormat =
				"<dependency><groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version>" +
					"<scope>system</scope><systemPath>${project.basedir}/lib/%s</systemPath></dependency>";
			String content = null;
			try
			{
				content = FileUtils.readFileToString( pomxmlFile, Charset.forName( "UTF-8" ) );
			}
			catch ( final IOException e )
			{
				e.printStackTrace( );// NOPMD
			}
			if ( content == null )
			{
				this.log.error( "No content was given during update of pom.xml, aborting pom update." );
				return;
			}
			if ( libsToRemove != null )
			{
				for ( final String libToRemove : libsToRemove )
				{
					this.log.info( "Removing pom.xml dependency entry for: " + libToRemove );
					final String searchString = String.format( depIdentStringToFormat, libToRemove );
					final int libIdx = content.indexOf( searchString );
					if ( libIdx == -1 )
					{
						/* continue, as the lib did not exist */
						continue;
					}
					String contentBeforeSearch = content.substring( 0, libIdx );
					contentBeforeSearch =
						contentBeforeSearch.substring( 0, contentBeforeSearch.lastIndexOf( depStart ) );
					String contentAfterSearch = content.substring( libIdx );
					contentAfterSearch =
						contentAfterSearch.substring( contentAfterSearch.indexOf( depEnd ) + depEnd.length( ) );
					content = contentBeforeSearch + contentAfterSearch;
				}
			}
			if ( libsToAdd != null && libsToAdd.length > 0 )
			{
				final int depsEndIdx = content.indexOf( "</dependencies>" );
				String beforeDepsEnd = content.substring( 0, depsEndIdx );
				final String afterDepsEnd = content.substring( depsEndIdx );
				for ( final String libToAdd : libsToAdd )
				{
					final String searchString = String.format( depIdentStringToFormat, libToAdd );
					if ( content.contains( searchString ) == false )
					{
						this.log.info( "Adding pom.xml dependency entry for: " + libToAdd );
						final String[ ] data = getNameAndVersionFromString( libToAdd );
						final String depStringToAdd =
							String.format( depFullStringToFormat, data[ 0 ], data[ 0 ], data[ 1 ], libToAdd );

						beforeDepsEnd += depStringToAdd;
					}
				}
				content = beforeDepsEnd + afterDepsEnd;
			}
			try (FileOutputStream fos = new FileOutputStream( pomxmlFile ))
			{
				fos.write( content.getBytes( ) );
				fos.flush( );
			}
			catch ( final IOException e )
			{
				e.printStackTrace( );// NOPMD
			}
		}
	}

	Map<File, File> createUpdated330PomFile( final List<Dependency> libsToChange, final String[ ] libNames,
		final List<File> usedLibs )
		throws ParserConfigurationException, SAXException, IOException, TransformerException
	{
		final Map<File, File> oldNewFiles = new HashMap<>( );
		final File pomFile = new File( this.basePath, "pom.xml" );
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance( );
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder( );
		final Document doc = dBuilder.parse( pomFile );
		doc.getDocumentElement( ).normalize( );
		NodeList nodes = doc.getElementsByTagName( "project" );
		if ( nodes == null || nodes.getLength( ) == 0 )
		{
			this.log.error( "Updating pom.xml failed, probably corrupted pom.xml?" );
			return oldNewFiles;
		}
		final Node projectNode = nodes.item( 0 );
		nodes = projectNode.getChildNodes( );
		Node dependenciesNode = null;
		Node buildNode = null;
		Node profilesNode = null;
		Node nameNode = null;
		for ( int i = 0; i < nodes.getLength( ); i++ )
		{
			final Node node = nodes.item( i );
			if ( "dependencies".equals( node.getNodeName( ) ) )
			{
				dependenciesNode = node;
			}
			if ( "version".equals( node.getNodeName( ) ) )
			{
				final Text text = ( Text ) node.getFirstChild( );
				if ( text.getData( ).equals( "1" ) )
				{
					text.setData( "1.0.0" );
				}
			}
			if ( "build".equals( node.getNodeName( ) ) )
			{
				buildNode = node;
			}
			if ( "name".equals( node.getNodeName( ) ) )
			{
				nameNode = node;
			}
			if ( "profiles".equals( node.getNodeName( ) ) )
			{
				profilesNode = node;
			}
		}
		if ( nameNode == null )
		{
			/* no name found, set it */
			nameNode = doc.createElement( "name" );
			nameNode.appendChild( doc.createTextNode( this.oneModuleName ) );
			projectNode.appendChild( nameNode );
		}

		handleBuildPluginNode330( doc, projectNode, buildNode, libsToChange );
		cleanProfiles( profilesNode );
		oldNewFiles.putAll( handleDependenciesNode( projectNode, dependenciesNode, libsToChange, libNames, usedLibs ) );

		final Transformer tf = TransformerFactory.newInstance( ).newTransformer( );
		tf.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
		tf.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "4" );
		tf.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
		tf.setOutputProperty( OutputKeys.INDENT, "yes" );
		final StringWriter out = new StringWriter( );
		tf.transform( new DOMSource( doc ), new StreamResult( out ) );
		FileUtils.write( pomFile, out.toString( ), "UTF-8" );

		return oldNewFiles;
	}

	private void handleBuildPluginNode330( final Document doc, final Node projectNode, final Node buildNode,
		final List<Dependency> libsToChange )
	{
		Node aomNmPluginNode = null;
		Node pluginsNode = null;
		Node antrunPluginNode = null;
		if ( buildNode != null )
		{
			Node copyDependencyPluginNode = null;

			final int childNodeLength = buildNode.getChildNodes( ).getLength( );
			for ( int i = 0; i < childNodeLength; i++ )
			{
				final Node buildChild = buildNode.getChildNodes( ).item( i );
				if ( buildChild != null && "plugins".equals( buildChild.getNodeName( ) ) )
				{
					pluginsNode = buildChild;
					/* only <plugin> allowed within <plugins> */
					final NodeList pluginNodes = pluginsNode.getChildNodes( );
					for ( int j = 0; j < pluginNodes.getLength( ); j++ )
					{
						final Node pluginNode = pluginNodes.item( j );
						if ( copyDependencyPluginNode == null &&
							matchSpecificInnerNode( pluginNode, "artifactId", "maven-dependency-plugin" ) )
						{
							copyDependencyPluginNode = pluginNode;
						}
						if ( aomNmPluginNode == null &&
							matchSpecificInnerNode( pluginNode, "artifactId", "nm-maven-plugin" ) )
						{
							aomNmPluginNode = pluginNode;
						}
						if ( antrunPluginNode == null &&
							matchSpecificInnerNode( pluginNode, "artifactId", "maven-antrun-plugin" ) &&
							isAntrunPluginToRemove( pluginNode ) )
						{
							antrunPluginNode = pluginNode;
						}
						if ( copyDependencyPluginNode != null && aomNmPluginNode != null )
						{
							break;
						}
					}
					/* break loop after plugins-element has been processed */
					break;
				}
			}
			if ( copyDependencyPluginNode != null )
			{
				/* change the copy dependency node */
				updateCopyDependenciesNode( copyDependencyPluginNode, libsToChange, doc );
			}
		}
		/* append the plugin if it does not exist */
		if ( aomNmPluginNode == null )
		{

			this.log.info( "Append aom-nm plugin child" );
			final Element plugin = doc.createElement( "plugin" );
			final Element groupId = doc.createElement( "groupId" );
			final Element artifactId = doc.createElement( "artifactId" );
			final Element version = doc.createElement( "version" );
			final Element executions = doc.createElement( "executions" );
			final Element execution = doc.createElement( "execution" );
			final Element goals = doc.createElement( "goals" );
			final Element goal = doc.createElement( "goal" );

			groupId.appendChild( doc.createTextNode( "com.apiomat.helper" ) );
			artifactId.appendChild( doc.createTextNode( "nm-maven-plugin" ) );
			// TODO 5582 must be set to actual version name when released, 1.0.0-SNAPSHOT is just provisionally
			version.appendChild( doc.createTextNode( "1.0.0-SNAPSHOT" ) );
			goal.appendChild( doc.createTextNode( "package" ) );

			goals.appendChild( goal );
			execution.appendChild( goals );
			executions.appendChild( execution );

			plugin.appendChild( groupId );
			plugin.appendChild( artifactId );
			plugin.appendChild( version );
			plugin.appendChild( executions );

			Node existingBuildNode = buildNode;
			if ( buildNode == null )
			{
				existingBuildNode = doc.createElement( "build" );
				projectNode.appendChild( existingBuildNode );
			}
			if ( pluginsNode == null )
			{
				pluginsNode = doc.createElement( "plugins" );
			}
			pluginsNode.appendChild( plugin );
		}
		/* AOM-5541 remove obsolete antrun plugin */
		if ( antrunPluginNode != null )
		{
			pluginsNode.removeChild( antrunPluginNode );
		}

	}

	/**
	 * AOM-5541 removes obsolete ant targets from pom.xml
	 *
	 * @param profilesNode
	 */
	private static void cleanProfiles( final Node profilesNode )
	{
		if ( profilesNode != null )
		{
			final NodeList profileNodes = profilesNode.getChildNodes( );
			final List<Node> profilesToRemove = new ArrayList<Node>( );
			for ( int i = 0; i < profileNodes.getLength( ); i++ )
			{
				final Node node = profileNodes.item( i );
				if ( node.getTextContent( ).contains( "ant.target" ) &&
					( matchSpecificInnerNode( node, "id", "release" ) ||
						matchSpecificInnerNode( node, "id", "unrelease" ) ) )
				{
					profilesToRemove.add( node );
				}
			}
			profilesToRemove.forEach( n -> profilesNode.removeChild( n ) );
		}
	}

	private static boolean isAntrunPluginToRemove( final Node node )
	{
		final String toCompareWith =
			"<plugin>" +
				"<artifactId>maven-antrun-plugin</artifactId>" +
				"<dependencies>" +
				"<dependency>" +
				"<groupId>com.sun</groupId>" +
				"<artifactId>tools</artifactId>" +
				"<version>${tools.version}</version>" +
				"<scope>system</scope>" +
				"<systemPath>${java.home}/../lib/tools.jar</systemPath>" +
				"</dependency>" +
				"</dependencies>" +
				"<executions>" +
				"<execution>" +
				"<phase>install</phase>" +
				"<goals><goal>run</goal></goals>" +
				"<configuration>" +
				"<tasks><ant antfile=\"build.xml\" target=\"${ant.target}\"/></tasks>" +
				"</configuration>" +
				"</execution>" +
				"</executions>" +
				"</plugin>";
		final String startsWith = "<plugin>" +
			"<artifactId>maven-antrun-plugin</artifactId>";
		final String contains = "<ant antfile=\"build.xml\" target=\"${ant.target}\"/>";
		final String nodeXmlString = getNodeAsXmlString( node );
		return toCompareWith.equals( nodeXmlString ) ||
			( nodeXmlString.startsWith( startsWith ) && nodeXmlString.contains( contains ) );
	}

	/**
	 * Converts a {@link Node} to an xml String containing all tags and text contents. Removes spaces between tags and
	 * all control sequences. (See
	 * https://stackoverflow.com/questions/33935718/save-new-xml-node-to-file/33936257#33936257)
	 *
	 * @param node
	 * @return an xml String representation of the Node
	 */
	private static String getNodeAsXmlString( final Node node )
	{
		if ( node == null )
		{
			throw new IllegalArgumentException( "node is null." );
		}
		try
		{
			/* Remove unwanted whitespaces */
			node.normalize( );
			final XPath xpath = XPathFactory.newInstance( ).newXPath( );
			final XPathExpression expr = xpath.compile( "//text()[normalize-space()='']" );
			final NodeList nodeList = ( NodeList ) expr.evaluate( node, XPathConstants.NODESET );

			for ( int i = 0; i < nodeList.getLength( ); ++i )
			{
				final Node nd = nodeList.item( i );
				nd.getParentNode( ).removeChild( nd );
			}
			/* Create and setup transformer */
			final Transformer transformer = TransformerFactory.newInstance( ).newTransformer( );
			transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );

			transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
			/* Turn the node into a string */
			final StringWriter writer = new StringWriter( );
			transformer.transform( new DOMSource( node ), new StreamResult( writer ) );
			return writer.toString( );
		}
		catch ( final TransformerException e )
		{
			throw new RuntimeException( e );
		}
		catch ( final XPathExpressionException e )
		{
			throw new RuntimeException( e );
		}
	}

	private Map<File, File> handleDependenciesNode( final Node projectNode, final Node dependenciesNode,
		final List<Dependency> libsToChange, final String[ ] libNames,
		final List<File> usedLibs )
	{
		final Map<File, File> oldNewFiles = new HashMap<>( );
		final Document doc = projectNode.getOwnerDocument( );
		final Set<String> alreadyContainedAsProvided = new HashSet<>( );
		Node existingDependenciesNode = dependenciesNode;
		if ( existingDependenciesNode != null )
		{
			final List<Node> nodesToRemove = new ArrayList<>( );
			final NodeList nodes = existingDependenciesNode.getChildNodes( );
			/* find the nodes to remove from dependencies */
			for ( int i = 0; i < nodes.getLength( ); i++ )
			{
				/* each node should be a dependency-tag */
				final Node dependencyNode = nodes.item( i );
				if ( dependencyNode.hasChildNodes( ) == false )
				{
					nodesToRemove.add( dependencyNode );
					continue;
				}

				final NodeList nodesInner = dependencyNode.getChildNodes( );
				boolean isSystemScopeNode = false;
				boolean isSystemPathAvailable = false;
				boolean isLibToRemove = false;
				boolean isProvidedLib = false;
				boolean isDependencyToChange = false;
				File currentUsedFile = null;
				Text systemPathTextNode = null;
				String artifactId = null;
				String groupId = null;
				String version = null;
				for ( int j = 0; j < nodesInner.getLength( ); j++ )
				{
					final Node nodeInner = nodesInner.item( j );

					if ( "scope".equals( nodeInner.getNodeName( ) ) )
					{
						final Text text = ( Text ) nodeInner.getFirstChild( );
						if ( "system".equals( text.getData( ) ) )
						{
							isSystemScopeNode = true;
						}
						else if ( "provided".equals( text.getData( ) ) )
						{
							isProvidedLib = true;
						}
					}
					if ( "systemPath".equals( nodeInner.getNodeName( ) ) )
					{
						final Text text = ( Text ) nodeInner.getFirstChild( );
						if ( text != null && text.getData( ) != null )
						{
							final String systemPathContent = text.getData( );
							isLibToRemove = Stream.of( libNames ).filter( lib -> systemPathContent.endsWith( lib ) )
								.findAny( ).isPresent( );
							final Optional<File> optUsedNMFile = usedLibs.stream( )
								.filter( usedFile -> systemPathContent.endsWith( usedFile.getName( ) ) ).findAny( );
							if ( optUsedNMFile.isPresent( ) )
							{
								currentUsedFile = optUsedNMFile.get( );
								systemPathTextNode = text;
							}
						}
						isSystemPathAvailable = true;

					}
					if ( "artifactId".equals( nodeInner.getNodeName( ) ) )
					{
						final Text text = ( Text ) nodeInner.getFirstChild( );
						if ( text != null && text.getData( ) != null )
						{
							artifactId = text.getData( );
						}
					}
					if ( "groupId".equals( nodeInner.getNodeName( ) ) )
					{
						final Text text = ( Text ) nodeInner.getFirstChild( );
						if ( text != null && text.getData( ) != null )
						{
							groupId = text.getData( );
						}
					}
					if ( "version".equals( nodeInner.getNodeName( ) ) )
					{
						final Text text = ( Text ) nodeInner.getFirstChild( );
						if ( text != null && text.getData( ) != null )
						{
							version = text.getData( );
						}
					}
					if ( artifactId != null && groupId != null )
					{
						/* must be final */
						final String aIdToCheck = artifactId;
						final String gIdToCheck = groupId;
						final Optional<Dependency> optDep = libsToChange.stream( ).filter(
							dep -> dep.getArtifactId( ).equals( aIdToCheck ) && dep.getGroupId( ).equals( gIdToCheck ) )
							.findAny( );
						isDependencyToChange = optDep.isPresent( );

					}
					if ( isSystemScopeNode && isSystemPathAvailable )
					{
						if ( isLibToRemove )
						{
							nodesToRemove.add( dependencyNode );
							break;
						}
					}
					if ( isDependencyToChange && isProvidedLib )
					{
						/* isDependencyToChange includes that gId and aId are set */
						alreadyContainedAsProvided.add( groupId + ":" + artifactId );
					}
					if ( currentUsedFile != null && artifactId != null && systemPathTextNode != null &&
						version != null )
					{
						/* change to versioned jar */
						final String systemPathContent = systemPathTextNode.getData( );
						String newSystemPath = systemPathContent.substring( 0, systemPathContent.length( ) - 4 );
						if ( newSystemPath.contains( version ) == false &&
							newSystemPath.toLowerCase( ).endsWith( artifactId.toLowerCase( ) ) )
						{
							newSystemPath = newSystemPath + "-" + version + ".jar";
							this.log.info( "Rename artifact " + artifactId + " systempath from " + systemPathContent +
								" to " + newSystemPath + " in pom.xml" );
							systemPathTextNode.setData( newSystemPath );
							final File newPathFile = new File( newSystemPath );
							final File targetPathFile =
								new File( currentUsedFile.getParentFile( ), newPathFile.getName( ) );
							oldNewFiles.put( currentUsedFile, targetPathFile );

						}
						else if ( systemPathContent.toLowerCase( ).trim( )
							.endsWith( ( artifactId + "-" + version + ".jar" ).toLowerCase( ) ) )
						{
							/* AOM-5541 check if an orphaned file exists without version which is a used module lib */
							/* new lib already exists, check whether we have to remove the old lib */
							final String aidForStream = artifactId;
							final Optional<File> optUsedLib = usedLibs.stream( )
								.filter(
									f -> f.getName( ).toLowerCase( )
										.equals( ( aidForStream + ".jar" ).toLowerCase( ) ) )
								.findAny( );
							if ( optUsedLib.isPresent( ) )
							{
								oldNewFiles.put( optUsedLib.get( ), currentUsedFile );
							}
						}
					}
				}
			}

			/* remove the nodes from dependencies */
			for ( final Node nodeToRemove : nodesToRemove )
			{
				existingDependenciesNode.removeChild( nodeToRemove );
			}
		}
		else
		{
			/* no dependencies node found, create and add it */
			existingDependenciesNode = doc.createElement( "dependencies" );
			projectNode.appendChild( existingDependenciesNode );
		}

		for ( final Dependency jarInf : libsToChange )
		{
			if ( alreadyContainedAsProvided.contains( jarInf.getGroupId( ) + ":" + jarInf.getArtifactId( ) ) == false )
			{
				this.log.info( "Adding child " + jarInf.getArtifactId( ) );
				final Element dependency = doc.createElement( "dependency" );
				final Element groupId = doc.createElement( "groupId" );
				final Element artifactId = doc.createElement( "artifactId" );
				final Element version = doc.createElement( "version" );
				final Element scope = doc.createElement( "scope" );

				groupId.appendChild( doc.createTextNode( jarInf.getGroupId( ) ) );
				artifactId.appendChild( doc.createTextNode( jarInf.getArtifactId( ) ) );
				version.appendChild( doc.createTextNode( jarInf.getVersion( ) ) );
				scope.appendChild( doc.createTextNode( jarInf.getScope( ) ) );

				dependency.appendChild( groupId );
				dependency.appendChild( artifactId );
				dependency.appendChild( version );
				dependency.appendChild( scope );
				existingDependenciesNode.appendChild( dependency );
			}
		}
		return oldNewFiles;
	}

	/**
	 * Returns the parent node, if the inner node matches a specific name and inner text
	 * Example1: calling matchSpecificInnerNode(pluginNode, "artifactId", "my-artifact-name" on this example:
	 *
	 * <pre>
	 * {@code
	 * <plugin>
	 *   <groupId>foo</groupId>
	 *   <artifactId>my-artifact-name</artifactId>
	 * </plugin>
	 * }
	 * </pre>
	 *
	 * will return true
	 *
	 * @param parentNode
	 * @param textToMatch
	 * @return
	 */
	private static boolean matchSpecificInnerNode( final Node parentNode, final String innerNodeName,
		final String textToMatch )
	{
		boolean matchesNode = false;
		final NodeList pluginInnerChilds = parentNode.getChildNodes( );
		for ( int k = 0; k < pluginInnerChilds.getLength( ); k++ )
		{
			if ( innerNodeName.equals( pluginInnerChilds.item( k ).getNodeName( ) ) )
			{
				final Text textNode = ( Text ) pluginInnerChilds.item( k ).getFirstChild( );
				if ( textToMatch.equals( textNode.getData( ) ) )
				{
					matchesNode = true;
					break;
				}
			}
		}
		return matchesNode;
	}

	private static void updateCopyDependenciesNode( final Node copyDependenciesNode,
		final List<Dependency> libsToUpdate, final Document doc )
	{
		final NodeList pluginChilds = copyDependenciesNode.getChildNodes( );
		NodeList correctExecutionNodeChilds = null;
		for ( int i = 0; i < pluginChilds.getLength( ); i++ )
		{
			if ( "executions".equals( pluginChilds.item( i ).getNodeName( ) ) )
			{
				final NodeList executionNodes = pluginChilds.item( i ).getChildNodes( );
				for ( int j = 0; j < executionNodes.getLength( ); j++ )
				{
					final NodeList executionNodeChilds = executionNodes.item( j ).getChildNodes( );
					for ( int k = 0; k < executionNodeChilds.getLength( ); k++ )
					{
						if ( "goals".equals( executionNodeChilds.item( k ).getNodeName( ) ) )
						{
							final boolean correctNode =
								matchSpecificInnerNode( executionNodeChilds.item( k ), "goal", "copy-dependencies" );
							if ( correctNode )
							{
								/* this node is the correct execution */
								correctExecutionNodeChilds = executionNodeChilds;
								break;
							}
						}
					}
					if ( correctExecutionNodeChilds != null )
					{
						break;
					}
				}
				if ( correctExecutionNodeChilds != null )
				{
					break;
				}
			}
		}

		if ( correctExecutionNodeChilds != null )
		{
			Node configurationNode = null;
			/* now find the configuration node */
			for ( int i = 0; i < correctExecutionNodeChilds.getLength( ); i++ )
			{
				if ( "configuration".equals( correctExecutionNodeChilds.item( i ).getNodeName( ) ) )
				{
					configurationNode = correctExecutionNodeChilds.item( i );
					break;
				}
			}
			if ( configurationNode != null )
			{
				String content =
					libsToUpdate.stream( ).map( dep -> dep.getArtifactId( ) ).collect( Collectors.joining( "," ) );
				final String exclusionIdentifier = "excludeArtifactIds";

				/* check if exclusionArtifactIds is already part of the configNode or not */
				boolean exclusionNeeded = true;
				int itemToReplaceIndex = -1;
				final NodeList configNodes = configurationNode.getChildNodes( );
				for ( int i = 0; i < configNodes.getLength( ); i++ )
				{
					final Node element = configNodes.item( i );
					if ( exclusionIdentifier.equals( element.getNodeName( ) ) )
					{
						/* case 1: check if node exclusionArtifacs is empty => repair */
						final String currentContent = element.getTextContent( ).toString( );
						if ( StringUtils.isBlank( currentContent ) )
						{
							exclusionNeeded = true;
							itemToReplaceIndex = i;
							break;
						}

						/* case 2: check if node content is the same => skip adding exclusionArtifacts again */
						if ( currentContent.equals( content ) )
						{
							exclusionNeeded = false;
							break;
						}

						/* case 3: check if each expected lib is in exclusion => repair broken exclusionArtifacts */
						final StringBuilder contentBuilder = new StringBuilder( );
						contentBuilder.append( currentContent );
						exclusionNeeded = false;
						for ( final Dependency lib : libsToUpdate )
						{
							final String libName = lib.getArtifactId( );
							if ( currentContent.contains( libName ) == false )
							{
								contentBuilder.append( ',' );
								contentBuilder.append( libName );
								exclusionNeeded = true;
								itemToReplaceIndex = i;
							}
						}
						content = contentBuilder.toString( );
						break;
					}
				}

				if ( exclusionNeeded )
				{
					/* replace old exclusion item with new content */
					if ( itemToReplaceIndex > -1 )
					{
						configurationNode.removeChild( configurationNode.getChildNodes( ).item( itemToReplaceIndex ) );
					}

					final Element excludedArtifactIdsNode = doc.createElement( exclusionIdentifier );
					excludedArtifactIdsNode.appendChild( doc.createTextNode( content ) );
					configurationNode.appendChild( excludedArtifactIdsNode );
				}
			}
		}
	}

	/**
	 * extracts the version and the name of the library from the given string
	 *
	 * @param fileNameWithEnding
	 * @return a string array with the lib name (without version and file-ending) on first position and the version on
	 *         the second position of the array
	 */
	private static String[ ] getNameAndVersionFromString( final String fileNameWithEnding )
	{
		String fileNameWithoutEnding =
			fileNameWithEnding.substring( 0, fileNameWithEnding.lastIndexOf( '.' ) );
		final int verStart = fileNameWithoutEnding.lastIndexOf( '-' );

		final String fileVersion =
			verStart == -1 ? "1.0.0" : fileNameWithoutEnding.substring( verStart + 1 );
		if ( verStart > -1 )
		{
			fileNameWithoutEnding = fileNameWithoutEnding.substring( 0, verStart );
		}
		return new String[ ] { fileNameWithoutEnding, fileVersion };
	}
}
