/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classloader;

import static com.google.common.collect.Sets.newHashSet;
import static org.mule.runtime.core.util.Preconditions.checkNotNull;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.functional.api.classloading.isolation.PluginUrlClassification;
import org.mule.runtime.core.util.PropertiesUtils;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the {@link PluginUrlClassification} resources, exported pacakges and resources.
 *
 * @since 4.0
 */
public class PluginResourcesResolver {

  private static final String PLUGIN_PROPERTIES = "plugin.properties";
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ExtensionManagerAdapter extensionManager;

  /**
   * Creates an instance of the resolver.
   *
   * @param extensionManager {@link ExtensionManagerAdapter} to be used
   */
  public PluginResourcesResolver(ExtensionManagerAdapter extensionManager) {
    checkNotNull(extensionManager, "extensionManager cannot be null");
    this.extensionManager = extensionManager;
  }

  /**
   * Resolves for the given {@link PluginUrlClassification} the resources exported.
   *
   * @param pluginUrlClassification {@link PluginUrlClassification} to be resolved
   * @return {@link PluginResourcedClassification} with the resources resolved
   */
  public PluginResourcedClassification resolvePluginResources(PluginUrlClassification pluginUrlClassification) {
    URLClassLoader pluginCL = new URLClassLoader(pluginUrlClassification.getUrls().toArray(new URL[0]), null);
    URL manifestUrl = pluginCL.findResource("META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
    if (manifestUrl != null) {
      logger.debug("Plugin '{}' has extension descriptor therefore it will be handled as an extension",
                   pluginUrlClassification.getName());
      ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
      Set<String> exportPackages = newHashSet(extensionManifest.getExportedPackages());
      Set<String> exportResources = newHashSet(extensionManifest.getExportedResources());
      return new PluginResourcedClassification(pluginUrlClassification, exportPackages, exportResources);
    } else {
      logger.debug("Plugin '{}' will be handled as standard plugin", pluginUrlClassification.getName());
      ClassLoader pluginArtifactClassLoader =
          new URLClassLoader(pluginUrlClassification.getUrls().toArray(new URL[0]), null);
      URL pluginPropertiesUrl = pluginArtifactClassLoader.getResource(PLUGIN_PROPERTIES);
      if (pluginPropertiesUrl == null) {
        throw new IllegalStateException(PLUGIN_PROPERTIES + " couldn't be found for plugin: " +
            pluginUrlClassification.getName());
      }
      Properties pluginProperties;
      try {
        pluginProperties = PropertiesUtils.loadProperties(pluginPropertiesUrl);
      } catch (IOException e) {
        throw new RuntimeException("Error while reading plugin properties: " + pluginPropertiesUrl);
      }
      Set<String> exportPackages = newHashSet(pluginProperties.getProperty(EXPORTED_CLASS_PACKAGES_PROPERTY));
      Set<String> exportResources = newHashSet(pluginProperties.getProperty(EXPORTED_RESOURCE_PROPERTY));
      return new PluginResourcedClassification(pluginUrlClassification, exportPackages, exportResources);
    }
  }

}
