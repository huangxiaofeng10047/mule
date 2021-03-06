/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.routing.filter.ObjectFilter;

/**
 * A filter that accepts everything.
 */
public class AcceptAllFilter implements Filter, ObjectFilter {

  public static final AcceptAllFilter INSTANCE = new AcceptAllFilter();

  @Override
  public boolean accept(InternalMessage message, Event.Builder builder) {
    return true;
  }

  @Override
  public boolean accept(Object object) {
    return true;
  }
}
