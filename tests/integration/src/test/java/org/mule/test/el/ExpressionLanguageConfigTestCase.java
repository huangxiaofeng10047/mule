/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.test.AbstractIntegrationTestCase;

import java.text.DateFormat;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

public class ExpressionLanguageConfigTestCase extends AbstractIntegrationTestCase {

  ExpressionLanguage el;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/el/expression-language-config.xml";
  }

  @Before
  public void setup() {
    el = muleContext.getExpressionLanguage();
  }

  @Test
  public void testExpressionLanguageImport() {
    assertEquals(Locale.class, el.evaluate("loc"));
  }

  @Test
  public void testExpressionLanguageImportNoName() {
    assertEquals(DateFormat.class, el.evaluate("DateFormat"));
  }

  @Test
  public void testExpressionLanguageAlias() {
    assertEquals(muleContext.getConfiguration().getId(), el.evaluate("appName"));
  }

  @Test
  public void testExpressionLanguageGlobalFunction() {
    // NOTE: This indirectly asserts that echo() function defined in config file rather than external
    // function definition file is being used (otherwise hiOTHER' would be returned

    assertEquals("hi", el.evaluate("echo('hi')"));
  }

  @Test
  public void testExpressionLanguageGlobalFunctionFromFile() {
    assertEquals("hi", el.evaluate("echo2('hi')"));
  }

  @Test
  public void testExpressionLanguageGlobalFunctionUsingStaticContext() {
    assertEquals("Hello " + muleContext.getConfiguration().getId() + "!", el.evaluate("hello()"));
  }

  @Test
  public void testExpressionLanguageGlobalFunctionUsingMessageContext() throws Exception {
    Event event = getTestEvent("123");
    assertEquals("123appended", el.evaluate("appendPayload()", event, getTestFlow()));
  }

  @Test
  public void testExpressionLanguageGlobalFunctionUsingMessageContextAndImport() throws Exception {
    Event event = getTestEvent("123");
    assertEquals("321", el.evaluate("reversePayload()", event, getTestFlow()));
  }

  @Test
  public void testExpressionLanguageExecuteElement() throws Exception {
    flowRunner("flow").withPayload("foo").run();
  }

}
