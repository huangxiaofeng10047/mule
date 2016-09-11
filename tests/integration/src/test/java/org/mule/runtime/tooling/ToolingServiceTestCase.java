/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.repository.internal.RepositoryServiceFactory.MULE_REMOTE_REPOSITORIES_PROPERTY;
import static org.mule.runtime.module.repository.internal.RepositoryServiceFactory.MULE_REPOSITORY_FOLDER_PROPERTY;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.config.spring.dsl.api.config.ComponentConfiguration;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.repository.api.BundleDescriptor;
import org.mule.runtime.module.tooling.api.ToolingServiceAware;
import org.mule.runtime.module.repository.api.BundleNotFoundException;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.FakeMuleServer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ToolingServiceTestCase extends AbstractMuleTestCase {

  @Rule
  public SystemProperty repositoryFolderConfiguration =
      new SystemProperty(MULE_REPOSITORY_FOLDER_PROPERTY, "/Users/pablolagreca/.m2/repository");
  @Rule
  public SystemProperty repositoryRemoteConfiguration =
      new SystemProperty(MULE_REMOTE_REPOSITORIES_PROPERTY, "http://central.maven.org/maven2/");
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = none();
  private ToolingService toolingService;
  private FakeMuleServer fakeMuleServer;

  @Test
  public void nonExistentBundle() {
    ComponentConfiguration componentConfiguration =
        new ComponentConfiguration.Builder().setNamespace("mule").setIdentifier("configuration").build();
    ArtifactConfiguration artifactConfiguration = new ArtifactConfiguration(asList(componentConfiguration));

    expectedException.expect(BundleNotFoundException.class);
    toolingService.newConnectivityTestingServiceBuilder().setArtifactConfiguration(artifactConfiguration)
        .addExtension("org.mule.extensions", "mule-module-file", "4.0-SNAPSHOT").build();
  }

  @Test
  public void validFileConnectionWithInvalidFtpConnection() throws IOException {
    File applicationFolder = createApplicationFolder2("tooling/file-app",
                                                      getExtensionModuleBundleDescriptor("file"),
                                                      getExtensionModuleBundleDescriptor("ftp"));
    long initialTime = System.currentTimeMillis();
    DevelopmentApplication application =
        toolingService.newToolingApplicationArtifactBuilder().artifactContent(applicationFolder).build();
    ConnectionValidationResult connectionValidationResult = application.verifyConnectivity("fileConfig");
    System.out.println("time: " + (System.currentTimeMillis() - initialTime));
    assertThat(connectionValidationResult.isValid(), is(true));
  }

  @Test
  public void invalidFtpConnection() throws IOException {
    DevelopmentApplication application = createApplicationFolder("tooling/ftp-app", getExtensionModuleBundleDescriptor("ftp"));
    long initialTime = System.currentTimeMillis();
    ConnectionValidationResult connectionValidationResult = application.verifyConnectivity("ftpConfig");
    System.out.println("time: " + (System.currentTimeMillis() - initialTime));
    assertThat(connectionValidationResult.isValid(), is(false));
  }

  @Test
  public void resolveMetadataKeys() throws IOException {
    DevelopmentApplication application =
        createApplicationFolder("tooling/metadata-app", getExtensionTestModuleBundleDescriptor("mule-vegan-extension"));
    long initialTime = System.currentTimeMillis();
    MetadataResult<MetadataKeysContainer> metadataResult = application.retrieveMetadataKeys("veganConfig");
    assertThat(metadataResult.isSuccess(), is(true));
    System.out.println("time: " + (System.currentTimeMillis() - initialTime));
  }

  @Test
  public void resolveMetadata() throws IOException {
    DevelopmentApplication application =
            createApplicationFolder("tooling/metadata-app", getExtensionTestModuleBundleDescriptor("mule-vegan-extension"));
    long initialTime = System.currentTimeMillis();
    MetadataResult<TypeMetadataDescriptor> metadataResult = application.retrieveMetadata("appleOk/0", "APPLE");
    assertThat(metadataResult.isSuccess(), is(true));
    System.out.println("time: " + (System.currentTimeMillis() - initialTime));
  }


  private DevelopmentApplication createApplicationFolder(String applicationFolderPath, BundleDescriptor... bundleDescriptors)
      throws IOException {
    URL resource = getClass().getClassLoader().getResource(applicationFolderPath);
    File applicationFolder = new File(resource.getFile());
    copyDirectory(applicationFolder, new File(temporaryFolder.getRoot(), "testing-app"));
    File pluginsFolder = new File(applicationFolder, "plugins");
    pluginsFolder.mkdir();
    for (BundleDescriptor bundleDescriptor : bundleDescriptors) {
      File extensionPlugin = fakeMuleServer.getRepositoryService()
          .lookupBundle(bundleDescriptor);
      copyFile(extensionPlugin, new File(pluginsFolder, extensionPlugin.getName()));
    }
    return toolingService.newToolingApplicationArtifactBuilder().artifactContent(applicationFolder).build();
  }

  private File createApplicationFolder2(String applicationFolderPath, BundleDescriptor... bundleDescriptors)
      throws IOException {
    URL resource = getClass().getClassLoader().getResource(applicationFolderPath);
    File applicationFolder = new File(resource.getFile());
    copyDirectory(applicationFolder, new File(temporaryFolder.getRoot(), "testing-app"));
    File pluginsFolder = new File(applicationFolder, "plugins");
    pluginsFolder.mkdir();
    for (BundleDescriptor bundleDescriptor : bundleDescriptors) {
      File extensionPlugin = fakeMuleServer.getRepositoryService()
          .lookupBundle(bundleDescriptor);
      copyFile(extensionPlugin, new File(pluginsFolder, extensionPlugin.getName()));
    }
    return applicationFolder;
  }

  private BundleDescriptor getExtensionModuleBundleDescriptor(String extensionName) {
    return new BundleDescriptor.Builder()
        .setGroupId("org.mule.modules")
        .setArtifactId("mule-module-" + extensionName)
        .setVersion("4.0-SNAPSHOT")
        .setType("zip")
        .build();
  }

  private BundleDescriptor getExtensionTestModuleBundleDescriptor(String artifactId) {
    return new BundleDescriptor.Builder()
        .setGroupId("org.mule.tests")
        .setArtifactId(artifactId)
        .setVersion("4.0-SNAPSHOT")
        .setType("zip")
        .build();
  }

  @Before
  public void getConnectionTestingService() throws IOException, MuleException {
    TestCoreExtension testCoreExtension = new TestCoreExtension();
    fakeMuleServer = new FakeMuleServer(temporaryFolder.getRoot().getAbsolutePath(), Arrays.asList(testCoreExtension));
    fakeMuleServer.start();
    toolingService = testCoreExtension.getToolingService();
    //
    // MuleArtifactResourcesRegistry muleArtifactResourcesRegistry = new MuleArtifactResourcesRegistry();
    // toolingService = createToolingService(muleArtifactResourcesRegistry);
  }

  private RepositoryService createFakeRepositorySystem() {
    RepositoryService mockRepositoryService = mock(RepositoryService.class);
    when(mockRepositoryService.lookupBundle(any())).thenThrow(BundleNotFoundException.class);
    return mockRepositoryService;
  }

  public static class TestCoreExtension implements MuleCoreExtension, ToolingServiceAware {

    private ToolingService toolingService;

    public ToolingService getToolingService() {
      return toolingService;
    }

    @Override
    public void setContainerClassLoader(ArtifactClassLoader containerClassLoader) {

    }

    @Override
    public String getName() {
      return "tooling-test";
    }

    @Override
    public void dispose() {

    }

    @Override
    public void initialise() throws InitialisationException {

    }

    @Override
    public void start() throws MuleException {

    }

    @Override
    public void stop() throws MuleException {

    }

    @Override
    public void setToolingService(ToolingService toolingService) {
      this.toolingService = toolingService;
    }
  }

}
