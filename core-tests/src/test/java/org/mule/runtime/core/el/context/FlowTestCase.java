/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;

import org.junit.Test;

public class FlowTestCase extends AbstractELTestCase {

  public FlowTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Override
  public void setupFlowConstruct() throws Exception {
    flowConstruct = getTestFlow("flowName", Object.class);
  }

  @Test
  public void flowName() throws Exception {
    Event event = Event.builder(context).message(InternalMessage.builder().payload("").build()).exchangePattern(ONE_WAY)
        .flow(flowConstruct).build();
    assertEquals("flowName", evaluate("flow.name", event));
  }

  @Test
  public void assignToFlowName() throws Exception {
    Event event = Event.builder(context).message(InternalMessage.builder().payload("").build()).exchangePattern(ONE_WAY)
        .flow(flowConstruct).build();
    assertFinalProperty("flow.name='foo'", event);
  }

}
