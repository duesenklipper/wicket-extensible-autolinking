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

import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

/**
 * Created by calle on 01.07.16.
 */
class ClasspathResolver extends ResourceResolver
{
	private final Class<?> scope;

	ClasspathResolver(Class<?> scope, String prefix)
	{
		super(prefix);
		this.scope = scope;
	}

	@Override
	public ResourceReference resolve(String src)
	{
		return new PackageResourceReference(scope, removePrefix(src));
	}

	@Override
	public ResourceReference resolveForCss(String src)
	{
		return new CssResourceReference(scope, removePrefix(src));
	}
}
