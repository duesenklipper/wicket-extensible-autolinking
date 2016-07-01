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

import java.text.ParseException;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.css.ICssCompressor;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupElement;
import org.apache.wicket.markup.MarkupFactory;
import org.apache.wicket.markup.MarkupParser;
import org.apache.wicket.markup.MarkupResourceStream;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.parser.AbstractMarkupFilter;
import org.apache.wicket.markup.parser.IMarkupFilter;
import org.apache.wicket.markup.resolver.IComponentResolver;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.ResourceReference;

public class ExtensibleAutolinker
{
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
		application.getResourceSettings().setCssCompressor(autolinker.cssProcessor);
		application.getMarkupSettings().setAutomaticLinking(true);

		return autolinker;
	}

	private IComponentResolver newComponentResolver()
	{
		return new IComponentResolver()
		{
			@Override
			public Component resolve(MarkupContainer container, MarkupStream markupStream, ComponentTag tag)
			{
				String src = tag.getAttribute("src");
				final String attribute;
				if (src != null)
				{
					attribute = "src";
				}
				else
				{
					src = tag.getAttribute("href");
					attribute = "href";
				}

				final ResourceResolver resolver = resolvers.getResolverForUrl(src);
				if (resolver != null)
				{
					final ResourceReference resourceReference;
					if (tag.getName().equals("link") && "stylesheet".equals(tag.getAttribute("rel")))
					{
						resourceReference = resolver.resolveForCss(src);
					}
					else
					{
						resourceReference = resolver.resolve(src);
					}
					return new WebMarkupContainer(tag.getId())
					{
						@Override
						protected void onComponentTag(ComponentTag tag)
						{
							super.onComponentTag(tag);
							tag.put(attribute, urlFor(resourceReference, null));
						}
					};
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
				final ResourceResolver resolver = resolvers.getResolverForUrl(src);
				if (resolver != null)
				{
					tag.setAutoComponentTag(true);
					tag.setModified(true);
					tag.setId(AUTOLINK_ID + getRequestUniqueId());
				}
				return tag;
			}
		};
	}

	private ResourceResolvers resolvers = new ResourceResolvers();

	private ExtensibleAutolinker(ICssCompressor originalCssCompressor)
	{
		cssProcessor = new CssProcessor(originalCssCompressor, resolvers);
		resolvers.add(new ClasspathRootResolver());
		resolvers.add(new ContextRootResolver(cssProcessor));
	}

	public ExtensibleAutolinker addScopePrefix(Class<?> scope, String prefix)
	{
		resolvers.add(new ClasspathResolver(scope, prefix));
		return this;
	}
}
