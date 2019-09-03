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
package com.apiomat.helper.mvnnmhelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import com.google.common.io.Files;

/**
 * Tests the module update
 *
 * @author thum
 */
public class ModuleUpdateManagerTest
{

	/**
	 * Tests the update of old unversioned libraries within the pom.xml
	 *
	 * @throws Exception
	 */
	@Test
	public void testCreateUpdated33PomFile( ) throws Exception
	{
		final File tmpDir = Files.createTempDir( );
		try
		{
			final File pomFile = new File( tmpDir, "pom.xml" );
			final File jdbcWOVersionFile = new File( tmpDir, "lib" + File.separatorChar + "JDBC.jar" );
			final File jdbcWVersionFile = new File( tmpDir, "lib" + File.separatorChar + "JDBC-1.0.0.jar" );

			try (InputStream is = this.getClass( ).getResourceAsStream( "pom.xml" ))
			{
				FileUtils.copyStreamToFile( new RawInputStreamFacade( is ), pomFile );
			}

			final ModuleUpdateManager mum = new ModuleUpdateManager( tmpDir, "JDBC", new SystemStreamLog( ) );
			final List<File> filesToMove = new LinkedList<>( );

			filesToMove.add( new File( tmpDir, "lib" + File.separatorChar + "JDBC.jar" ) );
			Map<File, File> resultMap =
				mum.createUpdated330PomFile( mum.dependencies330, mum.jarsToRemove330, filesToMove );
			assertEquals( 1, resultMap.size( ) );
			assertTrue( resultMap.containsKey( jdbcWOVersionFile ) );
			assertEquals( jdbcWVersionFile, resultMap.get( jdbcWOVersionFile ) );
			final MavenXpp3Reader reader = new MavenXpp3Reader( );
			try (final FileInputStream fis = new FileInputStream( pomFile ))
			{
				final Model pomModel = reader.read( fis );
				checkPomModel( pomModel, mum );
			}
			/* run again and check again */
			resultMap = mum.createUpdated330PomFile( mum.dependencies330, mum.jarsToRemove330, filesToMove );
			assertTrue( resultMap.isEmpty( ) );
			try (final FileInputStream fis = new FileInputStream( pomFile ))
			{
				final Model pomModel = reader.read( fis );
				checkPomModel( pomModel, mum );
			}

			/* check that that excludeArtifactIds was only set once after second update execution */
			final String pomFileContent = FileUtils.fileRead( pomFile );
			final String findExclusionTag = "<excludeArtifactIds>";
			final int firstIndex = pomFileContent.indexOf( findExclusionTag );
			final int lastIndex = pomFileContent.lastIndexOf( findExclusionTag );

			assertEquals( "The xml attribute '" + findExclusionTag + "' was expected to exist only once.",
				firstIndex, lastIndex );
		}
		finally
		{
			FileUtils.deleteDirectory( tmpDir );
		}
	}

	/**
	 * Tests the update of old unversioned libraries, if the new library already exists
	 *
	 * @throws Exception
	 */
	@Test
	public void testUpdatedUsedModuleLibWithExistingVersioned( ) throws Exception
	{
		final File tmpDir = Files.createTempDir( );
		try
		{
			final File pomFile = new File( tmpDir, "pom.xml" );
			final File jdbcWOVersionFile = new File( tmpDir, "lib" + File.separatorChar + "JDBC.jar" );
			final File jdbcWVersionFile = new File( tmpDir, "lib" + File.separatorChar + "JDBC-1.0.0.jar" );

			try (InputStream is = this.getClass( ).getResourceAsStream( "pom.xml" ))
			{
				FileUtils.copyStreamToFile( new RawInputStreamFacade( is ), pomFile );
			}
			/* loading small dummy jar that only contains a manifest to check if libs are cleaned up correctly */
			try (InputStream is = this.getClass( ).getResourceAsStream( "JDBC-DUMMY.jar" ))
			{
				FileUtils.copyStreamToFile( new RawInputStreamFacade( is ), jdbcWOVersionFile );
			}
			try (InputStream is = this.getClass( ).getResourceAsStream( "JDBC-DUMMY.jar" ))
			{
				FileUtils.copyStreamToFile( new RawInputStreamFacade( is ), jdbcWVersionFile );
			}

			final ModuleUpdateManager mum = new ModuleUpdateManager( tmpDir, "JDBC", new SystemStreamLog( ) );
			final List<File> filesToMove = new LinkedList<>( );

			filesToMove.add( new File( tmpDir, "lib" + File.separatorChar + "JDBC.jar" ) );
			mum.cleanJarsFor330( );
			/* libs should have been cleaned correctly and only the correct used module lib file should exist */
			assertFalse( jdbcWOVersionFile.exists( ) );
			assertTrue( jdbcWVersionFile.exists( ) );
		}
		finally
		{
			FileUtils.deleteDirectory( tmpDir );
		}
	}

	/**
	 * Tests the update of old unversioned libraries, if the new library already exists
	 *
	 * @throws Exception
	 */
	@Test
	public void testUpdatedUsedModuleLibWithoutExistingVersioned( ) throws Exception
	{
		final File tmpDir = Files.createTempDir( );
		try
		{
			final File pomFile = new File( tmpDir, "pom.xml" );
			final File jdbcWOVersionFile = new File( tmpDir, "lib" + File.separatorChar + "JDBC.jar" );
			final File jdbcWVersionFile = new File( tmpDir, "lib" + File.separatorChar + "JDBC-1.0.0.jar" );

			try (InputStream is = this.getClass( ).getResourceAsStream( "pom.xml" ))
			{
				FileUtils.copyStreamToFile( new RawInputStreamFacade( is ), pomFile );
			}
			/* loading small dummy jar that only contains a manifest to check if libs are cleaned up correctly */
			try (InputStream is = this.getClass( ).getResourceAsStream( "JDBC-DUMMY.jar" ))
			{
				FileUtils.copyStreamToFile( new RawInputStreamFacade( is ), jdbcWOVersionFile );
			}

			final ModuleUpdateManager mum = new ModuleUpdateManager( tmpDir, "JDBC", new SystemStreamLog( ) );
			final List<File> filesToMove = new LinkedList<>( );

			filesToMove.add( new File( tmpDir, "lib" + File.separatorChar + "JDBC.jar" ) );
			mum.cleanJarsFor330( );
			/* libs should have been cleaned correctly and only the correct used module lib file should exist */
			assertFalse( jdbcWOVersionFile.exists( ) );
			assertTrue( jdbcWVersionFile.exists( ) );
		}
		finally
		{
			FileUtils.deleteDirectory( tmpDir );
		}
	}

	private static void checkPomModel( final Model pomModel, final ModuleUpdateManager mum )
	{
		/* check the changed version */
		assertEquals( "1.0.0", pomModel.getVersion( ) );

		/* check name */
		assertEquals( "JDBC", pomModel.getName( ) );

		final List<Plugin> buildPlugins = pomModel.getBuild( ).getPlugins( );
		/* excludeArtifactsId should have been added to the copy dependencies plugin */
		final Optional<Plugin> copyDepPluginOpt =
			buildPlugins.stream( ).filter( p -> p.getGroupId( ).equals( "org.apache.maven.plugins" ) &&
				p.getArtifactId( ).equals( "maven-dependency-plugin" ) ).findFirst( );
		assertTrue( copyDepPluginOpt.isPresent( ) );
		/* AOM-5541 now also checking for presence of outdated antrun plugin */
		final Optional<Plugin> antrunPluginOpt =
			buildPlugins.stream( ).filter( p -> p.getArtifactId( ).equals( "maven-antrun-plugin" ) ).findFirst( );
		assertFalse( antrunPluginOpt.isPresent( ) );
		final Plugin copyDepPlugin = copyDepPluginOpt.get( );
		final PluginExecution cdExec = copyDepPlugin.getExecutionsAsMap( ).get( "copy-dependencies" );
		final Xpp3Dom configDom = ( Xpp3Dom ) cdExec.getConfiguration( );
		final Xpp3Dom eai = configDom.getChild( "excludeArtifactIds" );
		assertNotNull( eai );

		/* our maven plugin should have been added to the buildplugins */
		final List<Plugin> aomHelper = buildPlugins.stream( ).filter(
			p -> p.getGroupId( ).equals( "com.apiomat.helper" ) && p.getArtifactId( ).equals( "nm-maven-plugin" ) )
			.collect( Collectors.toList( ) );
		assertEquals( 1, aomHelper.size( ) );

		/* check the changed dependencies */
		final String eaiValue = eai.getValue( );
		final String excludedDeps =
			mum.dependencies330.stream( ).map( n -> n.getArtifactId( ) ).collect( Collectors.joining( "," ) );
		assertEquals( excludedDeps, eaiValue );
		final List<Dependency> dependencies = pomModel.getDependencies( );

		for ( final Dependency expectedDependency : mum.dependencies330 )
		{
			int containedCount = 0;
			int containedWithScope = 0;
			for ( final Dependency containedDependency : dependencies )
			{
				if ( expectedDependency.getArtifactId( ).equals( containedDependency.getArtifactId( ) ) &&
					expectedDependency.getGroupId( ).equals( containedDependency.getGroupId( ) ) )
				{
					containedCount++;
					if ( expectedDependency.getScope( ).equals( containedDependency.getScope( ) ) )
					{
						containedWithScope++;
					}
				}
			}
			assertEquals( expectedDependency + " was contained more than one time", 1, containedCount );
			assertEquals( expectedDependency + " was contained more than one time", containedCount,
				containedWithScope );
		}
		/* check the jdbc dependency, should be changed during first update and not changed on second update */
		final Dependency jdbcDep =
			dependencies.stream( ).filter( dep -> dep.getArtifactId( ).equalsIgnoreCase( "JDBC" ) &&
				dep.getGroupId( ).equals( "com.apiomat.nativemodule" ) ).findFirst( ).get( );
		assertEquals( "${project.basedir}/lib/JDBC-1.0.0.jar", jdbcDep.getSystemPath( ) );
		/* AOM-5541 now also checking for presence of outdated profiles */
		final List<Profile> profiles = pomModel.getProfiles( );
		assertTrue( profiles.isEmpty( ) );
	}

}
