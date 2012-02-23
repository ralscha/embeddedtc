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
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class EmbeddedTomcat {

	private String contextPath;
	private int port;
	private String tempDir;
	private String contextDir;
	private boolean removeDefaultServlet;
	private List<Artifact> resourceArtifacts;
	private Map<Class<ServletContainerInitializer>, Set<Class<?>>> initializers;
	private List<ContextEnvironment> contextEnvironments;
	private List<ContextResource> contextResources;

	public EmbeddedTomcat() {
		this("/", 8080);
	}

	public EmbeddedTomcat(int port) {
		this("/", port);
	}

	public EmbeddedTomcat(String contextPath, int port) {
		if (contextPath != null && !contextPath.startsWith("/")) {
			throw new IllegalArgumentException("contextPath does not start with /");
		}
		this.contextEnvironments = new ArrayList<ContextEnvironment>();
		this.contextResources = new ArrayList<ContextResource>();
		this.initializers = new HashMap<Class<ServletContainerInitializer>, Set<Class<?>>>();
		this.resourceArtifacts = new ArrayList<Artifact>();
		this.contextDir = new File(".").getAbsolutePath() + "/src/main/webapp";
		this.tempDir = null;
		this.removeDefaultServlet = false;
		this.port = port;

		if (contextPath != null) {
			this.contextPath = contextPath;
		} else {
			this.contextPath = "/";
		}

		try {
			ServerSocket srv = new ServerSocket(port);
			srv.close();
		} catch (IOException e) {
			System.out.println("PORT " + port + " ALREADY IN USE");
		}
	}

	public void setContextDir(String contextDir) {
		this.contextDir = contextDir;
	}

	public void setTempDir(File baseDir) {
		this.tempDir = baseDir.getAbsolutePath();
	}

	public void setTempDirName(String name) {
		this.tempDir = System.getProperty("java.io.tmpdir") + "/" + name;
	}

	public void removeDefaultServlet() {
		removeDefaultServlet = true;
	}

	public void addAllDependenciesAsResourceJar() {
		resourceArtifacts.add(new AllArtifact());
	}

	public void addDependencyAsResourceJar(String groupId, String artifact) {
		resourceArtifacts.add(new Artifact(groupId, artifact));
	}

	public void addInitializer(Class<ServletContainerInitializer> containerInitializer, Class<?> handlesClass) {
		Set<Class<?>> classes = initializers.get(containerInitializer);
		if (classes == null) {
			classes = new HashSet<Class<?>>();
			initializers.put(containerInitializer, classes);
		}
		classes.add(handlesClass);
	}

	public void addContextEnvironment(ContextEnvironment env) {
		contextEnvironments.add(env);
	}

	public void addContextResource(ContextResource res) {
		contextResources.add(res);
	}

	public void addContextEnvironment(String name, String value, String type) {
		ContextEnvironment env = new ContextEnvironment();
		env.setType(type);
		env.setName(name);
		env.setValue(value);
		addContextEnvironment(env);
	}

	public void addContextEnvironmentString(String name, String value) {
		addContextEnvironment(name, value, "java.lang.String");
	}

	public void start() throws LifecycleException, ServletException, InstantiationException, IllegalAccessException,
			ParserConfigurationException, SAXException, IOException {
		installSlf4jBridge();

		Tomcat tomcat = new Tomcat();
		tomcat.setPort(port);

		if (tempDir == null) {
			String baseDirName = contextPath;
			if ("/".equals(baseDirName)) {
				baseDirName = "_";
			} else {
				baseDirName = baseDirName.substring(1) + "_";
			}

			baseDirName += port;

			tempDir = System.getProperty("java.io.tmpdir") + "/" + baseDirName;
		}

		tomcat.setBaseDir(tempDir);
		tomcat.getConnector().setURIEncoding("UTF-8");

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
			for (Map.Entry<Class<ServletContainerInitializer>, Set<Class<?>>> entry : initializers.entrySet()) {
				ctx.addServletContainerInitializer(entry.getKey().newInstance(), entry.getValue());
			}

		}

		tomcat.start();
		((StandardManager) ctx.getManager()).setPathname("");

		tomcat.getServer().await();
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

		File homeDir = new File(System.getProperty("user.home"));

		List<URL> jars = new ArrayList<URL>();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new File("./pom.xml"));

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

					String pathName = new File(homeDir, ".m2/repository/" + artifactFileName).getPath();
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
