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

import static org.apache.wicket.markup.parser.filter.WicketLinkTagHandler.AUTOLINK_ID;
import static org.apache.wicket.resource.CssUrlReplacer.EMBED_BASE64;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.css.ICssCompressor;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupElement;
import org.apache.wicket.markup.MarkupFactory;
import org.apache.wicket.markup.MarkupParser;
import org.apache.wicket.markup.MarkupResourceStream;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.parser.AbstractMarkupFilter;
import org.apache.wicket.markup.parser.IMarkupFilter;
import org.apache.wicket.markup.resolver.IComponentResolver;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CssUrlReplacer;
import org.apache.wicket.resource.IScopeAwareTextResourceProcessor;
import org.apache.wicket.response.ByteArrayResponse;
import org.apache.wicket.util.image.ImageUtil;

public class ExtensibleAutolinker
{
	private static final Pattern URL_PATTERN = Pattern
			.compile("url\\([ ]*['|\"]?([^ ]*?)['|\"]?[ ]*\\)");

	private CssProcessor getCssProcessor()
	{
		return cssProcessor;
	}

	private final CssProcessor cssProcessor;


	public static ExtensibleAutolinker configure(WebApplication application)
	{
		final ICssCompressor originalCssCompressor = application.getResourceSettings().getCssCompressor();
		final ExtensibleAutolinker autolinker = new ExtensibleAutolinker(originalCssCompressor);
		application.getMarkupSettings().setMarkupFactory(new MarkupFactory()
		{
			@Override
			public MarkupParser newMarkupParser(MarkupResourceStream resource)
			{
				final MarkupParser parser = super.newMarkupParser(resource);
				parser.add(autolinker.newMarkupFilter(resource));
				return parser;
			}
		});

		application.getPageSettings().addComponentResolver(autolinker.newComponentResolver());

		application.getResourceSettings().setCssCompressor(autolinker.getCssProcessor());

		application.getMarkupSettings().setAutomaticLinking(true);

		return autolinker;
	}

	class CssProcessor implements IScopeAwareTextResourceProcessor, ICssCompressor
	{
		private final ICssCompressor originalCssCompressor;

		CssProcessor(ICssCompressor originalCssCompressor)
		{
			if (originalCssCompressor instanceof CssUrlReplacer)
			{
				this.originalCssCompressor = null;
			}
			else
			{
				this.originalCssCompressor = originalCssCompressor;
			}

		}

		@Override
		public String process(String input, Class<?> scope, String name)
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
				CharSequence processedUrl = null;
				final String urlString = matcher.group(1);
				for (Entry<String, ResourceResolver> entry : resolvers.entrySet())
				{
					if (urlString.startsWith(entry.getKey()))
					{
						processedUrl = cycle.urlFor(entry.getValue().resolve(urlString), null);
						break;
					}
				}
				boolean embedded = false;
				// if we didn't find anything to resolve, but we have a scope, that means we can let the
				// original CssUrlReplacer logic run. If we don't have a scope, that means we are not in the classpath
				// but in the webapp context, where PackageResourceReferences don't work.
				if (processedUrl == null && scope != null)
				{
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

		@Override
		public String compress(String original)
		{
			return null;
		}
	}

	private IComponentResolver newComponentResolver()
	{
		return new IComponentResolver()
		{
			@Override
			public Component resolve(MarkupContainer container, MarkupStream markupStream, ComponentTag tag)
			{
				final String src;
				final String attribute;
				if (tag.getAttribute("src") != null)
				{
					src = tag.getAttribute("src");
					attribute = "src";
				}
				else
				{
					src = tag.getAttribute("href");
					attribute = "href";
				}

				if (src != null)
				{
					for (Entry<String, ResourceResolver> entry : resolvers.entrySet())
					{
						if (src.startsWith(entry.getKey()))
						{
							final ResourceReference resourceReference;
							if (tag.getName().equals("link") && "stylesheet".equals(tag.getAttribute("rel")))
							{
								resourceReference = entry.getValue().resolveForCss(src);
							}
							else
							{
								resourceReference = entry.getValue().resolve(src);
							}
							return new org.apache.wicket.markup.html.WebMarkupContainer(tag.getId())
							{
								@Override
								protected void onComponentTag(ComponentTag tag)
								{
									super.onComponentTag(tag);
									tag.put(attribute, urlFor(resourceReference, null));
								}
							};
						}
					}
				}
				return null;
			}
		};
	}

	private IMarkupFilter newMarkupFilter(MarkupResourceStream resource)
	{
		return new AbstractMarkupFilter(resource)
		{
			@Override
			protected MarkupElement onComponentTag(ComponentTag tag) throws ParseException
			{
				final String src = tag.getAttribute("src") != null ? tag.getAttribute("src") : tag.getAttribute("href");
				if (src != null)
				{
					for (String prefix : resolvers.keySet())
					{
						if (src.startsWith(prefix))
						{
							tag.setAutoComponentTag(true);
							tag.setModified(true);
							tag.setId(AUTOLINK_ID + getRequestUniqueId());
							break;
						}
					}
				}
				return tag;
			}
		};
	}

	private final Map<String, ResourceResolver> resolvers = new HashMap<>();

	private ExtensibleAutolinker(ICssCompressor originalCssCompressor)
	{
		resolvers.put("cp:/", new ClasspathRootResolver());
		resolvers.put("ctx:/", new ContextRootResolver());
		cssProcessor = new CssProcessor(originalCssCompressor);
	}

	public ExtensibleAutolinker addScopePrefix(Class<?> scope, String prefix)
	{
		prefix = prefix + ":/";
		resolvers.put(prefix, new ClasspathResolver(scope, prefix));
		return this;
	}


	private interface ResourceResolver
	{
		ResourceReference resolve(String src);

		ResourceReference resolveForCss(String src);
	}

	private static class ClasspathResolver implements ResourceResolver
	{
		private final Class<?> scope;
		private final String prefix;

		ClasspathResolver(Class<?> scope, String prefix)
		{
			this.scope = scope;
			this.prefix = prefix;
		}

		@Override
		public ResourceReference resolve(String src)
		{
			return new PackageResourceReference(scope, src.substring(prefix.length()));
		}

		@Override
		public ResourceReference resolveForCss(String src)
		{
			return new CssResourceReference(scope, src.substring(prefix.length()));
		}
	}

	private static class ClasspathRootResolver implements ResourceResolver
	{
		@Override
		public ResourceReference resolve(String src)
		{
			return new PackageResourceReference(_cp._.class, "../" + src.substring(4));
		}

		@Override
		public ResourceReference resolveForCss(String src)
		{
			return new CssResourceReference(_cp._.class, "../" + src.substring(4));
		}
	}

	private class ContextRootResolver implements ResourceResolver
	{
		private final Charset UTF8 = Charset.forName("UTF-8");

		@Override
		public ResourceReference resolve(String src)
		{
			return new ContextRelativeResourceReference(src.substring(5));
		}

		@Override
		public ResourceReference resolveForCss(final String src)
		{
			return new ResourceReference(src)
			{
				@Override
				public IResource getResource()
				{
					return new ContextRelativeResource(src.substring(5))
					{
						@Override
						protected ResourceResponse newResourceResponse(Attributes attributes)
						{
							final ResourceResponse rr = super.newResourceResponse(attributes);

							final WriteCallback wrappedWriteCallback = rr.getWriteCallback();

							rr.setWriteCallback(new WriteCallback()
							{
								@Override
								public void writeData(Attributes attributes) throws IOException
								{
									final ByteArrayResponse buffer = new ByteArrayResponse();
									wrappedWriteCallback.writeData(new Attributes(attributes.getRequest(), buffer, attributes.getParameters()));
									final String css = new String(buffer.getBytes(), UTF8);
									final String processedCss = cssProcessor.process(css, null, src);
									attributes.getResponse().write(processedCss);
								}
							});

							return rr;
						}
					};
				}
			};
		}
	}
}
