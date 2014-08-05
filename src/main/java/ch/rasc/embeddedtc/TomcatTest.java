/**
 * Copyright 2012-2013 Ralph Schaer <ralphschaer@gmail.com>
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

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Convenient JUnit test class that starts a Tomcat on port 9998 before the test and stops
 * it afterwards. Extend your JUnit test class with this class.
 *
 * @author Ralph Schaer
 */
public class TomcatTest {
	private static EmbeddedTomcat et;

	/**
	 * Starts an embedded tomcat on port 9998. Does not print any log messages and does
	 * not add a shutdown hook
	 */
	@BeforeClass
	public static void startServer() {

		final int port = 9998;

		// Cleaning temp directory
		final File tempDirectory = new File(".", "/target/tomcat." + port);
		deleteDir(tempDirectory);

		// Starting Tomcat
		et = new EmbeddedTomcat(port);
		et.setSilent(true);
		et.setPrivileged(true);
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

	private static void deleteDir(File dir) {
		if (dir.isDirectory()) {
			for (File child : dir.listFiles()) {
				deleteDir(child);
			}
		}
		dir.delete();
	}
}
