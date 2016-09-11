/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.artifact;

import org.mule.runtime.config.spring.MuleArtifactContext;
import org.mule.runtime.core.api.MuleContext;

public class ArtifactContext {

  private MuleContext muleContext;
  private MuleArtifactContext muleArtifactContext;

  public ArtifactContext(MuleContext muleContext, MuleArtifactContext muleArtifactContext) {
    this.muleContext = muleContext;
    this.muleArtifactContext = muleArtifactContext;
  }

  public MuleContext getMuleContext() {
    return this.muleContext;
  }

  public MuleArtifactContext getMuleArtifactContext() {
    return muleArtifactContext;
  }
}
