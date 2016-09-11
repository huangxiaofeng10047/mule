/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.connectivity;

import static java.lang.String.format;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.connectivity.ConnectionValidator;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingStrategy;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.registry.SpiServiceRegistry;

import java.util.Collection;

import javax.inject.Inject;

public class DefaultConnectionValidator implements ConnectionValidator, Initialisable {

  protected static final String NO_CONNECTIVITY_TESTING_STRATEGY_FOUND =
      format("No %s instances where found", ConnectivityTestingStrategy.class);

  private ServiceRegistry serviceRegistry = new SpiServiceRegistry();
  private Collection<ConnectivityTestingStrategy> connectivityTestingStrategies;

  @Inject
  private MuleContext muleContext;

  @Override
  public ConnectionValidationResult validateConnectivity(Object connectivityObject) {
    for (ConnectivityTestingStrategy connectivityTestingStrategy : connectivityTestingStrategies) {
      if (connectivityTestingStrategy.accepts(connectivityObject)) {
        return connectivityTestingStrategy.validateConnectivity(connectivityObject);
      }
    }
    throw new MuleRuntimeException(createStaticMessage("Could not do connectivity testing over object of type "
        + connectivityObject.getClass().getName()));
  }

  @Override
  public void initialise() throws InitialisationException {
    connectivityTestingStrategies =
        serviceRegistry.lookupProviders(ConnectivityTestingStrategy.class);

    for (ConnectivityTestingStrategy connectivityTestingStrategy : connectivityTestingStrategies) {
      if (connectivityTestingStrategy instanceof MuleContextAware) {
        ((MuleContextAware) connectivityTestingStrategy).setMuleContext(muleContext);
      }
    }

    if (connectivityTestingStrategies.isEmpty()) {
      throw new MuleRuntimeException(createStaticMessage(NO_CONNECTIVITY_TESTING_STRATEGY_FOUND));
    }
  }
}
