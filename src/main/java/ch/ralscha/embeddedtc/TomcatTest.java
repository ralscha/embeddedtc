package ch.ralscha.embeddedtc;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Convenient JUnit test class that starts a Tomcat on port 9998 before the test
 * and stops it afterwards. Extend your JUnit test class with this class.
 * 
 * @author Ralph Schaer
 */
public class TomcatTest {
	private static EmbeddedTomcat et;

	/**
	 * Starts an embedded tomcat on port 9998. 
	 * Does not print any log messages and does not add a shutdown hook
	 */
	@BeforeClass
	public static void startServer() {
		et = new EmbeddedTomcat(9998);
		et.setSilent(true);
		et.dontAddShutdownHook();
		et.start();
	}

	/**
	 * Stops the Tomcat after running all the tests in the test class
	 */
	@AfterClass
	public static void stopServer() {
		et.stop();
	}
}
