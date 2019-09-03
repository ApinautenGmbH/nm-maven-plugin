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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Goal to unrelease a module
 *
 * @author thum
 */
@Mojo( name = "unrelease", defaultPhase = LifecyclePhase.NONE )
public class UnreleaseNMMojo extends AbstractReleaseStateMojo
{
	@Override
	protected boolean isRelease( )
	{
		return false;
	}
}
