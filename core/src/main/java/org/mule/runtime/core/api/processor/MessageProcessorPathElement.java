/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import java.util.List;

public interface MessageProcessorPathElement {


  public MessageProcessorPathElement getParent();

  public void setParent(MessageProcessorPathElement parent);

  public List<MessageProcessorPathElement> getChildren();

  public MessageProcessorPathElement addChild(Processor mp);

  public MessageProcessorPathElement addChild(String name);

  public Processor getMessageProcessor();

  public String getName();

  public String getPath();


}
