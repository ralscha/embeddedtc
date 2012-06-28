/**
 * Copyright 2012 Ralph Schaer <ralphschaer@gmail.com>
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

import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.startup.SetAllPropertiesRule;
import org.apache.tomcat.util.digester.Digester;

@SuppressWarnings("javadoc")
public class ContextConfig {
	private final List<ContextResource> resources = new ArrayList<ContextResource>();

	private final List<ContextEnvironment> environments = new ArrayList<ContextEnvironment>();

	public void addResource(final ContextResource res) {
		resources.add(res);
	}

	public void addEnvironment(final ContextEnvironment env) {
		environments.add(env);
	}

	public List<ContextResource> getResources() {
		return resources;
	}

	public List<ContextEnvironment> getEnvironments() {
		return environments;
	}

	public static Digester createDigester() {
		final Digester digester = new Digester();
		digester.setValidating(false);
		digester.setRulesValidation(true);

		digester.addObjectCreate("Context", ContextConfig.class);

		digester.addObjectCreate("Context/Environment", ContextEnvironment.class);
		digester.addSetProperties("Context/Environment");
		digester.addSetNext("Context/Environment", "addEnvironment");

		digester.addObjectCreate("Context/Resource", ContextResource.class);
		digester.addRule("Context/Resource", new SetAllPropertiesRule());
		digester.addSetNext("Context/Resource", "addResource");

		return digester;
	}

}
