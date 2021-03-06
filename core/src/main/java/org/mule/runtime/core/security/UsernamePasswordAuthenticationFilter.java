/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.security;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.security.Authentication;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.config.i18n.CoreMessages;

/**
 * Performs authentication based on a username and password. The username and password are retrieved from the {@link Message}
 * based on expressions specified via the username and password setters. These are then used to create a DefaultMuleAuthentication
 * object which is passed to the authenticate method of the {@link SecurityManager}.
 */
public class UsernamePasswordAuthenticationFilter extends AbstractAuthenticationFilter {

  private String username = "#[message.inboundProperties.username]";
  private String password = "#[message.inboundProperties.password]";

  public UsernamePasswordAuthenticationFilter() {
    super();
  }

  /**
   * Authenticates the current message.
   *
   * @param event the current message recieved
   * @throws org.mule.runtime.core.api.security.SecurityException if authentication fails
   */
  @Override
  public Event authenticate(Event event)
      throws SecurityException, SecurityProviderNotFoundException, UnknownAuthenticationTypeException {
    Authentication authentication = getAuthenticationToken(event);
    Authentication authResult;
    try {
      authResult = getSecurityManager().authenticate(authentication);
    } catch (UnauthorisedException e) {
      // Authentication failed
      if (logger.isDebugEnabled()) {
        logger.debug("Authentication request for user: " + username + " failed: " + e.toString());
      }
      throw new UnauthorisedException(CoreMessages.authFailedForUser(authentication.getPrincipal().toString()), e);
    }

    // Authentication success
    if (logger.isDebugEnabled()) {
      logger.debug("Authentication success: " + authResult.toString());
    }

    SecurityContext context = getSecurityManager().createSecurityContext(authResult);
    context.setAuthentication(authResult);
    event.getSession().setSecurityContext(context);

    return event;
  }

  protected Authentication getAuthenticationToken(Event event) throws UnauthorisedException {
    ExpressionLanguage expressionLanguage = muleContext.getExpressionLanguage();

    Object usernameEval = expressionLanguage.evaluate(username, event, null);
    Object passwordEval = expressionLanguage.evaluate(password, event, null);

    if (usernameEval == null) {
      throw new UnauthorisedException(CoreMessages.authNoCredentials());
    }

    if (passwordEval == null) {
      throw new UnauthorisedException(CoreMessages.authNoCredentials());
    }

    return new DefaultMuleAuthentication(new MuleCredentials(usernameEval.toString(), passwordEval.toString().toCharArray()));
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

}
