Helper class to simplify starting an embedded Tomcat in a
development environment for a Maven webapplication project.

Add this dependency to your project.

```
		<dependency>
			<groupId>ch.rasc</groupId>
			<artifactId>embeddedtc</artifactId>
			<version>1.5</version>
			<scope>provided</scope>
		</dependency>
```
		
and start the embedded Tomcat with ch.rasc.embeddedtc.EmbeddedTomcat.	
This starts a Tomcat on port 8080 with a context path "" and a context directory
that points to current_dir + /src/main/webapp

If you need more control create a class in your project (e.g. StartTomcat),
create an instance of EmbeddedTomcat, call the configure methods and start it
with startAndWait()

```
public class StartTomcat {
  public static void main(String[] args) throwsException {
    EmbeddedTomcat etc = new EmbeddedTomcat(9999);
    etc.setContextDirectory("/home/mywebapp");
    etc.setTempDirectoryName("tmp");
    etc.addContextEnvironmentString("key", "value");
    etc.startAndWait();
  }
}
```

or with method chaining

```
public class StartTomcat {
  public static void main(String[] args) throwsException {
    EmbeddedTomcat.create()
		          .setPort(9999)
		          .setContextDirectory("/home/mywebapp")
		          .setTempDirectoryName("tmp")
		          .addContextEnvironmentString("key", "value")
		          .startAndWait();
	}
}
```

It's also possible to configure the embedded Tomcat with a context xml file. This examples starts a Tomcat on port 8080 with a 
web application deployed to the ROOT context ("") and the context directory points to ./src/main/webapp

```
public class StartTomcat {
  public static void main(String[] args) throwsException {
    EmbeddedTomcat.create()
		          .setContextFile("./src/main/config/tomcat.xml")
		          .startAndWait();
	}
}
```


## CHANGELOG

### 1.5     February 19, 2013
  * Updated to Tomcat 7.0.37
  
### 1.4     January 31, 2013
  * Updated to Tomcat 7.0.35
  * Added addContextInitializationParameter method. Adds context parameters that are accessible within the application with 
    [ServletContext#getInitParameter](http://docs.oracle.com/javaee/6/api/javax/servlet/ServletContext.html#getInitParameter\(java.lang.String\))
  * Deprectated addContextEnvironmentAndResourceFromFile method and replaced it with setContextFile. This library no longer reads this file it
    passes the location of the file to Tomcat and he will then read the file during startup. As a result every configuration that is allowed 
    in the context xml file is now supported.
  * Shutdown Tomcat if an error occurs during startup

### 1.3     November 22, 2012 
  * Use "" for the root context. / is wrong.
  * Updated to Tomcat 7.0.33

### 1.2     October 17, 2012
  * Updated to Tomcat 7.0.32

### 1.1     September 8, 2012
  * Updated to Tomcat 7.0.30
  * Added useNio() method. Instructs the embedded Tomcat to use the Non Blocking Connector
    (org.apache.coyote.http11.Http11NioProtocol) instead of the Blocking Connector 
    (org.apache.coyote.http11.Http11Protocol)
  * Added enableNaming() method. Enables JNDI naming.
  * Removed resource jar handling methods. This works now out of the box with Tomcat 7.0.29 
    and later
  * Added code that reads the catalina.properties at startup. 
  * Added method skipJarsDefaultJarScanner(String skipJars). These jars are not scanned at 
    startup for web fragments, tlds, etc. This decreases startup time significantly. By 
    default catalina.properties already contains a list of jars to skip. The parameter of 
    this method is added to the end of the list from catalina.properties. 
  * Added skipJarsContextConfig(String skipJars) and skipJarsTldConfig(String skipJars) 
    methods to support Tomcat 7.0.30 feature of scan specific lists. 

    skipJarsContextConfig(String skipJars)
    Additional JARs (over and above the default JARs set with skipJarsDefaultJarScanner) 
    to skip when scanning for Servlet 3.0 pluggability features. These features include web
    fragments, annotations, SCIs and classes that match @HandlesTypes. 
    
    skipJarsTldConfig(String skipJars)
    Additional JARs (over and above the default JARs set with skipJarsDefaultJarScanner) 
    to skip when scanning for TLDs.

### 1.0     July 11, 2012
  * Moved to a new package ch.ralscha -> ch.rasc
  * Updated to Tomcat 7.0.29

### 0.0.9   June 21, 2012
  * Updated to Tomcat 7.0.28

### 0.0.8   May 21, 2012
  * Updated ecj to 3.7.2
  * Fix bug in installSlf4jBridge. install() does not have a parameter

### 0.0.7   April 17, 2012
  * Removed addInitializer(..) method.  
    Directory WEB-INF/classes is now mapped to ./target/classes and therefore all the
    Servlet 3.0 Annotations and ServletContainerInitializer are now working out of the box.

### 0.0.6   March 9, 2012
  * Updated to Tomcat 7.0.27
  
### 0.0.5   March 9, 2012
  * Bugfix: Make class ContextConfig public. Digester cannot access class if it has package visibility
  * All config methods now return the EmbeddedTomcat instance to enable method chaining 
  * Changed parameters to final

### 0.0.4   March 2, 2012
  * Add a new method addContextEnvironmentAndResourceFromFile(File f)
    Reads ContextEnvironment and Contextresource from a file and adds it to the embedded Tomcat
  * Changed the way resource jars are loaded. In older version the ZipFile was closed after starting Tomcat
    and was no longer able to read content from the jar file.   
  * Moved default temporary directory to the projects target directory
  * TomcatTest: Now deletes temp directory before starting Tomcat
    
### 0.0.3   February 26, 2012
  * The start() method no longer blocks after starting Tomcat
  * Added a new method startAndWait() that blocks the program after starting Tomcat  
  * Added setSilent(boolean) method. If set to true it disables output messages.
  * Added new constructor EmbeddedTomcat(String contextPath). 
    Creates Tomcat with specified context path and port 8080  
    
### 0.0.2   February 25, 2012                 
  * Convert exceptions to RuntimeException
  * Starts by default a shutdown listener on the shutdown port (port + 1000)
  * Before starting a new Tomcat it tries to stop a previous one 
    by sending a command to the shutdown port.

### 0.0.1   February 24, 2012 
  * Initial version