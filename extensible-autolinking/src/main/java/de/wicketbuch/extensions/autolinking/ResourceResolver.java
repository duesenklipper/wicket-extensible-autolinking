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

import org.apache.wicket.request.resource.ResourceReference;

/**
 * Created by calle on 01.07.16.
 */
abstract class ResourceResolver
{
	private final String urlPrefix;

	protected ResourceResolver(String urlPrefix)
	{
		if (!urlPrefix.endsWith(":/"))
		{
			urlPrefix += ":/";
		}
		this.urlPrefix = urlPrefix;
	}

	String getUrlPrefix()
	{
		return urlPrefix;
	}

	abstract ResourceReference resolve(String src);

	abstract ResourceReference resolveForCss(String src);

	protected String removePrefix(String src)
	{
		return src.substring(urlPrefix.length());
	}
}
