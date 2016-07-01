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

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.response.ByteArrayResponse;

/**
 * Created by calle on 01.07.16.
 */
class ContextRootResolver extends ResourceResolver
{
	private final Charset UTF8 = Charset.forName("UTF-8");

	private final CssProcessor cssProcessor;

	ContextRootResolver(CssProcessor cssProcessor)
	{
		super("ctx");
		this.cssProcessor = cssProcessor;
	}

	@Override
	public ResourceReference resolve(String src)
	{
		return new ContextRelativeResourceReference(removePrefix(src));
	}

	@Override
	public ResourceReference resolveForCss(final String src)
	{
		return new ResourceReference(src)
		{
			@Override
			public IResource getResource()
			{
				return new ContextRelativeResource(removePrefix(src))
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
