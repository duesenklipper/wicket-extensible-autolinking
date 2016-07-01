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

import static org.apache.wicket.resource.CssUrlReplacer.EMBED_BASE64;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.css.ICssCompressor;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.resource.CssUrlReplacer;
import org.apache.wicket.resource.IScopeAwareTextResourceProcessor;
import org.apache.wicket.util.image.ImageUtil;

/**
 * An {@link ICssCompressor} that uses {@link ExtensibleAutolinker} for extended autolinking. Unfortunately, the default
 * {@link CssUrlReplacer} shipped with Wicket is not very easily extensible, so parts of that class are copied here to
 * provide both "classic" and extended autolinking.
 */
class CssProcessor implements IScopeAwareTextResourceProcessor, ICssCompressor
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
		if (originalCssCompressor instanceof CssUrlReplacer)
		{
			// do not let original CssUrlReplacer run, because we duplicate its logic in this class.
			this.originalCssCompressor = null;
		}
		else
		{
			this.originalCssCompressor = originalCssCompressor;
		}

	}

	@Nonnull
	@Override
	public String process(String input, @Nullable Class<?> scope, @Nonnull String name)
	{
		if (originalCssCompressor instanceof IScopeAwareTextResourceProcessor)
		{
			input = ((IScopeAwareTextResourceProcessor) originalCssCompressor).process(input, scope, name);
		}
		else if (originalCssCompressor != null)
		{
			input = originalCssCompressor.compress(input);
		}
		RequestCycle cycle = RequestCycle.get();
		Url cssUrl = Url.parse(name);
		Matcher matcher = URL_PATTERN.matcher(input);
		StringBuffer output = new StringBuffer();

		while (matcher.find())
		{
			final String urlString = matcher.group(1);
			CharSequence processedUrl = urlString;
			final ResourceResolver resolver = resolvers.getResolverForUrl(urlString);
			boolean embedded = false;
			if (resolver != null)
			{
				processedUrl = cycle.urlFor(resolver.resolve(urlString), null);
			}
			else if (scope != null)
			{
				// if we didn't find anything to resolve, but we have a scope, that means we can let the
				// original CssUrlReplacer logic run. If we don't have a scope, that means we are not in the classpath
				// but in the webapp context, where PackageResourceReferences don't work.

				Url imageCandidateUrl = Url.parse(urlString);

				if (imageCandidateUrl.isFull())
				{
					processedUrl = imageCandidateUrl.toString(Url.StringMode.FULL);
				}
				else if (imageCandidateUrl.isContextAbsolute())
				{
					processedUrl = imageCandidateUrl.toString();
				}
				else
				{
					// relativize against the url for the containing CSS file
					Url cssUrlCopy = new Url(cssUrl);
					cssUrlCopy.resolveRelative(imageCandidateUrl);

					// if the image should be processed as URL or base64 embedded
					if (cssUrlCopy.getQueryString() != null
							&& cssUrlCopy.getQueryString().contains(EMBED_BASE64))
					{
						embedded = true;
						PackageResourceReference imageReference = new PackageResourceReference(scope,
								cssUrlCopy.toString().replace("?" + EMBED_BASE64, ""));
						try
						{
							processedUrl = ImageUtil.createBase64EncodedImage(imageReference, true);
						} catch (Exception e)
						{
							throw new WicketRuntimeException(
									"Error while embedding an image into the css: " + imageReference, e);
						}
					}
					else
					{
						PackageResourceReference imageReference = new PackageResourceReference(scope,
								cssUrlCopy.toString());
						processedUrl = cycle.urlFor(imageReference, null);
					}

				}
			}
			matcher.appendReplacement(output,
					embedded ? "url(" + processedUrl + ")" : "url('" + processedUrl + "')");
		}
		matcher.appendTail(output);
		return output.toString();
	}

	@Nullable
	@Override
	public String compress(String original)
	{
		return null;
	}
}
