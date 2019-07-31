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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Goal to release a module
 *
 * @author thum
 */
@Mojo( name = "release", defaultPhase = LifecyclePhase.NONE )
public class ReleaseNMMojo extends AbstractReleaseStateMojo
{
	@Override
	protected boolean isRelease( )
	{
		return true;
	}
}
