/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;

@ExclusiveOptionals
public class ExclusiveCashier {

  @Parameter
  private String cashierName;

  @Parameter
  @Optional
  private String rothIRA;

  @Parameter
  @Optional
  private String pensionPlan;

  @Parameter
  @Optional
  private PhoneNumber phoneNumber;

  @ParameterGroup
  private BankAccount account;

  public String getRothIRA() {
    return rothIRA;
  }

  public String getPensionPlan() {
    return pensionPlan;
  }

  public BankAccount getAccount() {
    return account;
  }
}
