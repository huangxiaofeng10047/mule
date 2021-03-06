/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.descriptor;

import static org.mule.runtime.module.deployment.internal.descriptor.ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE;
import static org.mule.runtime.module.reboot.MuleContainerBootstrapUtils.getMuleAppDir;
import org.mule.runtime.core.util.PropertiesUtils;
import org.mule.runtime.core.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.BooleanUtils;

public class PropertiesDescriptorParser implements DescriptorParser<ApplicationDescriptor> {

  public static final String PROPERTY_REDEPLOYMENT_ENABLED = "redeployment.enabled";
  protected static final String PROPERTY_ENCODING = "encoding";
  public static final String PROPERTY_DOMAIN = "domain";
  // support not yet implemented for CL reversal
  protected static final String PROPERTY_CONFIG_RESOURCES = "config.resources";
  protected static final String PROPERTY_LOG_CONFIG_FILE = "log.configFile";

  @Override
  public ApplicationDescriptor parse(File descriptor, String artifactName) throws IOException {
    final Properties properties = PropertiesUtils.loadProperties(new FileInputStream(descriptor));

    ApplicationDescriptor appDescriptor = new ApplicationDescriptor(artifactName);
    appDescriptor.setEncoding(properties.getProperty(PROPERTY_ENCODING));
    appDescriptor.setDomain(properties.getProperty(PROPERTY_DOMAIN));

    final String resProps = properties.getProperty(PROPERTY_CONFIG_RESOURCES);
    String[] urls;
    if (StringUtils.isBlank(resProps)) {
      urls = new String[] {DEFAULT_CONFIGURATION_RESOURCE};
    } else {
      urls = resProps.split(",");
    }
    appDescriptor.setConfigResources(urls);

    String[] absoluteResourcePaths = convertConfigResourcesToAbsolutePatch(urls, artifactName);
    appDescriptor.setAbsoluteResourcePaths(absoluteResourcePaths);
    appDescriptor.setConfigResourcesFile(convertConfigResourcesToFile(absoluteResourcePaths));

    // supports true (case insensitive), yes, on as positive values
    appDescriptor.setRedeploymentEnabled(BooleanUtils
        .toBoolean(properties.getProperty(PROPERTY_REDEPLOYMENT_ENABLED, Boolean.TRUE.toString())));

    if (properties.containsKey(PROPERTY_LOG_CONFIG_FILE)) {
      appDescriptor.setLogConfigFile(new File(properties.getProperty(PROPERTY_LOG_CONFIG_FILE)));
    }
    return appDescriptor;
  }

  private File[] convertConfigResourcesToFile(String[] absoluteResourcePaths) {
    File[] configResourcesFile = new File[absoluteResourcePaths.length];
    for (int i = 0; i < absoluteResourcePaths.length; i++) {
      configResourcesFile[i] = new File(absoluteResourcePaths[i]);
    }
    return configResourcesFile;
  }

  private String[] convertConfigResourcesToAbsolutePatch(final String[] configResources, String appName) {
    String[] absoluteResourcePaths = new String[configResources.length];
    for (int i = 0; i < configResources.length; i++) {
      String resource = configResources[i];
      absoluteResourcePaths[i] = toAbsoluteFile(resource, appName);
    }
    return absoluteResourcePaths;
  }

  /**
   * Resolve a resource relative to an application root.
   *
   * @param path the relative path to resolve
   * @return absolute path, may not actually exist (check with File.exists())
   */
  protected String toAbsoluteFile(String path, String appName) {
    return getMuleAppDir(appName).getAbsolutePath() + File.separator + path;
  }
}
