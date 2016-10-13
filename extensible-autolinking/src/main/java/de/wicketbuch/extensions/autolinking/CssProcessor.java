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


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wicket.css.ICssCompressor;
import org.apache.wicket.request.cycle.RequestCycle;

/**
 * An {@link ICssCompressor} that uses {@link ExtensibleAutolinker} for extended autolinking.
 */
class CssProcessor implements ICssCompressor
{
	private static final Pattern URL_PATTERN = Pattern
			.compile("url\\([ ]*['|\"]?([^ ]*?)['|\"]?[ ]*\\)");

	@Nullable
	private final ICssCompressor originalCssCompressor;
	@Nonnull
	private final ResourceResolvers resolvers;

	CssProcessor(ICssCompressor originalCssCompressor, @Nonnull ResourceResolvers resolvers)
	{
		this.resolvers = resolvers;
		this.originalCssCompressor = originalCssCompressor;
	}

	@Nonnull
	@Override
	public String compress(String input)
	{
		if (originalCssCompressor != null)
		{
			input = originalCssCompressor.compress(input);
		}
		RequestCycle cycle = RequestCycle.get();
		Matcher matcher = URL_PATTERN.matcher(input);
		StringBuffer output = new StringBuffer();

		while (matcher.find())
		{
			final String urlString = matcher.group(1);
			CharSequence processedUrl = urlString;
			final ResourceResolver resolver = resolvers.getResolverForUrl(urlString);
			if (resolver != null)
			{
				processedUrl = cycle.urlFor(resolver.resolve(urlString), null);
			}
			matcher.appendReplacement(output, "url('" + processedUrl + "')");
		}
		matcher.appendTail(output);
		return output.toString();
	}
}
