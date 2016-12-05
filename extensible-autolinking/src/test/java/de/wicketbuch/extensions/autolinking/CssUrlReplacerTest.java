/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.caching.FilenameWithVersionResourceCachingStrategy;
import org.apache.wicket.request.resource.caching.IStaticCacheableResource;
import org.apache.wicket.request.resource.caching.ResourceUrl;
import org.apache.wicket.request.resource.caching.version.MessageDigestResourceVersion;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;

/**
 * Adapted from wicket-core to test the CssUrlReplacer-parts of CssProcessor.
 * Used according to the Apache2 license.
 */
public class CssUrlReplacerTest
{
	private static final String DECORATION_SUFFIX = "--decorated";

	@Before
	public void setUp() throws Exception
	{
		new WicketTester(newApplication());
	}

	private WebApplication newApplication()
	{
		return new MockApplication()
		{
			@Override
			protected void init()
			{
				super.init();

				getResourceSettings().setCachingStrategy(
					new FilenameWithVersionResourceCachingStrategy("=VER=",
						new MessageDigestResourceVersion())
					{
						@Override
						public void decorateUrl(ResourceUrl url, IStaticCacheableResource resource)
						{
							url.setFileName(url.getFileName() + DECORATION_SUFFIX);
						}
					});
			}
		};
	}

	@Test
	public void doNotProcessFullUrls()
	{
		String input = ".class {background-image: url('http://example.com/some.img');}";
		Class<?> scope = CssUrlReplacerTest.class;
		String cssRelativePath = "res/css/some.css";
		CssProcessor replacer = newProcessor();

		String processed = replacer.process(input, scope, cssRelativePath);
		assertThat(processed, is(input));
	}

	@Test
	public void doNotProcessDataUrls_WICKET_6290()
	{
		String input = ".class {background-image: url(data:image/gif;base64,R0lGODlhEAAQAMQAAORHH);}";
		Class<?> scope = CssUrlReplacerTest.class;
		String cssRelativePath = "res/css/some.css";
		CssProcessor replacer = newProcessor();

		String processed = replacer.process(input, scope, cssRelativePath);
		assertThat(processed, is(input));
	}

	private CssProcessor newProcessor()
	{
		return ExtensibleAutolinker.configure(WebApplication.get()).cssProcessor;
	}

	@Test
	public void doNotProcessContextAbsoluteUrls()
	{
		String input = ".class {background-image: url('/some.img');}";
		Class<?> scope = CssUrlReplacerTest.class;
		String cssRelativePath = "res/css/some.css";
		CssProcessor replacer = newProcessor();

		String processed = replacer.process(input, scope, cssRelativePath);
		assertThat(processed, is(input));
	}

	@Test
	public void sameFolderSingleQuotes()
	{
		String input = ".class {background-image: url('some.img');}";
		Class<?> scope = CssUrlReplacerTest.class;
		String cssRelativePath = "res/css/some.css";
		CssProcessor replacer = newProcessor();

		String processed = replacer.process(input, scope, cssRelativePath);
		assertThat(
			processed,
			is(".class {background-image: url('./wicket/resource/de.wicketbuch.extensions.autolinking.CssUrlReplacerTest/res/css/some.img" +
				DECORATION_SUFFIX + "');}"));
	}

	@Test
	public void sameFolderDoubleQuotes()
	{
		String input = ".class {background-image: url(\"some.img\");}";
		Class<?> scope = CssUrlReplacerTest.class;
		String cssRelativePath = "res/css/some.css";
		CssProcessor replacer = newProcessor();

		String processed = replacer.process(input, scope, cssRelativePath);
		assertThat(
			processed,
			is(".class {background-image: url('./wicket/resource/de.wicketbuch.extensions.autolinking.CssUrlReplacerTest/res/css/some.img" +
				DECORATION_SUFFIX + "');}"));
	}

	@Test
	public void parentFolderAppendFolder()
	{
		String input = ".class {background-image: url('../images/some.img');}";
		Class<?> scope = CssUrlReplacerTest.class;
		String cssRelativePath = "res/css/some.css";
		CssProcessor replacer = newProcessor();

		String processed = replacer.process(input, scope, cssRelativePath);
		assertThat(
			processed,
			is(".class {background-image: url('./wicket/resource/de.wicketbuch.extensions.autolinking.CssUrlReplacerTest/res/images/some.img" +
				DECORATION_SUFFIX + "');}"));
	}

	@Test
	public void sameFolderAppendFolder()
	{
		String input = ".class {background-image: url('./images/some.img');}";
		Class<?> scope = CssUrlReplacerTest.class;
		String cssRelativePath = "res/css/some.css";
		CssProcessor replacer = newProcessor();

		String processed = replacer.process(input, scope, cssRelativePath);
		assertThat(
			processed,
			is(".class {background-image: url('./wicket/resource/de.wicketbuch.extensions.autolinking.CssUrlReplacerTest/res/css/images/some.img" +
				DECORATION_SUFFIX + "');}"));
	}

	@Test
	public void base64EncodedImage()
	{
		String input = ".class {background-image: url('Beer.gif?embedBase64');}";
		Class<?> scope = CssUrlReplacerTest.class;
		String cssRelativePath = "some.css";
		CssProcessor replacer = newProcessor();
		String processed = replacer.process(input, scope, cssRelativePath);
		assertThat(
			processed,
			containsString(".class {background-image: url(data:image/gif;base64,R0lGODlh1wATAXAAACH5BAEAAP8ALAAAAADXA"));
	}

	@Test
	public void severalUrls()
	{
		String input = ".class {\n" + "a: url('../images/a.img');\n" + "b: url('./b.img');\n" + "}";
		Class<?> scope = CssUrlReplacerTest.class;
		String cssRelativePath = "res/css/some.css";
		CssProcessor replacer = newProcessor();

		String processed = replacer.process(input, scope, cssRelativePath);
		assertThat(processed, containsString("CssUrlReplacerTest/res/images/a.img" +
			DECORATION_SUFFIX + "');"));
		assertThat(processed, containsString("CssUrlReplacerTest/res/css/b.img" +
			DECORATION_SUFFIX + "');"));
	}
}
