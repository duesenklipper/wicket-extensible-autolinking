/*
 * Copyright (C) 2016-2017 Carl-Eric Menzel <cmenzel@wicketbuch.de>
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

import org.apache.wicket.request.resource.ResourceReference;

/**
 * Resolves paths with a particular prefix to ResourceReferences.
 */
abstract class ResourceResolver
{
	@Nonnull
	private final String urlPrefix;

	protected ResourceResolver(@Nonnull String urlPrefix)
	{
		if (!urlPrefix.endsWith(":/"))
		{
			urlPrefix += ":/";
		}
		this.urlPrefix = urlPrefix;
	}

	@Nonnull
	String getUrlPrefix()
	{
		return urlPrefix;
	}

	/**
	 * Resolves the path given to a ResourceReference.
	 *
	 * @param src path
	 * @return ResourceReference
	 */
	abstract ResourceReference resolve(String src);

	/**
	 * Resolves the path given to a ResourceReference that is able to handle autolinking in CSS files. For CSS files
	 * in the classpath, this will typically be {@link org.apache.wicket.request.resource.CssResourceReference}, which
	 * does this automatically. {@link ContextRootResolver}, for example, will instead return a custom reference,
	 * because {@link org.apache.wicket.request.resource.CssResourceReference} only works in the classpath.
	 * @param src path
	 * @return ResourceReference
	 */
	abstract ResourceReference resolveForCss(String src);

	/**
	 * Remove the prefix used by this resolver from the given string.
	 * @param src string
	 * @return stripped string
	 */
	@Nonnull
	protected String removePrefix(@Nonnull String src)
	{
		return src.substring(urlPrefix.length());
	}
}
