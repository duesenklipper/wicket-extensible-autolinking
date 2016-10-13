/**
 * Copyright (C) 2016 Carl-Eric Menzel <cmenzel@wicketbuch.de>
 * and possibly other extensible-autolinking contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.wicketbuch.extensions.autolinking;

import javax.annotation.Nonnull;

import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * ResourceResolver working from the classpath root. Prefix "cp:/".
 */
class ClasspathRootResolver extends ResourceResolver
{

	ClasspathRootResolver()
	{
		super("cp:/");
	}

	@Nonnull
	@Override
	public ResourceReference resolve(@Nonnull String src)
	{
		return new PackageResourceReference(_cp._.class, "../" + removePrefix(src));
	}

	@Nonnull
	@Override
	public ResourceReference resolveForCss(@Nonnull String src)
	{
		return new CssResourceReference(_cp._.class, "../" + removePrefix(src));
	}
}
