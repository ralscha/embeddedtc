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

import java.util.Collections;
import java.util.List;

import javax.naming.NamingException;

import org.apache.naming.NamingEntry;
import org.apache.naming.resources.FileDirContext;

/**
 * Special FileDirContext that mapps /WEB-INF/classes to ./target/classes
 * 
 * @author Ralph Schaer
 */
public class TargetClassesContext extends FileDirContext {

	@Override
	protected List<NamingEntry> doListBindings(String name) throws NamingException {

		if ("/WEB-INF/classes".equals(name)) {
			FileDirContext fileDirContext = new FileDirContext();
			fileDirContext.setDocBase("./target/classes");
			NamingEntry namingEntry = new NamingEntry("/WEB-INF/classes", fileDirContext, -1);
			return Collections.singletonList(namingEntry);
		}

		return null;

	}

}
