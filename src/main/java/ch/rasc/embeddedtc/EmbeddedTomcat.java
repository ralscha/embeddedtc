/**
 * Copyright 2012-2017 Ralph Schaer <ralphschaer@gmail.com>
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
package ch.rasc.embeddedtc;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Server;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.JasperListener;
import org.apache.catalina.core.JreMemoryLeakPreventionListener;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.ThreadLocalLeakPreventionListener;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.mbeans.GlobalResourcesLifecycleListener;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.CatalinaProperties;
import org.apache.catalina.startup.Tomcat;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Helper class to simplify setting up a Embedded Tomcat in a IDE and with a Maven web
 * project.
 *
 * @author Ralph Schaer
 */
public class EmbeddedTomcat {

	private static final Log log = LogFactory.getLog(EmbeddedTomcat.class);

	private String contextPath;

	private Integer httpPort;

	private Integer shutdownPort;

	private int secondsToWaitBeforePortBecomesAvailable;

	public int maxPostSize = 2097152;

	private int httpsPort;

	private String keyStoreFile;

	private String keyAlias;

	private String keyStorePass;

	private String sslProtocol;

	private String tempDirectory;

	private String contextDirectory;

	private String skipJarsDefaultJarScanner;

	private String skipJarsContextConfig;

	private String skipJarsTldConfig;

	private boolean privileged;

	private boolean silent;

	private boolean addDefaultListeners = false;

	private boolean useNio = false;

	private int compressionMinSize = -1;

	private String compressableMimeType;

	private boolean enableNaming = false;

	private final List<ContextEnvironment> contextEnvironments;

	private final List<ContextResource> contextResources;

	private final List<ApplicationParameter> contextInitializationParameters;

	private URL contextFileURL;

	private Tomcat tomcat;

	/**
	 * Starts a embedded Tomcat on port 8080 with context path "" and context directory
	 * current directory + /src/main/webapp
	 *
	 * @param args program arguments
	 */
	public static void main(String[] args) {
		new EmbeddedTomcat().startAndWait();
	}

	/**
	 * Convenient method to create a embedded Tomcat that listens on port 8080 and with a
	 * context path of ""
	 *
	 * @return EmbeddedTomcat the embedded tomcat
	 */
	public static EmbeddedTomcat create() {
		return new EmbeddedTomcat();
	}

	/**
	 * Creates an embedded Tomcat with context path "" and port 8080. Context directory
	 * points to current directory + /src/main/webapp Change context directory with the
	 * method <code>setContextDirectory(String)</code>
	 *
	 * @see EmbeddedTomcat#setContextDirectory(String)
	 */
	public EmbeddedTomcat() {
		this("", 8080);
	}

	/**
	 * Creates an embedded Tomcat with context path "" and specified port. Context
	 * directory points to current directory + /src/main/webapp Change context directory
	 * with the method <code>setContextDirectory(String)</code>
	 *
	 * @param port ip port the server is listening
	 *
	 * @see #setContextDirectory(String)
	 */
	public EmbeddedTomcat(int port) {
		this("", port);
	}

	/**
	 * Creates an embedded Tomcat with the specified context path and port 8080 Context
	 * directory points to current directory + /src/main/webapp Change context directory
	 * with the method <code>setContextDirectory(String)</code>
	 *
	 * @param contextPath The context path has to start with /
	 *
	 * @see #setContextDirectory(String)
	 */
	public EmbeddedTomcat(String contextPath) {
		this(contextPath, 8080);
	}

	/**
	 * Creates an embedded Tomcat with specified context path and specified port. Context
	 * directory points to current directory + /src/main/webapp Change context directory
	 * with the method <code>setContextDirectory(String)</code>
	 *
	 * @param contextPath has to start with /
	 * @param httpPort ip port the server is listening for http requests. Shutdown port is
	 * set to port + 1000
	 *
	 * @see EmbeddedTomcat#setContextDirectory(String)
	 */
	public EmbeddedTomcat(String contextPath, int httpPort) {
		this(contextPath, httpPort, 0);
	}

	/**
	 * Creates an embedded Tomcat with specified context path and specified ports. Context
	 * directory points to current directory + /src/main/webapp Change context directory
	 * with the method <code>setContextDirectory(String)</code>
	 *
	 * @param contextPath has to start with /
	 * @param httpPort ip port the server is listening for http requests. Shutdown port is
	 * set to port + 1000
	 * @param httpsPort ip port the server is listening for https requests.
	 *
	 * @see EmbeddedTomcat#setContextDirectory(String)
	 */
	public EmbeddedTomcat(String contextPath, int httpPort, int httpsPort) {
		this.tomcat = null;

		setContextPath(contextPath);
		setHttpPort(httpPort);
		setShutdownPort(httpPort + 1000);
		setHttpsPort(httpsPort);
		setSecondsToWaitBeforePortBecomesAvailable(10);
		setPrivileged(false);
		setSilent(false);
		setContextDirectory(null);

		this.tempDirectory = null;
		this.contextEnvironments = new ArrayList<ContextEnvironment>();
		this.contextResources = new ArrayList<ContextResource>();
		this.contextInitializationParameters = new ArrayList<ApplicationParameter>();
	}

	/**
	 * Sets the port the server is listening for http requests
	 *
	 * @param port The new port
	 * @return The embedded Tomcat
	 * @deprecated Use {@link #setHttpPort(int)} instead
	 */
	@Deprecated
	public EmbeddedTomcat setPort(int port) {
		return setHttpPort(port);
	}

	/**
	 * Sets the port the server is listening for http requests
	 *
	 * @param httpPort The new port
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setHttpPort(int httpPort) {
		this.httpPort = httpPort;
		return this;
	}

	/**
	 * The maximum size in bytes of the POST which will be handled by the container FORM
	 * URL parameter parsing. The limit can be disabled by setting this attribute to a
	 * value less than or equal to 0. If not specified, this attribute is set to 2097152
	 * (2 megabytes).
	 *
	 * @param maxPostSize maximum size in bytes for POST requests
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setMaxPostSize(int maxPostSize) {
		this.maxPostSize = maxPostSize;
		return this;
	}

	/**
	 * Sets the port the server is listening for https requests
	 *
	 * @param httpsPort The new port
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setHttpsPort(int httpsPort) {
		this.httpsPort = httpsPort;
		return this;
	}

	/**
	 * The pathname of the keystore file where you have stored the server certificate to
	 * be loaded. By default, the pathname is the file ".keystore" in the operating system
	 * home directory of the user that is running Tomcat.
	 *
	 * @param keyStoreFile The keystore pathname
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setKeyStoreFile(String keyStoreFile) {
		this.keyStoreFile = keyStoreFile;
		return this;
	}

	/**
	 * The alias used to for the server certificate in the keystore. If not specified the
	 * first key read in the keystore will be used.
	 *
	 * @param keyAlias The key alias
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
		return this;
	}

	/**
	 * The password used to access the server certificate from the specified keystore
	 * file. The default value is "changeit".
	 *
	 * @param keyStorePass The keystore password
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setKeyStorePass(String keyStorePass) {
		this.keyStorePass = keyStorePass;
		return this;
	}

	/**
	 * Sets the contextPath for the webapplication
	 *
	 * @param contextPath The new contextPath. Has to start with / or is the empty ""
	 * string
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setContextPath(String contextPath) {

		if (contextPath == null
				|| !contextPath.equals("") && !contextPath.startsWith("/")) {
			throw new IllegalArgumentException(
					"contextPath must be the empty string \"\" or a path starting with /");
		}

		this.contextPath = contextPath;
		return this;
	}

	/**
	 * Sets the context directory. Normally this point to the directory that hosts the
	 * WEB-INF directory. Default value is: current directory + /src/main/webapp This is
	 * the normal location of the webapplication directory in a Maven web app project.
	 *
	 * @param contextDirectory Path name to the directory that contains the web
	 * application.
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setContextDirectory(String contextDirectory) {
		this.contextDirectory = contextDirectory;
		return this;
	}

	/**
	 * List of JAR files that should not be scanned using the JarScanner functionality.
	 * This is typically used to scan JARs for configuration information. JARs that do not
	 * contain such information may be excluded from the scan to speed up the scanning
	 * process. JARs on this list are excluded from all scans.
	 * <p>
	 * Scan specific lists (to exclude JARs from individual scans) see
	 * {@link #skipJarsContextConfig(String)} and {@link #skipJarsTldConfig(String)}.
	 * <p>
	 * The list must be a comma separated list of JAR file names. Example:
	 * spring*.jar,cglib*.jar.
	 * <p>
	 * This list is appended to the default list. The default list is located in the file
	 * CATALINA_HOME\conf\catalina.properties under the key
	 * tomcat.util.scan.DefaultJarScanner.jarsToSkip
	 *
	 * @param skipJars list of jars, comma separated
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat skipJarsDefaultJarScanner(String skipJars) {
		this.skipJarsDefaultJarScanner = skipJars;
		return this;
	}

	/**
	 * Additional JARs (over and above the default JARs set with
	 * {@link #skipJarsDefaultJarScanner(String)}) to skip when scanning for Servlet 3.0
	 * pluggability features. These features include web fragments, annotations, SCIs and
	 * classes that match @HandlesTypes. The list must be a comma separated list of JAR
	 * file names.
	 *
	 * @param skipJars list of jars, comma separated
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat skipJarsContextConfig(String skipJars) {
		this.skipJarsContextConfig = skipJars;
		return this;
	}

	/**
	 * Additional JARs (over and above the default JARs set with
	 * {@link #skipJarsDefaultJarScanner(String)}) to skip when scanning for TLDs. The
	 * list must be a comma separated list of JAR file names.
	 *
	 * @param skipJars list of jars, comma separated
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat skipJarsTldConfig(String skipJars) {
		this.skipJarsTldConfig = skipJars;
		return this;
	}

	/**
	 * Sets the location of the temporary directory. Tomcat needs this for storing
	 * temporary files like compiled jsp files. Default value is
	 * <p>
	 * <code>
	 * target/tomcat. + port
	 * </code>
	 *
	 * @param tempDirectory File object that represents the location of the temp directory
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setTempDirectory(File tempDirectory) {
		this.tempDirectory = tempDirectory.getAbsolutePath();
		return this;
	}

	/**
	 * Sets the temporary directory to a directory beneath the target directory <br>
	 * The directory does not have to exists, Tomcat will create it automatically if
	 * necessary.
	 *
	 * @param name directory name
	 * @return The embedded Tomcat
	 *
	 * @see EmbeddedTomcat#setTempDirectory(File)
	 */
	public EmbeddedTomcat setTempDirectoryName(String name) {
		this.tempDirectory = new File(".", "target/" + name).getAbsolutePath();
		return this;
	}

	/**
	 * The EmbeddedTomcat listens per default for shutdown commands on port 8005 with the
	 * shutdown command <code>SHUTDOWN</code>. Calling this method disables adding the
	 * shutdown hook.
	 *
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat dontAddShutdownHook() {
		this.shutdownPort = null;
		return this;
	}

	/**
	 * Before starting the embedded Tomcat the programm tries to stop a previous process
	 * by sendig the shutdown command to the shutdown port. It then waits for the port to
	 * become available. It checks this every second for the specified number of seconds
	 *
	 * @param seconds number of seconds
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setSecondsToWaitBeforePortBecomesAvailable(int seconds) {
		this.secondsToWaitBeforePortBecomesAvailable = seconds;
		return this;
	}

	/**
	 * Specifies the port the server is listen for the shutdown command. Default is port
	 * 8005
	 *
	 * @param shutdownPort the shutdown port
	 * @return The embedded Tomcat *
	 *
	 * @see EmbeddedTomcat#dontAddShutdownHook()
	 */
	public EmbeddedTomcat setShutdownPort(int shutdownPort) {
		this.shutdownPort = shutdownPort;
		return this;
	}

	/**
	 * Set the privileged flag for this web application.
	 *
	 * @param privileged The new privileged flag
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setPrivileged(boolean privileged) {
		this.privileged = privileged;
		return this;
	}

	/**
	 * Instructs the embedded tomcat to use the Non Blocking Connector
	 * (org.apache.coyote.http11.Http11NioProtocol) instead of the Blocking Connector
	 * (org.apache.coyote.http11.Http11Protocol)
	 *
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat useNio() {
		this.useNio = true;
		return this;
	}

	@SuppressWarnings("hiding")
	public EmbeddedTomcat enableCompression(int compressionMinSize,
			String compressableMimeType) {
		this.compressionMinSize = compressionMinSize;
		this.compressableMimeType = compressableMimeType;
		return this;
	}

	/**
	 * Enables JNDI naming which is disabled by default.
	 *
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat enableNaming() {
		this.enableNaming = true;
		return this;
	}

	/**
	 * Installs the default listeners AprLifecycleListener, JasperListener,
	 * JreMemoryLeakPreventionListener, GlobalResourcesLifecycleListener and
	 * ThreadLocalLeakPreventionListener during startup.
	 *
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat addDefaultListeners() {
		this.addDefaultListeners = true;
		return this;
	}

	/**
	 * Set the silent flag of Tomcat. If true Tomcat no longer prints any log messages
	 *
	 * @param silent The new silent flag
	 * @return The embedded Tomcat
	 */
	public EmbeddedTomcat setSilent(boolean silent) {
		this.silent = silent;
		return this;
	}

	/**
	 * Adds a {@link ContextEnvironment} object to embedded Tomcat.
	 * <p>
	 * Example:<br>
	 * Tomcat context xml file
	 *
	 * <pre>
	 *    &lt;Environment name="aparam"
	 *                 value="test"
	 *                 type="java.lang.String"
	 *                 override="false"/&gt;
	 * </pre>
	 *
	 * A programmatic way to add this environment to the embedded Tomcat is by calling
	 * this method
	 *
	 * <pre>
	 * ContextEnvironment env = new ContextEnvironment();
	 * env.setType(&quot;java.lang.String&quot;);
	 * env.setName(&quot;aparam&quot;);
	 * env.setValue(&quot;test&quot;);
	 * embeddedTomcat.addContextEnvironment(env);
	 * </pre>
	 *
	 *
	 * @param env context environment variable
	 * @return The embedded Tomcat
	 *
	 * @see ContextEnvironment
	 * @see NamingResources#addEnvironment(ContextEnvironment)
	 * @see EmbeddedTomcat#addContextEnvironment(String, String, String)
	 * @see EmbeddedTomcat#addContextEnvironmentString(String, String)
	 */
	public EmbeddedTomcat addContextEnvironment(ContextEnvironment env) {
		this.contextEnvironments.add(env);
		return this;
	}

	/**
	 * Adds a {@link ContextResource} object to the list of resources in the embedded
	 * Tomcat.
	 *
	 * <p>
	 * Example:<br>
	 * Tomcat context xml file
	 *
	 * <pre>
	 *  &lt;Resource name="jdbc/ds" auth="Container"
	 *     type="javax.sql.DataSource" username="sa" password=""
	 *     driverClassName="org.h2.Driver"
	 *     url="jdbc:h2:~/mydb"
	 *     maxActive="20" maxIdle="4" maxWait="10000"
	 *     defaultAutoCommit="false"/&gt;
	 * </pre>
	 *
	 * Programmatic way:
	 *
	 * <pre>
	 * ContextResource res = new ContextResource();
	 * res.setName(&quot;jdbc/ds&quot;);
	 * res.setType(&quot;javax.sql.DataSource&quot;);
	 * res.setAuth(&quot;Container&quot;);
	 * res.setProperty(&quot;username&quot;, &quot;sa&quot;);
	 * res.setProperty(&quot;password&quot;, &quot;&quot;);
	 * res.setProperty(&quot;driverClassName&quot;, &quot;org.h2.Driver&quot;);
	 * res.setProperty(&quot;url&quot;, &quot;jdbc:h2:&tilde;/mydb&quot;);
	 * res.setProperty(&quot;maxActive&quot;, &quot;20&quot;);
	 * res.setProperty(&quot;maxIdle&quot;, &quot;4&quot;);
	 * res.setProperty(&quot;maxWait&quot;, &quot;10000&quot;);
	 * res.setProperty(&quot;defaultAutoCommit&quot;, &quot;false&quot;);
	 *
	 * embeddedTomcat.addContextResource(res);
	 * </pre>
	 *
	 * @param res resource object
	 * @return The embedded Tomcat
	 *
	 * @see ContextResource
	 * @see NamingResources#addResource(ContextResource)
	 */
	public EmbeddedTomcat addContextResource(ContextResource res) {
		this.contextResources.add(res);
		return this;
	}

	public EmbeddedTomcat addContextInitializationParameter(String name, String value) {
		ApplicationParameter parameter = new ApplicationParameter();
		parameter.setName(name);
		parameter.setValue(value);
		this.contextInitializationParameters.add(parameter);
		return this;
	}

	/**
	 * Convenient method for adding a context environment to the embedded Tomcat. Creates
	 * a <code>ContextEnvironment</code> object and adds it to the list of the context
	 * environments.
	 *
	 * <p>
	 * Example:<br>
	 * Tomcat context xml file
	 *
	 * <pre>
	 *    &lt;Environment name="aparam"
	 *                 value="test"
	 *                 type="java.lang.String"
	 *                 override="false"/&gt;
	 * </pre>
	 *
	 * Programmatic way:
	 *
	 * <pre>
	 * embeddedTomcat.addContextEnvironment(&quot;aparam&quot;, &quot;test&quot;, &quot;java.lang.String&quot;);
	 * </pre>
	 *
	 * @param name name of the context environment
	 * @param value value of the context environment
	 * @param type type of the context environment
	 * @return The embedded Tomcat
	 *
	 * @see ContextEnvironment
	 * @see NamingResources#addEnvironment(ContextEnvironment)
	 * @see EmbeddedTomcat#addContextEnvironment(ContextEnvironment)
	 * @see EmbeddedTomcat#addContextEnvironmentString(String, String)
	 */
	public EmbeddedTomcat addContextEnvironment(String name, String value, String type) {
		final ContextEnvironment env = new ContextEnvironment();
		env.setType(type);
		env.setName(name);
		env.setValue(value);
		addContextEnvironment(env);
		return this;
	}

	/**
	 * Convenient method for adding a context environment with type java.lang.String to
	 * the embedded Tomcat.
	 *
	 * <pre>
	 * embeddedTomcat.addContextEnvironment(&quot;aparam&quot;, &quot;test&quot;);
	 * </pre>
	 *
	 * @param name name of the context environment
	 * @param value value of the context environment
	 * @return The embedded Tomcat
	 *
	 * @see ContextEnvironment
	 * @see NamingResources#addEnvironment(ContextEnvironment)
	 * @see EmbeddedTomcat#addContextEnvironment(ContextEnvironment)
	 * @see EmbeddedTomcat#addContextEnvironment(String, String, String)
	 */
	public EmbeddedTomcat addContextEnvironmentString(String name, String value) {
		addContextEnvironment(name, value, "java.lang.String");
		return this;
	}

	/**
	 * Read ContextEnvironment and ContextResource definition from a text file.
	 *
	 * @param contextFile Location of the context file
	 * @return The embedded Tomcat
	 * @deprecated use {@link #setContextFile(URL)}
	 */
	@Deprecated
	public EmbeddedTomcat addContextEnvironmentAndResourceFromFile(File contextFile) {
		try {
			return setContextFile(contextFile.toURI().toURL());
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets the location of the context file that configures this web application
	 *
	 * @param contextFileURL Location of the context file
	 * @return The embedded Tomcat instance
	 */
	public EmbeddedTomcat setContextFile(URL contextFileURL) {
		this.contextFileURL = contextFileURL;
		return this;
	}

	/**
	 * Sets the location of the context file that configures this web application
	 *
	 * @param contextFile Location of the context file
	 * @return The embedded Tomcat instance
	 */
	public EmbeddedTomcat setContextFile(String contextFile) {
		try {
			this.contextFileURL = new File(contextFile).toURI().toURL();
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	/**
	 * Read ContextEnvironment and ContextResource definition from a text file.
	 *
	 * @param contextFile Location to a context file
	 * @return The embedded Tomcat
	 * @deprecated use {@link #setContextFile(String)}
	 */
	@Deprecated
	public EmbeddedTomcat addContextEnvironmentAndResourceFromFile(String contextFile) {
		try {
			setContextFile(new File(contextFile).toURI().toURL());
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	/**
	 * Starts the embedded Tomcat and do not wait for incoming requests. Returns
	 * immediately if the configured port is in use.
	 *
	 * @see EmbeddedTomcat#startAndWait()
	 */
	public void start() {
		start(false);
	}

	/**
	 * Starts the embedded Tomcat and waits for incoming requests. Returns immediately if
	 * the configured port is in use.
	 *
	 * @see EmbeddedTomcat#start()
	 */
	public void startAndWait() {
		start(true);
	}

	private void start(boolean await) {

		// try to shutdown a previous Tomcat
		sendShutdownCommand();

		try {
			final ServerSocket srv = new ServerSocket(this.httpPort);
			srv.close();
		}
		catch (IOException e) {
			log.error("PORT " + this.httpPort + " ALREADY IN USE");
			return;
		}

		// Read a dummy value. This triggers loading of the catalina.properties
		// file
		CatalinaProperties.getProperty("dummy");

		appendSkipJars("tomcat.util.scan.DefaultJarScanner.jarsToSkip",
				this.skipJarsDefaultJarScanner);
		appendSkipJars("org.apache.catalina.startup.ContextConfig.jarsToSkip",
				this.skipJarsContextConfig);
		appendSkipJars("org.apache.catalina.startup.TldConfig.jarsToSkip",
				this.skipJarsTldConfig);

		this.tomcat = new Tomcat();

		if (this.tempDirectory == null) {
			this.tempDirectory = new File(".", "/target/tomcat." + this.httpPort)
					.getAbsolutePath();
		}

		this.tomcat.setBaseDir(this.tempDirectory);

		if (this.silent) {
			this.tomcat.setSilent(true);
		}

		if (this.addDefaultListeners) {
			this.tomcat.getServer().addLifecycleListener(new AprLifecycleListener());
		}

		if (this.useNio) {
			Connector connector = new Connector(
					"org.apache.coyote.http11.Http11NioProtocol");
			connector.setPort(this.httpPort);
			connector.setMaxPostSize(this.maxPostSize);
			connector.setURIEncoding("UTF-8");
			this.tomcat.setConnector(connector);
			this.tomcat.getService().addConnector(connector);
		}
		else {
			this.tomcat.setPort(this.httpPort);
			this.tomcat.getConnector().setURIEncoding("UTF-8");
			this.tomcat.getConnector().setMaxPostSize(this.maxPostSize);
		}

		if (this.compressionMinSize >= 0) {
			this.tomcat.getConnector().setProperty("compression",
					String.valueOf(this.compressionMinSize));
			this.tomcat.getConnector().setProperty("compressableMimeType",
					this.compressableMimeType);
		}

		if (this.httpsPort != 0) {
			final Connector httpsConnector;
			if (this.useNio) {
				httpsConnector = new Connector(
						"org.apache.coyote.http11.Http11NioProtocol");
			}
			else {
				httpsConnector = new Connector("HTTP/1.1");
			}
			httpsConnector.setSecure(true);
			httpsConnector.setPort(this.httpsPort);
			httpsConnector.setMaxPostSize(this.maxPostSize);
			httpsConnector.setScheme("https");
			httpsConnector.setURIEncoding("UTF-8");

			httpsConnector.setProperty("SSLEnabled", "true");
			httpsConnector.setProperty("keyAlias", this.keyAlias);
			httpsConnector.setProperty("keystoreFile", this.keyStoreFile);
			httpsConnector.setProperty("keystorePass", this.keyStorePass);
			httpsConnector.setProperty("sslProtocol", this.sslProtocol);

			if (this.compressionMinSize >= 0) {
				httpsConnector.setProperty("compression",
						String.valueOf(this.compressionMinSize));
				httpsConnector.setProperty("compressableMimeType",
						this.compressableMimeType);
			}

			this.tomcat.getEngine().setDefaultHost("localhost");
			this.tomcat.getService().addConnector(httpsConnector);
		}

		if (this.shutdownPort != null) {
			this.tomcat.getServer().setPort(this.shutdownPort);
		}

		String contextDir = this.contextDirectory;
		if (contextDir == null) {
			contextDir = new File(".").getAbsolutePath() + "/src/main/webapp";
		}

		final Context ctx;
		try {

			if (!this.contextPath.equals("")) {
				File rootCtxDir = new File("./target/tcroot");
				if (!rootCtxDir.exists()) {
					rootCtxDir.mkdirs();
				}
				Context rootCtx = this.tomcat.addWebapp("", rootCtxDir.getAbsolutePath());
				rootCtx.setPrivileged(true);
				Tomcat.addServlet(rootCtx, "listContexts",
						new ListContextsServlet(rootCtx)).addMapping("/");
			}

			ctx = this.tomcat.addWebapp(this.contextPath, contextDir);
			ctx.setResources(new TargetClassesContext());
		}
		catch (ServletException e) {
			throw new RuntimeException(e);
		}

		if (this.privileged) {
			ctx.setPrivileged(true);
		}

		if (this.enableNaming || !this.contextEnvironments.isEmpty()
				|| !this.contextResources.isEmpty() || this.contextFileURL != null) {
			this.tomcat.enableNaming();

			if (this.addDefaultListeners) {
				this.tomcat.getServer()
						.addLifecycleListener(new GlobalResourcesLifecycleListener());
			}
		}

		if (this.addDefaultListeners) {
			Server server = this.tomcat.getServer();
			server.addLifecycleListener(new JasperListener());
			server.addLifecycleListener(new JreMemoryLeakPreventionListener());
			server.addLifecycleListener(new ThreadLocalLeakPreventionListener());
		}

		for (ContextEnvironment env : this.contextEnvironments) {
			ctx.getNamingResources().addEnvironment(env);
		}

		for (ContextResource res : this.contextResources) {
			ctx.getNamingResources().addResource(res);
		}

		for (ApplicationParameter param : this.contextInitializationParameters) {
			ctx.addApplicationParameter(param);
		}

		if (this.contextFileURL != null) {
			ctx.setConfigFile(this.contextFileURL);
		}

		// Shutdown tomcat if a failure occurs during startup
		ctx.addLifecycleListener(new LifecycleListener() {
			@Override
			public void lifecycleEvent(LifecycleEvent event) {
				if (event.getLifecycle().getState() == LifecycleState.FAILED) {
					((StandardServer) EmbeddedTomcat.this.tomcat.getServer()).stopAwait();

				}
			}
		});

		try {
			this.tomcat.start();
		}
		catch (LifecycleException e) {
			throw new RuntimeException(e);
		}

		((StandardManager) ctx.getManager()).setPathname(null);

		installSlf4jBridge();

		if (await) {
			this.tomcat.getServer().await();
			stop();
		}

	}

	/**
	 * Stops the embedded tomcat. Does nothing if it's not started
	 */
	public void stop() {
		if (this.tomcat != null) {
			try {
				this.tomcat.stop();
			}
			catch (LifecycleException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static void appendSkipJars(String systemPropertyKey, String skipJars) {
		if (skipJars != null && !skipJars.trim().isEmpty()) {
			String oldValue = System.getProperty(systemPropertyKey);
			String newValue;
			if (oldValue != null && !oldValue.trim().isEmpty()) {
				newValue = oldValue + "," + skipJars;
			}
			else {
				newValue = skipJars;
			}
			System.setProperty(systemPropertyKey, newValue);
		}
	}

	private void sendShutdownCommand() {
		if (this.shutdownPort != null) {
			try {
				final Socket socket = new Socket("localhost", this.shutdownPort);
				final OutputStream stream = socket.getOutputStream();

				for (int i = 0; i < "SHUTDOWN".length(); i++) {
					stream.write("SHUTDOWN".charAt(i));
				}

				stream.flush();
				stream.close();
				socket.close();
			}
			catch (UnknownHostException e) {
				if (!this.silent) {
					log.debug(e);
				}
				return;
			}
			catch (IOException e) {
				if (!this.silent) {
					log.debug(e);
				}
				return;
			}

			// try to wait the specified amount of seconds until port becomes
			// available
			int count = 0;
			while (count < this.secondsToWaitBeforePortBecomesAvailable * 2) {
				try {
					final ServerSocket srv = new ServerSocket(this.httpPort);
					srv.close();
					return;
				}
				catch (IOException e) {
					count++;
				}
				try {
					TimeUnit.MILLISECONDS.sleep(500);
				}
				catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	private static void installSlf4jBridge() {
		try {
			// Check if slf4j bridge is available
			final Class<?> clazz = Class.forName("org.slf4j.bridge.SLF4JBridgeHandler");

			// Remove all JUL handlers
			java.util.logging.LogManager.getLogManager().reset();

			// Install slf4j bridge handler
			final Method method = clazz.getMethod("install", new Class<?>[0]);
			method.invoke(null);
		}
		catch (ClassNotFoundException e) {
			// do nothing
		}
		catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		catch (SecurityException e) {
			throw new RuntimeException(e);
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

}
