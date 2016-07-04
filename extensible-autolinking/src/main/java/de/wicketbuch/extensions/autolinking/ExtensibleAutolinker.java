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

import static org.apache.wicket.markup.parser.filter.WicketLinkTagHandler.AUTOLINK_ID;

import java.text.ParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.wicket.Application;
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

/**
 * The {@link ExtensibleAutolinker} allows additional autolinking in HTML and CSS files. Traditional Wicket autolinking
 * (activated via {@link org.apache.wicket.settings.MarkupSettings#setAutomaticLinking(boolean)} or
 * <code>&lt;wicket:link&gt;</code>) allows only relative paths to resources in the classpath. Usually, this is
 * sufficient, though sometimes you may have resources like icon graphics that you want to reference from many different
 * places in your application. In this case relative paths can be cumbersome and brittle.
 * <p>
 * ExtensibleAutolinker provides three new autolinking options:
 * <ul>
 * <li>Absolute paths, starting from the classpath root, with the prefix <code>cp:/</code>.</li>
 * <li>Absolute paths, starting from the context root (your "webapp folder"), with the prefix
 * <code>ctx:/</code></li>
 * <li>Relative paths, starting from an arbitrary class in your classpath, with an arbitrary prefix.</li>
 * </ul>
 * To activate, simply configure ExtensibleAutolinker in your {@link Application#init()} method like this:
 * <pre>ExtensibleAutolinker.configure(this);</pre>
 * <p>
 * This method also returns the instance of ExtensibleAutolinker that you can then use to add additional classpath
 * scopes:
 * <pre>autolinker.addScopePrefix(SomeClass.class, "sc");</pre>
 * You can now refer to paths relative to SomeClass in the classpath by using the prefix "sc:/".
 * <p>
 * ExtensibleAutolinker also activates autolinking in CSS files, replacing all <code>url(...)</code> URLs with the
 * proper references. If you use a custom {@link ICssCompressor}, make sure you set it <em>before</em> activating
 * ExtensibleAutolinker.
 */
public class ExtensibleAutolinker
{
	@Nonnull
	private final CssProcessor cssProcessor;

	/**
	 * Activate the ExtensibleAutolinker for the given Wicket application.
	 *
	 * @param application Your application
	 * @return an instance of ExtensibleAutolinker which you can use to add more scopes.
	 */
	@Nonnull
	public static ExtensibleAutolinker configure(@Nonnull WebApplication application)
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

	/**
	 * @return an {@link IComponentResolver} which actually resolves the paths to proper URLs.
	 */
	@Nullable
	private IComponentResolver newComponentResolver()
	{
		return new IComponentResolver()
		{
			@Nullable
			@Override
			public Component resolve(MarkupContainer container, MarkupStream markupStream, @Nonnull ComponentTag tag)
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
						protected void onComponentTag(@Nonnull ComponentTag tag)
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

	/**
	 * @return a {@link IMarkupFilter} which marks HTML tags that have resolvable src or href attributes, so that
	 * the {@link IComponentResolver} from {@link #newComponentResolver()} can then resolve them.
	 */
	@Nullable
	private IMarkupFilter newMarkupFilter(MarkupResourceStream resource)
	{
		return new AbstractMarkupFilter(resource)
		{
			@Nonnull
			@Override
			protected MarkupElement onComponentTag(@Nonnull ComponentTag tag) throws ParseException
			{
				final String src = tag.getAttribute("src") != null ? tag.getAttribute("src") : tag.getAttribute("href");
				final ResourceResolver resolver = resolvers.getResolverForUrl(src);
				if (resolver != null)
				{
					tag.setAutoComponentTag(true);
					tag.setModified(true);
					tag.setId(AUTOLINK_ID);
				}
				return tag;
			}
		};
	}

	@Nonnull
	private ResourceResolvers resolvers = new ResourceResolvers();

	private ExtensibleAutolinker(ICssCompressor originalCssCompressor)
	{
		cssProcessor = new CssProcessor(originalCssCompressor, resolvers);
		resolvers.add(new ClasspathRootResolver());
		resolvers.add(new ContextRootResolver(cssProcessor));
	}

	/**
	 * Add an extra scope to the autolinker.
	 * @param scope The class to use as the starting point for relative paths.
	 * @param prefix The prefix used to recognize the paths resolvable in this scope. For example, passing
	 *                  <code>"myscope"</code> here will let you use paths prefixed with <code>myscope:/</code>.
	 * @return <code>this</code>, for method chaining.
	 */
	@Nonnull
	public ExtensibleAutolinker addScopePrefix(Class<?> scope, @Nonnull String prefix)
	{
		resolvers.add(new ClasspathResolver(scope, prefix));
		return this;
	}
}
