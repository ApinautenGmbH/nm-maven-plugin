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

import java.util.regex.Pattern;

/**
 * Helper class for comparing version strings
 *
 * @author andreas
 */
public class VersionCompareHelper
{
	final static Pattern PATTERN_FULLVERSION = Pattern.compile( "^(\\d+).(\\d+).(\\d+).(\\d+).*" );
	final static Pattern PATTERN_MEDIUMVERSION = Pattern.compile( "^(\\d+).(\\d+).(\\d+).*" );
	final static Pattern PATTERN_SMALLVERSION = Pattern.compile( "^(\\d+).(\\d+).*" );

	/**
	 * Returns true if the given version number is older than the existing schema
	 *
	 * @param major1 existing/current major version
	 * @param minor1 existing/current minor version
	 * @param hotfix1 existing/current hotfix version
	 * @param patchLevel1 existing/current patchlevel
	 * @param major2 given major number
	 * @param minor2 given minor version
	 * @param hotfix2 given hotfix version
	 * @param patchLevel2 given patchlevel
	 *
	 * @return TRUE if the given version number is older than the existing schema
	 */
	public static boolean isOlder( final int major1, final int minor1, final int hotfix1, final int patchLevel1,
		final int major2, final int minor2, final int hotfix2, final int patchLevel2 )
	{
		return major2 < major1 ||
			major2 == major1 && minor2 < minor1 ||
			major2 == major1 && minor2 == minor1 && hotfix2 < hotfix1 ||
			major2 == major1 && minor2 == minor1 && hotfix2 == hotfix1 && patchLevel2 < patchLevel1;
	}

	/**
	 * Returns true if the given version number is older or equal to the existing schema
	 *
	 * @param major1 existing/current major version
	 * @param minor1 existing/current minor version
	 * @param hotfix1 existing/current hotfix version
	 * @param patchLevel1 existing/current patchlevel
	 * @param major2 given major number
	 * @param minor2 given minor version
	 * @param hotfix2 given hotfix version
	 * @param patchLevel2 given patchlevel
	 *
	 * @return TRUE if the given version number is older or equal to the existing schema
	 */
	public static boolean isOlderOrEqual( final int major1, final int minor1, final int hotfix1, final int patchLevel1,
		final int major2, final int minor2, final int hotfix2, final int patchLevel2 )
	{
		return isOlder( major1, minor1, hotfix1, patchLevel1, major2, minor2, hotfix2, patchLevel2 ) ||
			isEqual( major1, minor1, hotfix1, patchLevel1, major2, minor2, hotfix2, patchLevel2 );
	}

	/**
	 * Returns true if the given version number is equal to the existing schema
	 *
	 * @param major1 existing/current major version
	 * @param minor1 existing/current minor version
	 * @param hotfix1 existing/current hotfix version
	 * @param patchLevel1 existing/current patchlevel
	 * @param major2 given major number
	 * @param minor2 given minor version
	 * @param hotfix2 given hotfix version
	 * @param patchLevel2 given patchlevel
	 *
	 * @return TRUE if the given version number is older than the existing schema
	 */
	public static boolean isEqual( final int major1, final int minor1, final int hotfix1, final int patchLevel1,
		final int major2, final int minor2, final int hotfix2, final int patchLevel2 )
	{
		return major1 == major2 && minor1 == minor2 && hotfix1 == hotfix2 && patchLevel1 == patchLevel2;
	}

	/**
	 * Returns true if the given version number is newer than the existing schema
	 *
	 * @param major1 existing/current major version
	 * @param minor1 existing/current minor version
	 * @param hotfix1 existing/current hotfix version
	 * @param patchLevel1 existing/current patchlevel
	 * @param major2 given major number
	 * @param minor2 given minor version
	 * @param hotfix2 given hotfix version
	 * @param patchLevel2 given patchlevel
	 *
	 * @return TRUE if the given version number is newer than the existing schema
	 */
	public static boolean isNewer( final int major1, final int minor1, final int hotfix1, final int patchLevel1,
		final int major2, final int minor2, final int hotfix2, final int patchLevel2 )
	{
		return major2 > major1 ||
			major2 == major1 && minor2 > minor1 ||
			major2 == major1 && minor2 == minor1 && hotfix2 > hotfix1 ||
			major2 == major1 && minor2 == minor1 && hotfix2 == hotfix1 && patchLevel2 > patchLevel1;
	}

	/**
	 * Returns true if the given version number is newer or equal to the existing schema
	 *
	 * @param major1 existing/current major version
	 * @param minor1 existing/current minor version
	 * @param hotfix1 existing/current hotfix version
	 * @param patchLevel1 existing/current patchlevel
	 * @param major2 given major number
	 * @param minor2 given minor version
	 * @param hotfix2 given hotfix version
	 * @param patchLevel2 given patchlevel
	 *
	 * @return TRUE if if the given version number is newer or equal to the existing schema
	 */
	public static boolean isNewerOrEqual( final int major1, final int minor1, final int hotfix1, final int patchLevel1,
		final int major2, final int minor2, final int hotfix2, final int patchLevel2 )
	{
		return isNewer( major1, minor1, hotfix1, patchLevel1, major2, minor2, hotfix2, patchLevel2 ) ||
			isEqual( major1, minor1, hotfix1, patchLevel1, major2, minor2, hotfix2, patchLevel2 );
	}

	/**
	 * Parses a string to int array (major, minor, hofix)
	 *
	 * @param versionString
	 * @return int array (major, minor, hofix)
	 */
	@SuppressWarnings( "PMD.SystemPrintln" )
	public static int[ ] parseNumbers( final String versionString )
	{
		java.util.regex.Matcher m = PATTERN_FULLVERSION.matcher( versionString );

		try
		{
			if ( m.matches( ) )
			{
				final int major = Integer.parseInt( m.group( 1 ) );
				final int minor = Integer.parseInt( m.group( 2 ) );
				final int hotfix = Integer.parseInt( m.group( 3 ) );
				final int patch = Integer.parseInt( m.group( 4 ) );
				return new int[ ] { major, minor, hotfix, patch };
			}

			m = PATTERN_MEDIUMVERSION.matcher( versionString );
			if ( m.matches( ) )
			{
				final int major = Integer.parseInt( m.group( 1 ) );
				final int minor = Integer.parseInt( m.group( 2 ) );
				final int hotfix = Integer.parseInt( m.group( 3 ) );
				return new int[ ] { major, minor, hotfix, 0 };
			}

			m = PATTERN_SMALLVERSION.matcher( versionString );
			if ( m.matches( ) )
			{
				final int major = Integer.parseInt( m.group( 1 ) );
				final int minor = Integer.parseInt( m.group( 2 ) );
				return new int[ ] { major, minor, 0, 0 };
			}
			return null;
		}
		catch ( final Exception e )
		{
			System.out.println( "Could not parse version numbers of string '" + versionString + "'" );
		}
		return null;
	}
}
