/*
 * Copyright 2012-2015, the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trpr.platform.runtime.impl.config.spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.trpr.platform.core.impl.logging.LogFactory;
import org.trpr.platform.core.spi.logging.Logger;
import org.trpr.platform.runtime.common.RuntimeConstants;
import org.trpr.platform.runtime.common.RuntimeVariables;
import org.trpr.platform.runtime.impl.config.FileLocator;

/**
*
* The <code>PropertyPlaceholderConfigurer</code> is a sub-type of the Spring {@link org.springframework.beans.factory.config.PropertyPlaceholderConfigurer} that
* follows the following order for locating and loading properties:
* <pre>
* 	1. Check if default properties have been set using {@link PropertyPlaceholderConfigurer#setDefaultPropertiesOnClasspath(String)}
* 	2. Check if a properties file path(s) has been set on this class via the PropertyPlaceholderConfigurer methods
* 	3. Check if a properties file path has been set on this class using the Trooper config location i.e. file on config locations recognized by Trooper runtime
* 	4. Check {@link RuntimeVariables} for a runtime variable by name {@link RuntimeConstants#CONFIG_PROPERTIES_VAR} that points to a .properties file to load
* <pre>
* 
* The override behavior is therefore defined by the last loaded location - in this case,  RuntimeConstants#CONFIG_PROPERTIES_VAR would take precedence over all else.
* 
* @author  Regunath B
* @version 1.0 13 Feb 2013
*/
public class PropertyPlaceholderConfigurer extends org.springframework.beans.factory.config.PropertyPlaceholderConfigurer {
	
	/** The logger for this class */
	private static final Logger LOGGER = LogFactory.getLogger(PropertyPlaceholderConfigurer.class);
	
	/** The default properties location on the classpath */
	private String defaultPropertiesOnClasspath;
	
	/** Properties file on Trooper config location(s)*/
	private String propertiesOnConfigPath;

	/**
	 * Overriden super class method. Creates merged properties following the loading order described in the class summary
	 * @see org.springframework.core.io.support.PropertiesLoaderSupport#mergeProperties()
	 */
	protected Properties mergeProperties() throws IOException {
		Properties mergedProperties = new Properties();
		// check to see if default properties from classpath has been set
		if (this.getDefaultPropertiesOnClasspath() != null) {
			mergedProperties.load(new ClassPathResource(this.getDefaultPropertiesOnClasspath()).getInputStream());				
			// set this as the default properties
			super.setProperties(mergedProperties);
		}
		// get merged properties from all specified locations on super type
		mergedProperties = super.mergeProperties();
		// check if there is a properties path specified on the Trooper config paths
		if (this.getPropertiesOnConfigPath() != null) {
			FileReader fileReader = null;
			try {
				fileReader = new FileReader(FileLocator.findUniqueFile(this.getPropertiesOnConfigPath()));
				mergedProperties.load(fileReader);
			} catch (Exception e) {
				// we dont expect this to happen if the file is found. Log a warning and proceed with default values
				LOGGER.warn("Error loading property configurations from file : {}. Using defaults", this.getPropertiesOnConfigPath());
			} finally {
				if (fileReader != null) {
					fileReader.close();
				}
			}
		}
		// check to see if there is an override via RuntimeVariables
		String runtimePropertiesPath = RuntimeVariables.getVariable(RuntimeConstants.CONFIG_PROPERTIES_VAR);
		if (runtimePropertiesPath != null) {
			try {
				mergedProperties.load(new FileInputStream(new File(runtimePropertiesPath)));// not using FileLocator for this as we expect the path is absolute
			} catch (Exception e) {
				// we dont expect this to happen if the file is found. Log a warning and proceed with default values
				LOGGER.warn("Error loading property configurations from file specified in JVM starup : {}. Using defaults", runtimePropertiesPath);
			}
		}
		return mergedProperties; 
	}
	
	/** Getter/Setter methods*/
	public String getDefaultPropertiesOnClasspath() {
		return this.defaultPropertiesOnClasspath;
	}
	public void setDefaultPropertiesOnClasspath(String defaultPropertiesOnClasspath) {
		this.defaultPropertiesOnClasspath = defaultPropertiesOnClasspath;
	}
	public String getPropertiesOnConfigPath() {
		return this.propertiesOnConfigPath;
	}
	public void setPropertiesOnConfigPath(String propertiesOnConfigPath) {
		this.propertiesOnConfigPath = propertiesOnConfigPath;
	}
	
}
