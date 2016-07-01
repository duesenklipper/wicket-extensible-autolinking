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

import javax.annotation.Nullable;

import de.wicketbuch.extensions.autolinking.res.Scope;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by calle on 30.06.16.
 */
public class ExtensibleAutolinkerTest
{
	@Nullable
	private static WicketTester tester;

	@Test
	public void regularAutolinkingWorks() throws Exception
	{
		tester.startPage(RegularAutolinkingPage.class);
		// img src autolinking
		tester.assertContains("src=\"../resource/de.wicketbuch.extensions.autolinking" +
				".ExtensibleAutolinkerTest\\$RegularAutolinkingPage/res/test.png\"");
		// stylesheet link href autolinking
		tester.assertContains("href=\"../resource/de.wicketbuch.extensions.autolinking" +
				".ExtensibleAutolinkerTest\\$RegularAutolinkingPage/res/test.css\"");
	}

	@Test
	public void regularAutolinkingInAutolinkedCssResource() throws Exception
	{
		tester.startPage(ClasspathRootAutolinkingPage.class);
		tester.executeUrl("/context/servlet/wicket/resource/_cp._/::/de/wicketbuch/extensions/autolinking/res/test" +
				".css");
		tester.assertContains("\\.regular.*background: url\\('\\./test\\.png'\\);");
	}

	@Test
	public void extendedAutolinkingInAutolinkedCssResource() throws Exception
	{
		tester.startPage(ClasspathRootAutolinkingPage.class);
		tester.executeUrl("/context/servlet/wicket/resource/_cp._/::/de/wicketbuch/extensions/autolinking/res/test.css");
		tester.assertContains(".ctxroot \\{ background: url\\('../../../../../../../org.apache.wicket" +
				".Application/res/test.png'\\); \\}");
	}

	@Test
	public void extendedAutolinkingInContextRootCssResource() throws Exception
	{
		// this needs special testing because wicket's org.apache.wicket.resource.CssUrlReplacer is only called
		// by org.apache.wicket.request.resource.CssPackageResource, which does not work with context resources.
		// ExtensibleAutolinker handles this by copying some of the logic from CssUrlReplacer and invokes it for
		// org.apache.wicket.request.resource.ContextRelativeResource.
		tester.startPage(ContextRootAutolinkingPage.class);
		tester.executeUrl("/context/servlet/wicket/resource/org.apache.wicket.Application/ctx:/res/test.css");
		tester.assertContains(".ctxroot \\{ background: url\\('../../res/test.png'\\); \\}");
	}

	@Test
	public void classpathRootAutolinking() throws Exception
	{
		tester.startPage(ClasspathRootAutolinkingPage.class);
		// img src autolinking
		tester.assertContains("src=\"../resource/_cp._/::/de/wicketbuch/extensions/autolinking/res/test.png\"");
		// stylesheet link href autolinking
		tester.assertContains("href=\"../resource/_cp._/::/de/wicketbuch/extensions/autolinking/res/test.css\"");
	}

	@Test
	public void contextRootAutolinking() throws Exception
	{
		tester.startPage(ContextRootAutolinkingPage.class);
		// contextroot resources are available with Application scope
		// img src autolinking
		tester.assertContains("src=\"../resource/org.apache.wicket.Application/res/test.png\"");
		// stylesheet link href autolinking
		tester.assertContains("href=\"../resource/org.apache.wicket.Application/ctx:/res/test.css\"");
	}

	@Test
	public void customScopeAutolinking() throws Exception
	{
		tester.startPage(CustomScopeAutolinkingPage.class);
		// contextroot resources are available with Application scope
		// img src autolinking
		tester.assertContains("src=\"../resource/de.wicketbuch.extensions.autolinking.res.Scope/test.png\"");
		// stylesheet link href autolinking
		tester.assertContains("href=\"../resource/de.wicketbuch.extensions.autolinking.res.Scope/test.css\"");
	}

	public static class RegularAutolinkingPage extends WebPage
	{
		// no code, just template, see html file
	}

	public static class ClasspathRootAutolinkingPage extends WebPage
	{
		// no code, just template, see html file
	}

	public static class ContextRootAutolinkingPage extends WebPage
	{
		// no code, just template, see html file
	}

	public static class CustomScopeAutolinkingPage extends WebPage
	{
		// no code, just template, see html file
	}


	@BeforeClass
	public static void setupTester()
	{
		tester = new WicketTester(new MockApplication()
		{
			@Override
			protected void init()
			{
				super.init();
				ExtensibleAutolinker autolinker = ExtensibleAutolinker.configure(this);
				autolinker.addScopePrefix(Scope.class, "testscope");
			}
		}, new File("src/test/webapp").getAbsolutePath());
	}

	@AfterClass
	public static void destroyTester()
	{
		tester.destroy();
		tester = null;
	}
}