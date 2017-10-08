**Current version**: 1.3.0.wicket{6|7} for Wicket 6.x, 7.x ([CHANGELOG](CHANGELOG-1.x.md))

# Extensible Autolinking for Wicket

Wicket can autolink resources referenced in HTML markup files, for example
images and CSS files that are reachable relative to the markup file.

This only works with resources on the classpath, not in the webapp context (the
"webapp folder"). Generally, one should indeed use classpath resources instead
of context resources, because that way components are much more easily
packageable. There are situations where one might want to still reference
context resources, such as migration scenarios where some resources might still
need to live in the webapp context for the sake of legacy components.

You only get to use the predefined tags and attributes for the autolinking, such as 
```img```/```src``` or ```a```/```href```. With Extensible Autolinker, you can add
your own tag/attribute combinations, for example ```object```/```data```.

Unfortunately, Wicket autolinking also only works with relative paths, because
absolute paths (starting with "`/`") are rejected. Again, in general, this is a
good thing, because resource files _should_ live close to the components using
them. There can be commonly used files though, like basic icons (arrow up, down,
open, close, ok, cancel, etc) that are used in many places. It can be practical
to put these in a common package near the root of the classpath, for example
`/resources/img/` or something like that.

The Extensible Autolinker allows all the normal operations that Wicket
autolinking provides plus a few more. To distinguish the new variants, they all
use path prefixes:

* Refer to classpath resources starting from the classpath root by prefixing
  with `cp:/`
* Resources paths in the the context root ("webapp folder") are prefixed
  with `ctx:/`
* Define custom prefixes starting at arbitrary packages in your classpath.

All this is done in HTML and CSS files.

Autolinking of `url()` in CSS files is only partially available in the version
for Wicket 1.5.x, because Wicket's own `CssUrlReplacer` is only available in
Wicket 6. Only autolinking of resources with a prefix known by
ExtensibleAutolinker will work in Wicket 1.5.x.

## How to use
To activate, simply configure `ExtensibleAutolinker` in your
`Application#init()` method like this:

	ExtensibleAutolinker.configure(this);

This method also returns the instance of ExtensibleAutolinker that you can then
use to add additional classpath scopes:

	ExtensibleAutolinker autolinker = ExtensibleAutolinker.configure(this);
	autolinker.addScopePrefix(SomeClass.class, "sc");

You can now refer to paths relative to SomeClass in the classpath by using the
prefix "`sc:/`", in addition to the.

You can also add your own tag/attribute combinations:

    autolinker.setAttributesFor("object", "data");

## Maven coordinates

    <dependency>
        <groupId>de.wicketbuch.extensions</groupId>
        <artifactId>extensible-autolinking</artifactId>
        <version>...</version>
    </dependency>

Make sure you choose the correct version for the version of Wicket you are
using, they are suffixed with `.wicket5`, `.wicket6`, `.wicket7` respectively.

This project uses [Semantic Versioning](http://semver.org/), so you can rely on
things not breaking within a major version.
