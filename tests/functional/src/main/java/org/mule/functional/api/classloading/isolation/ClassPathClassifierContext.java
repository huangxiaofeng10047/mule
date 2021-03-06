/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static com.google.common.collect.Lists.newArrayList;
import static org.mule.runtime.core.util.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a context that contains what is needed in order to do a classpath classification. It is used in
 * {@link ClassPathClassifier}.
 *
 * @since 4.0
 */
public class ClassPathClassifierContext {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Artifact rootArtifact;
  private final List<URL> classPathURLs;

  private final List<String> providedExclusions = newArrayList();
  private final List<String> providedInclusions = newArrayList();
  private final List<String> testExclusions = newArrayList();
  private final List<String> testInclusions = newArrayList();

  private final List<String> extraBootPackages = newArrayList();

  private final List<String> pluginCoordinates = newArrayList();
  private final List<Class> exportPluginClasses = newArrayList();
  private final List<String> excludedArtifacts = newArrayList();
  private boolean extensionMetadataGenerationEnabled = false;
  private File pluginResourcesFolder;

  /**
   * Creates a context used for doing the classification of the class path.
   *
   * @param rootArtifact {@link Artifact} to the root artifact being classified. Not null.
   * @param pluginResourcesFolder {@link File} where resources for classification will be created.
   * @param classPathURLs the whole set of {@link URL}s that were loaded by IDE/Maven Surefire plugin when running the test. Not
   * @param excludedArtifacts Maven artifacts to be excluded from artifact class loaders created here due to they are going to be
   *        added as boot packages. In format {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   * @param extraBootPackages {@link List} of {@link String}s containing the extra boot packages defined to be appended to the
   *        container in addition to the pre-defined ones.
   * @param providedExclusions Maven artifacts to be excluded from the provided scope direct dependencies of rootArtifact. In
   *        format {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   * @param providedInclusions Maven artifacts to be excluded from the provided scope direct dependencies of rootArtifact. In
   *        format {@code <groupId>:<artifactId>:[[<classifier>]:<version>]}.
   * @param testExclusions {@link List} of Maven coordinates to be excluded from application class loader.
   * @param testInclusions {@link List} of Maven coordinates to be included in application class loader.
   * @param pluginCoordinates {@link List} of Maven coordinates in format {@code <groupId>:<artifactId>} in order to create plugin
   *        {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}s
   * @param exportPluginClasses {@link List} of {@link Class} to be exported in addition to the ones already exported by the
   *        plugin, for testing purposes only.
   * @param extensionMetadataGenerationEnabled if while building the a plugin
   *        {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} for an
   *        {@link org.mule.runtime.extension.api.annotation.Extension} the metadata should be generated.
   * @throws IOException if an error happened while reading
   *         {@link org.mule.functional.classloading.isolation.utils.RunnerModuleUtils#EXCLUDED_PROPERTIES_FILE} file
   */
  public ClassPathClassifierContext(final Artifact rootArtifact,
                                    final File pluginResourcesFolder,
                                    final List<URL> classPathURLs,
                                    final List<String> excludedArtifacts,
                                    final List<String> extraBootPackages,
                                    final List<String> providedExclusions,
                                    final List<String> providedInclusions,
                                    final List<String> testExclusions,
                                    final List<String> testInclusions,
                                    final List<String> pluginCoordinates,
                                    final List<Class> exportPluginClasses,
                                    final boolean extensionMetadataGenerationEnabled)
      throws IOException {
    checkNotNull(rootArtifact, "rootArtifact cannot be null");
    checkNotNull(classPathURLs, "classPathURLs cannot be null");

    this.rootArtifact = rootArtifact;
    this.pluginResourcesFolder = pluginResourcesFolder;
    this.classPathURLs = classPathURLs;

    this.excludedArtifacts.addAll(excludedArtifacts);
    this.extraBootPackages.addAll(extraBootPackages);

    this.providedExclusions.addAll(providedExclusions);
    this.providedInclusions.addAll(providedInclusions);

    this.testExclusions.addAll(testExclusions);
    this.testInclusions.addAll(testInclusions);

    this.pluginCoordinates.addAll(pluginCoordinates);
    this.exportPluginClasses.addAll(exportPluginClasses);
    this.extensionMetadataGenerationEnabled = extensionMetadataGenerationEnabled;
  }

  /**
   * @return a {@link Artifact} to the current (root) artifact being tested.
   */
  public Artifact getRootArtifact() {
    return rootArtifact;
  }

  /**
   * @return {@link File} where resources for classification will be created.
   */
  public File getPluginResourcesFolder() {
    return pluginResourcesFolder;
  }

  /**
   * @return a {@link List} of {@link URL}s for the classpath provided by JUnit (it is the complete list of URLs)
   */
  public List<URL> getClassPathURLs() {
    return classPathURLs;
  }

  /**
   * @return Maven artifacts to be excluded from the {@code provided} scope direct dependencies of the rootArtifact. In format
   *         {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   */
  public List<String> getProvidedExclusions() {
    return this.providedExclusions;
  }

  /**
   * @return Maven artifacts to be explicitly included from the {@code provided} scope direct dependencies of the rootArtifact. In
   *         format {@code <groupId>:<artifactId>:[[<classifier>]:<version>]}.
   */
  public List<String> getProvidedInclusions() {
    return this.providedInclusions;
  }

  /**
   * @return Maven artifacts to be excluded from artifact class loaders created here due to they are going to be added as boot
   *         packages. In format {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   */
  public List<String> getExcludedArtifacts() {
    return this.excludedArtifacts;
  }

  /**
   * Artifacts to be excluded from being added to application {@link ClassLoader} due to they are going to be in container
   * {@link ClassLoader}.
   * 
   * @return {@link Set} of Maven coordinates in the format:
   * 
   *         <pre>
   *         {@code <groupId>:<artifactId>:[[<extension>]:<version>]}.
   *         </pre>
   */
  public List<String> getTestExclusions() {
    return this.testExclusions;
  }

  /**
   * Artifacts to be included from being added to application {@link ClassLoader}.
   *
   * @return {@link Set} of Maven coordinates in the format:
   *
   *         <pre>
   *         {@code <groupId>:<artifactId>:[[<classifier>]:<version>]}.
   *         </pre>
   */
  public List<String> getTestInclusions() {
    return this.testInclusions;
  }

  /**
   * @return {@link List} of {@link String}s containing the extra boot packages defined to be appended to the container in
   *         addition to the pre-defined ones.
   */
  public List<String> getExtraBootPackages() {
    return this.extraBootPackages;
  }

  /**
   * @return {@link List} of {@link Class}es that are going to be exported in addition to the ones already exported by plugins.
   *         For testing purposes only.
   */
  public List<Class> getExportPluginClasses() {
    return this.exportPluginClasses;
  }

  /**
   * @return {@link List} of plugins as {@code [groupId]:[artifactId]} format for creates plugin
   *         {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}
   */
  public List<String> getPluginCoordinates() {
    return this.pluginCoordinates;
  }

  /**
   * @return {@code true} if while building the a plugin {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}
   *         for an {@link org.mule.runtime.extension.api.annotation.Extension} the metadata should be generated.
   */
  public boolean isExtensionMetadataGenerationEnabled() {
    return extensionMetadataGenerationEnabled;
  }

}
