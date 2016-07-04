/*
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manages {@link ResourceResolver}s.
 */
class ResourceResolvers
{
	private final Map<String, ResourceResolver> resolvers = new HashMap<String, ResourceResolver>();

	void add(@Nonnull ResourceResolver resolver)
	{
		String prefix = resolver.getUrlPrefix();
		resolvers.put(prefix, resolver);
	}

	@Nullable
	ResourceResolver getResolverForUrl(@Nullable String src)
	{
		if (src != null)
		{
			for (Entry<String, ResourceResolver> entry : resolvers.entrySet())
			{
				if (src.startsWith(entry.getKey()))
				{
					return entry.getValue();
				}
			}
		}
		return null;
	}
}
