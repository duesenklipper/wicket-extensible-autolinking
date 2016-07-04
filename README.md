# Extensible Autolinking for Wicket

Wicket can autolink resources referenced in HTML markup files, for example images and CSS files that are reachable relative to the markup file.

This only works with resources on the classpath, not in the webapp context (the "webapp folder"). Generally, one should indeed use classpath resources instead of context resources, because that way components are much more easily packageable. There are situations where one might want to still reference context resources, such as migration scenarios.

Unfortunately, Wicket autolinking also only works with relative paths, because absolute paths (starting with "`/`") are rejected. Again, in general, this is a good thing, because resource files should live close to the components using them. There can be commonly used files though, like basic icons (arrow up, down, open, close, ok, cancel, etc) that are used in many places. It can be practical to put these in a common package near the root of the classpath, for example `/resources/img/` or something like that.

The Extensible Autolinker allows all the normal operations that Wicket autolinking provides plus a few more. To distinguish the new variants, they all use path prefixes:

* Refer to classpath resources starting from the classpath root by prefixing with `cp:/`
* Resources paths in the the context root ("webapp folder") are prefixed with `ctx:/`
* Define custom prefixes starting at arbitrary packages in your classpath.

## How to use
To activate, simply configure `ExtensibleAutolinker` in your `Application#init()` method like this:

	ExtensibleAutolinker.configure(this);

This method also returns the instance of ExtensibleAutolinker that you can then use to add additional classpath scopes:

	autolinker.addScopePrefix(SomeClass.class, "sc");

You can now refer to paths relative to SomeClass in the classpath by using the prefix "`sc:/`".
