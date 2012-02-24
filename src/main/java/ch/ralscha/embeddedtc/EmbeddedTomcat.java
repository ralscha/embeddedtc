/**
 * Copyright 2012-2012 Ralph Schaer <ralphschaer@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ralscha.embeddedtc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Helper class to simplify setting up a Embedded Tomcat 
 * in a IDE and with a Maven web project.  
 * 
 * @author Ralph Schaer
 */
public class EmbeddedTomcat {

	private String contextPath;
	private int port;
	private String tempDirectory;
	private String contextDirectory;
	private boolean removeDefaultServlet;
	private List<Artifact> resourceArtifacts;
	private Map<Class<? extends ServletContainerInitializer>, Set<Class<?>>> initializers;
	private List<ContextEnvironment> contextEnvironments;
	private List<ContextResource> contextResources;
	private File pomFile;
	private File m2Directory;

	private Tomcat tomcat;

	/**
	 * Starts a embedded Tomcat on port 8080 with context path "/"
	 * and context directory current directory + /src/main/webapp
	 * 
	 * @param args
	 * @throws IllegalAccessException
	 * @throws LifecycleException
	 * @throws ServletException
	 * @throws InstantiationException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IllegalAccessException, LifecycleException, ServletException,
			InstantiationException, ParserConfigurationException, SAXException, IOException {
		new EmbeddedTomcat().start();
	}

	/**
	 * Creates an embedded Tomcat with context path "/" and port 8080. 
	 * Context directory points to current directory + /src/main/webapp
	 * Change context directory with the method <code>setContextDirectory(String)</code>
	 *  
	 * @see 	EmbeddedTomcat#setContextDirectory(String)
	 */
	public EmbeddedTomcat() {
		this("/", 8080);
	}

	/**
	 * Creates an embedded Tomcat with context path "/" and specified port. 
	 * Context directory points to current directory + /src/main/webapp
	 * Change context directory with the method <code>setContextDirectory(String)</code>
	 *  
	 * @see 	#setContextDirectory(String)
	 */
	public EmbeddedTomcat(int port) {
		this("/", port);
	}

	/**
	 * Creates an embedded Tomcat with specified context path and specified port. 
	 * Context directory points to current directory + /src/main/webapp
	 * Change context directory with the method <code>setContextDirectory(String)</code>
	 *  
	 * @see 	EmbeddedTomcat#setContextDirectory(String)
	 */
	public EmbeddedTomcat(String contextPath, int port) {
		if (contextPath != null && !contextPath.startsWith("/")) {
			throw new IllegalArgumentException("contextPath does not start with /");
		}

		this.pomFile = null;
		this.m2Directory = null;
		this.contextEnvironments = new ArrayList<ContextEnvironment>();
		this.contextResources = new ArrayList<ContextResource>();
		this.initializers = new HashMap<Class<? extends ServletContainerInitializer>, Set<Class<?>>>();
		this.resourceArtifacts = new ArrayList<Artifact>();
		this.contextDirectory = null;
		this.tempDirectory = null;
		this.removeDefaultServlet = false;
		this.port = port;
		this.tomcat = null;

		if (contextPath != null) {
			this.contextPath = contextPath;
		} else {
			this.contextPath = "/";
		}

	}

	/**
	 * Sets the context directory. Normally this point to the directory
	 * that hosts the WEB-INF directory. Default value is: current directory + /src/main/webapp
	 * This is the normal location of the webapplication directory 
	 * in a Maven web app project.
	 * 
	 * @param contextDirectory Path name to the directory that contains the 
	 * 						   web application. 
	 */
	public void setContextDirectory(String contextDirectory) {
		this.contextDirectory = contextDirectory;
	}

	/**
	 * Sets the location of the temporary directory. Tomcat needs this
	 * for storing temporary files like compiled jsp files.
	 * Default value is for a context path "/"
	 * <p>
	 * <code>
	 * System.getProperty("java.io.tmpdir") + "/_" + port 
	 * </code>
	 * <p>
	 * and with every other context path 
	 * <p>
	 * <code>
	 * System.getProperty("java.io.tmpdir") + contextPath + "_" + port 
	 * </code> 
	 * 
	 * @param tempDirectory File object that represents the location of the temp directory 
	 */
	public void setTempDirectory(File tempDirectory) {
		this.tempDirectory = tempDirectory.getAbsolutePath();
	}

	/**
	 * Sets the temporary directory to a directory beneath
	 * <code>System.getProperty("java.io.tmpdir")</code>
	 * <br>
	 * The directory does not have to exists, Tomcat will create it
	 * automatically if necessary.
	 * 
	 * @param name directory name
	 * 
	 * @see EmbeddedTomcat#setTempDirectory(File)
	 */
	public void setTempDirectoryName(String name) {
		this.tempDirectory = System.getProperty("java.io.tmpdir") + "/" + name;
	}

	/**
	 * Sets the location of the pom file. 
	 * <br>
	 * Default value is current directory + /pom.xml  
	 * 
	 * @param pomFile File object that points to the maven pom file (pom.xml)
	 */
	public void setPomFile(File pomFile) {
		this.pomFile = pomFile;
	}

	/**
	 * Sets the path to the Maven local repository. 
	 * Default value is  
	 * <code>
	 * System.getProperty("user.home") + /.m2/repository/
	 * </code>
	 * @param m2Directory File object that points to the Maven local repository directory
	 */
	public void setM2Directory(File m2Directory) {
		this.m2Directory = m2Directory;
	}

	public void removeDefaultServlet() {
		this.removeDefaultServlet = true;
	}

	/**
	 * Adds all the dependencies specified in the pom.xml (except scope provided)
	 * to the context as a resource jar. A resource jar contains
	 * static resources in the directory META-INF/resources
	 * < 
	 * @see Context#addResourceJarUrl(URL)
	 */
	public void addAllDependenciesAsResourceJar() {
		this.resourceArtifacts.add(new AllArtifact());
	}

	/**
	 * Adds the specified jar to the context as a resource jar.
	 * The helper class automatically locates the version of the
	 * specified artifact in the pom.xml and locates the jar file
	 * in the local maven repository.
	 * 
	 * @see Context#addResourceJarUrl(URL)
	 * @see EmbeddedTomcat#addAllDependenciesAsResourceJar()
	 */
	public void addDependencyAsResourceJar(String groupId, String artifact) {
		this.resourceArtifacts.add(new Artifact(groupId, artifact));
	}

	/**
	 * Adds a container initializer to the list of initializers.
	 * <p>
	 * For example in a spring project adding a SpringServlet Initializer
	 * <pre>
	 *  etc.addInitializer(SpringServletContainerInitializer.class, 
	 *          MyWebAppInitializer.class);
	 * </pre>
	 * If the initializer adds a servlet to the default root ("/")  
	 * call <code>removeDefaultServlet()</code>
	 * 
	 * 
	 * @param containerInitializer class that implements ServletContainerInitializer
	 * @param handlesClass any class that is handled by the initializer 
	 * 
	 * @see Context#addServletContainerInitializer(ServletContainerInitializer, Set)
	 * @see EmbeddedTomcat#removeDefaultServlet()
	 */
	public void addInitializer(Class<? extends ServletContainerInitializer> containerInitializer, Class<?> handlesClass) {
		Set<Class<?>> classes = initializers.get(containerInitializer);
		if (classes == null) {
			classes = new HashSet<Class<?>>();
			initializers.put(containerInitializer, classes);
		}
		classes.add(handlesClass);
	}

	/**
	 * Adds a {@link ContextEnvironment} object to embedded Tomcat.
	 * <p>
	 * Example:<br> 
	 * Tomcat context xml file
	 * <pre>
	 *    &lt;Environment name="aparam" 
	 *                 value="test" 
	 *                 type="java.lang.String" 
	 *                 override="false"/&gt;  
	 * </pre>
	 * A programmatic way to add this environment to the embedded Tomcat
	 * is by calling this method
	 * <pre>
	 *   ContextEnvironment env = new ContextEnvironment();
	 *   env.setType("java.lang.String");
	 *   env.setName("aparam");
	 *   env.setValue("test");
	 *   embeddedTomcat.addContextEnvironment(env);
	 * </pre>
	 * 
	 * 
	 * @param env context environment variable
	 * 
	 * @see ContextEnvironment
	 * @see NamingResources#addEnvironment(ContextEnvironment)
	 * @see EmbeddedTomcat#addContextEnvironment(String, String, String)
	 * @see EmbeddedTomcat#addContextEnvironmentString(String, String)
	 */
	public void addContextEnvironment(ContextEnvironment env) {
		contextEnvironments.add(env);
	}

	/**
	 * Adds a {@link ContextResource} object to the list of resources
	 * in the embedded Tomcat.
	 * 
	 * <p>
	 * Example:<br> 
	 * Tomcat context xml file
	 * <pre>
  	 *  &ltResource name="jdbc/ds" auth="Container"
     *     type="javax.sql.DataSource" username="sa" password=""
     *     driverClassName="org.h2.Driver"
     *     url="jdbc:h2:~/mydb"
     *     maxActive="20" maxIdle="4" maxWait="10000"
     *     defaultAutoCommit="false"/&gt;
	 * </pre>
	 * Programmatic way:
	 * <pre>
	 *   ContextResource res = new ContextResource();
	 *   res.setName("jdbc/ds");
	 *   res.setType("javax.sql.DataSource");
	 *   res.setAuth("Container");
	 *   res.setProperty("username", "sa");
	 *   res.setProperty("password", "");
	 *   res.setProperty("driverClassName", "org.h2.Driver");
	 *   res.setProperty("url", "jdbc:h2:~/mydb");
	 *   res.setProperty("maxActive", "20");
	 *   res.setProperty("maxIdle", "4");
	 *   res.setProperty("maxWait", "10000");
	 *   res.setProperty("defaultAutoCommit", "false");
	 *    
	 *   embeddedTomcat.addContextResource(res);
	 * </pre> 
	 * 
	 * @param res resource object
	 * 
	 * @see ContextResource
	 * @see NamingResources#addResource(ContextResource) 
	 */
	public void addContextResource(ContextResource res) {
		contextResources.add(res);
	}

	/**
	 * Convenient method for adding a context environment to the embedded Tomcat.
	 * Creates a <code>ContextEnvironment</code> object and adds it to the
	 * list of the context environments.
	 * 
	 * <p>
	 * Example:<br> 
	 * Tomcat context xml file
	 * <pre>
	 *    &lt;Environment name="aparam" 
	 *                 value="test" 
	 *                 type="java.lang.String" 
	 *                 override="false"/&gt;  
	 * </pre>
	 * Programmatic way:
	 * <pre>
	 *   embeddedTomcat.addContextEnvironment("aparam", "test", "java.lang.String");
	 * </pre>
	 * 
	 * @param name name of the context environment
	 * @param value value of the context environment
	 * @param type type of the context environment
	 * 
	 * @see ContextEnvironment
	 * @see NamingResources#addEnvironment(ContextEnvironment) 
	 * @see EmbeddedTomcat#addContextEnvironment(ContextEnvironment)
	 * @see EmbeddedTomcat#addContextEnvironmentString(String, String)
	 */
	public void addContextEnvironment(String name, String value, String type) {
		ContextEnvironment env = new ContextEnvironment();
		env.setType(type);
		env.setName(name);
		env.setValue(value);
		addContextEnvironment(env);
	}

	/**
	 * Convenient method for adding a context environment with 
	 * type java.lang.String to the embedded Tomcat.
	 * 
	 * <pre>
	 *   embeddedTomcat.addContextEnvironment("aparam", "test");
	 * </pre> 
	 * 
	 * @param name name of the context environment
	 * @param value value of the context environment
	 * 
	 * @see ContextEnvironment
	 * @see NamingResources#addEnvironment(ContextEnvironment) 
	 * @see EmbeddedTomcat#addContextEnvironment(ContextEnvironment)
	 * @see EmbeddedTomcat#addContextEnvironment(String, String, String)
	 */
	public void addContextEnvironmentString(String name, String value) {
		addContextEnvironment(name, value, "java.lang.String");
	}

	/**
	 * Starts the embedded Tomcat. 
	 * Returns immediately if the configured port is in use.
	 *  
	 * @throws LifecycleException
	 * @throws ServletException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public void start() throws LifecycleException, ServletException, InstantiationException, IllegalAccessException,
			ParserConfigurationException, SAXException, IOException {

		try {
			ServerSocket srv = new ServerSocket(port);
			srv.close();
		} catch (IOException e) {
			System.out.println("PORT " + port + " ALREADY IN USE");
			return;
		}

		installSlf4jBridge();

		tomcat = new Tomcat();
		tomcat.setPort(port);

		if (tempDirectory == null) {
			String baseDirName = contextPath;
			if ("/".equals(baseDirName)) {
				baseDirName = "_";
			} else {
				baseDirName = baseDirName.substring(1) + "_";
			}

			baseDirName += port;

			tempDirectory = System.getProperty("java.io.tmpdir") + "/" + baseDirName;
		}

		tomcat.setBaseDir(tempDirectory);
		tomcat.getConnector().setURIEncoding("UTF-8");

		String contextDir = contextDirectory;
		if (contextDir == null) {
			contextDir = new File(".").getAbsolutePath() + "/src/main/webapp";
		}

		final Context ctx = tomcat.addWebapp(contextPath, contextDir);

		if (!contextEnvironments.isEmpty() || !contextResources.isEmpty()) {
			tomcat.enableNaming();
		}

		for (ContextEnvironment env : contextEnvironments) {
			ctx.getNamingResources().addEnvironment(env);
		}

		for (ContextResource res : contextResources) {
			ctx.getNamingResources().addResource(res);
		}

		if (removeDefaultServlet) {
			ctx.addLifecycleListener(new LifecycleListener() {
				@Override
				public void lifecycleEvent(LifecycleEvent event) {
					if (Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
						ctx.removeServletMapping("/");
					}
				}
			});
		}

		if (!resourceArtifacts.isEmpty()) {
			List<URL> resourceUrls = findResourceUrls();
			for (URL url : resourceUrls) {
				ctx.addResourceJarUrl(url);
			}
		}

		if (!initializers.isEmpty()) {
			for (Map.Entry<Class<? extends ServletContainerInitializer>, Set<Class<?>>> entry : initializers.entrySet()) {
				ctx.addServletContainerInitializer(entry.getKey().newInstance(), entry.getValue());
			}

		}

		tomcat.start();
		((StandardManager) ctx.getManager()).setPathname("");

		tomcat.getServer().await();
	}

	/**
	 * Stops the embedded tomcat. Does nothing if it's not started
	 * 
	 * @throws LifecycleException
	 */
	public void stop() throws LifecycleException {
		if (tomcat != null) {
			tomcat.stop();
		}
	}

	private static void installSlf4jBridge() {
		try {
			// Check class is available
			final Class<?> clazz = Class.forName("org.slf4j.bridge.SLF4JBridgeHandler");

			// Remove all JUL handlers
			java.util.logging.LogManager.getLogManager().reset();

			// Install slf4j bridge handler
			final Method method = clazz.getMethod("install", (Class<?>) null);
			method.invoke(null);
		} catch (ClassNotFoundException e) {
			//do nothing
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	private List<URL> findResourceUrls() throws ParserConfigurationException, SAXException, IOException {

		File m2Dir = this.m2Directory;
		if (m2Dir == null) {
			File homeDir = new File(System.getProperty("user.home"));
			m2Dir = new File(homeDir, ".m2/repository/");
		}

		List<URL> jars = new ArrayList<URL>();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		File pom = this.pomFile;
		if (pom == null) {
			pom = new File("./pom.xml");
		}

		Document doc = db.parse(pom);

		Map<String, String> properties = new HashMap<String, String>();
		NodeList propertiesNodeList = doc.getElementsByTagName("properties");
		if (propertiesNodeList != null && propertiesNodeList.item(0) != null) {
			NodeList propertiesChildren = propertiesNodeList.item(0).getChildNodes();
			for (int i = 0; i < propertiesChildren.getLength(); i++) {
				Node node = propertiesChildren.item(i);
				if (node instanceof Element) {
					properties.put("${" + node.getNodeName() + "}", stripWhitespace(node.getTextContent()));
				}
			}
		}

		NodeList nodeList = doc.getElementsByTagName("dependency");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element node = (Element) nodeList.item(i);
			String groupId = node.getElementsByTagName("groupId").item(0).getTextContent();
			String artifact = node.getElementsByTagName("artifactId").item(0).getTextContent();
			String version = node.getElementsByTagName("version").item(0).getTextContent();

			groupId = stripWhitespace(groupId);
			artifact = stripWhitespace(artifact);
			version = stripWhitespace(version);

			groupId = resolveProperty(groupId, properties);
			artifact = resolveProperty(artifact, properties);
			version = resolveProperty(version, properties);

			if (isIncluded(groupId, artifact)) {

				String scope = null;
				NodeList scopeNode = node.getElementsByTagName("scope");
				if (scopeNode != null && scopeNode.item(0) != null) {
					scope = stripWhitespace(scopeNode.item(0).getTextContent());
				}

				if (scope == null || !scope.equals("provided")) {
					groupId = groupId.replace(".", "/");
					String artifactFileName = groupId + "/" + artifact + "/" + version + "/" + artifact + "-" + version
							+ ".jar";

					String pathName = new File(m2Dir, artifactFileName).getPath();
					jars.add(new URL("jar:file:/" + pathName + "!/"));
				}

			}
		}

		return jars;
	}

	private boolean isIncluded(String groupId, String artifactId) {

		for (Artifact artifact : resourceArtifacts) {
			if (artifact.is(groupId, artifactId)) {
				return true;
			}
		}
		return false;

	}

	private String stripWhitespace(String orig) {
		if (orig != null) {
			return orig.replace("\r", "").replace("\n", "").replace("\t", "").trim();
		}
		return orig;
	}

	private String resolveProperty(String orig, Map<String, String> properties) {
		String property = properties.get(orig);
		if (property != null) {
			return property;
		}
		return orig;
	}

}
