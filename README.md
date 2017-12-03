[![Build Status](https://api.travis-ci.org/ralscha/embeddedtc.png)](https://travis-ci.org/ralscha/embeddedtc)

Helper class to simplify starting an embedded Tomcat  for a Maven web application project in a integrated development environment (e.g. Eclipse) .

Add this dependency to your project.

```
		<dependency>
			<groupId>ch.rasc</groupId>
			<artifactId>embeddedtc</artifactId>
			<version>1.29</version>
			<!-- Tomcat8: <version>2.16</version> -->
			<scope>provided</scope>
		</dependency>
```
		
and start the embedded Tomcat with the main class ch.rasc.embeddedtc.EmbeddedTomcat.	
This starts a Tomcat on port 8080 with a context path "" and a context directory
that points to current_dir + /src/main/webapp

If you need more control create a class in your project (e.g. StartTomcat),
create an instance of EmbeddedTomcat, call the configure methods and start the server
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

## CHANGELOG Tomcat 8

### 2.16     December 3, 2017
  * Tomcat 8.0.47

### 2.15     July 18, 2017
  * Tomcat 8.0.45

### 2.14     April 23, 2016
  * Tomcat 8.0.33

### 2.13     February 18, 2016
  * Tomcat 8.0.32

### 2.12     December 8, 2015
  * Tomcat 8.0.30

### 2.11     November 25, 2015
  * Tomcat 8.0.29

### 2.10     October 13, 2015
  * Tomcat 8.0.28

### 2.9     October 2, 2015
  * Tomcat 8.0.27

### 2.8     August 22, 2015
  * Tomcat 8.0.26

### 2.7     July 7, 2015
  * Tomcat 8.0.24

### 2.6     May 23, 2015
  * Tomcat 8.0.23

### 2.5     May 18, 2015
  * Tomcat 8.0.22

### 2.4     March 27, 2015
  * Tomcat 8.0.21

### 2.3     February 21, 2015
  * Tomcat 8.0.20

### 2.2     January 27, 2015
  * Tomcat 8.0.18

### 2.1     January 17, 2015
  * Tomcat 8.0.17  

### 2.0     January 15, 2015
  * Initial release with Tomcat 8.0.15  


## CHANGELOG Tomcat 7

### 1.29     December 3, 2017
  * Tomcat 7.0.82

### 1.28     July 18, 2017
  * Tomcat 7.0.79

### 1.27     October 23, 2016
  * Tomcat 7.0.72

### 1.26     April 23, 2016
  * Tomcat 7.0.69

### 1.25     February 18, 2016
  * Tomcat 7.0.68

### 1.24     October 21, 2015
  * Tomcat 7.0.67

### 1.23     October 21, 2015
  * Tomcat 7.0.65

### 1.22     September 2, 2015
  * Tomcat 7.0.64

### 1.21     July 8, 2015
  * Tomcat 7.0.63

### 1.20     May 18, 2015
  * Tomcat 7.0.62

### 1.19     April 14, 2015
  * Tomcat 7.0.61

### 1.18     February 9, 2015
  * Tomcat 7.0.59

### 1.17     January 15, 2015
  * Tomcat 7.0.57
  * Add support for TLS
  * Add config option for maxPostSize

### 1.16     October 8, 2014
  * Tomcat 7.0.56

### 1.15     August 5, 2014
  * Tomcat 7.0.55

### 1.14     May 28, 2014
  * Tomcat 7.0.54

### 1.13     April 2, 2014
  * Tomcat 7.0.53
  
### 1.12     February 20, 2014
  * Tomcat 7.0.52

### 1.11     January 13, 2014
  * Tomcat 7.0.50
  * Add a call to stop() when somebody sends a shutdown command. Previous versions did not stop the server.  

### 1.10     October 26, 2013
  * Tomcat 7.0.47
  * Fix some issues and javadocs concerning the shutdown port
  
### 1.9     July 7, 2013
  * Tomcat 7.0.42
  * Fixed bug in TargetClassesContext. Only handle /WEB-INF/classes special case and call superclass for every other name.
    Bug prevented [ServletContext.getResourcePaths](http://docs.oracle.com/javaee/7/api/javax/servlet/ServletContext.html#getResourcePaths(java.lang.String)) to work correctly. 

### 1.8     June 10, 2013
  * Tomcat 7.0.41
  * Automatically add a list context servlet when application is not running on root context (""). 
    This servlet sends a 404 and lists the configured context path when the user tries to connect to an unknown context. Similar to jetty

### 1.7     May 9, 2013
  * Tomcat 7.0.40

### 1.6     March 28, 2013
  * Tomcat 7.0.39

### 1.5     February 19, 2013
  * Tomcat 7.0.37
  
### 1.4     January 31, 2013
  * Tomcat 7.0.35
  * Added addContextInitializationParameter method. Adds context parameters that are accessible within the application with 
    [ServletContext#getInitParameter](http://docs.oracle.com/javaee/6/api/javax/servlet/ServletContext.html#getInitParameter\(java.lang.String\))
  * Deprectated addContextEnvironmentAndResourceFromFile method and replaced it with setContextFile. This library no longer reads this file it
    passes the location of the file to Tomcat and he will then read the file during startup. As a result every configuration that is allowed 
    in the context xml file is now supported.
  * Shutdown Tomcat if an error occurs during startup

### 1.3     November 22, 2012 
  * Use "" for the root context. / is wrong.
  * Tomcat 7.0.33

### 1.2     October 17, 2012
  * Tomcat 7.0.32

### 1.1     September 8, 2012
  * Tomcat 7.0.30
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
  * Tomcat 7.0.29

### 0.0.9   June 21, 2012
  * Tomcat 7.0.28

### 0.0.8   May 21, 2012
  * Upgraded ecj to 3.7.2
  * Fix bug in installSlf4jBridge. install() does not have a parameter

### 0.0.7   April 17, 2012
  * Removed addInitializer(..) method.  
    Directory WEB-INF/classes is now mapped to ./target/classes and therefore all the
    Servlet 3.0 Annotations and ServletContainerInitializer are now working out of the box.

### 0.0.6   March 9, 2012
  * Tomcat 7.0.27
  
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