# Install4J updates.xml Merger

A simple Maven plugin to take several `update.xml` files produced by [Install4J](https://www.ej-technologies.com/products/install4j/overview.html) for a 
particular project and merge them into a single file.

This plugin came about due to the need to build multiple different installers on different machines for
different platforms. 

Todays modern software deployments requirements are become a lot more onerous for a Java developer,
and in particular in CI environments.

Many code signing authorities are requiring the keys for Windows signing are stored on hardware devices, 
introducing difficulties signing on platforms other than Windows. Apple have similar requirements for
their application notarisation. 

We make use of Install4J's update mechanism, which relies on `updates.xml` files that contains meta-data 
required for this to work. However, compiling an Install4J project produces an XML file only for the platforms
that are being built. 

So we must (in our `Jenkinsfile`) build on each of our supported platform, then merge the XML produced for
each platform the create the final `updates.xml` that is deployed to the public file repository.

## Obtaining    

Available on Maven Central. Adjust for your build system.

```xml
<artifact>
	<groupId>com.sshtools</groupId>
	<artifactId>updatesxmlmerger-maven-plugin</artifactId>
	<version>1.0.0</version>
</artifact>
```

## Usage

There is no other documentation for this plugin, but the following should give you a good idea how to use
it.

```xml
<plugin>
	<groupId>com.sshtools</groupId>
	<artifactId>updatesxmlmerger-maven-plugin</artifactId>
	<version>1.0.0</version>
	<configuration>
		<inputs>
			<input>${project.build.directory}/media-macos/updates.xml</input>
			<input>${project.build.directory}/media-windows/updates.xml</input>
			<input>${project.build.directory}/media-linux/updates.xml</input>
		</inputs>
		<output>${project.build.directory}/media/updates.xml</output>
	</configuration>
</plugin>
```
