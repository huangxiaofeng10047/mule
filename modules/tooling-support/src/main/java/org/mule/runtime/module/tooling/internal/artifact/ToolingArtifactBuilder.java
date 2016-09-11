/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact;

import org.mule.runtime.module.deployment.internal.application.DefaultApplicationFactory;

import java.io.File;
import java.io.IOException;

public class ToolingArtifactBuilder {

  private final DefaultApplicationFactory applicationFactory;
  private DevelopmentApplication application;

  public ToolingArtifactBuilder(DefaultApplicationFactory applicationFactory) {
    this.applicationFactory = applicationFactory;
  }

  public ToolingArtifactBuilder artifactContent(File artifactLocation) {
    try {
      this.application = applicationFactory.createArtifact(artifactLocation);
      application.install();
      this.application.lazyInit();
      this.application.start();
    } catch (IOException e) {
      //TODO handle exception properly
      throw new RuntimeException(e);
    }
    return this;
  }

  public DevelopmentApplication build() {
    return application;
  }

}
