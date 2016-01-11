/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExamParameterized;

@RunWith(PaxExamParameterized.class)
@Ignore("OSGi - PaxExamParameterized needs static configuration")
public class HttpListenerExpectHeaderStreamingAutoStringTestCase extends HttpListenerExpectHeaderStreamingNeverTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-listener-expect-header-streaming-auto-string-config.xml";
    }

    public HttpListenerExpectHeaderStreamingAutoStringTestCase(String persistentConnections)
    {
        super(persistentConnections);
    }

}
