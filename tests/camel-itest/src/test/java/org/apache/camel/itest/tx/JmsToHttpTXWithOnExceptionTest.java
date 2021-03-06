/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.itest.tx;

import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.ExchangeTimedOutException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.itest.utils.extensions.JmsServiceExtension;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test will look for the spring .xml file with the same class name but postfixed with -config.xml as filename.
 * <p/>
 */
@CamelSpringTest
@ContextConfiguration
public class JmsToHttpTXWithOnExceptionTest {
    @RegisterExtension
    public static JmsServiceExtension jmsServiceExtension = JmsServiceExtension.createExtension();

    @Autowired
    private ProducerTemplate template;

    @EndpointInject("ref:data")
    private Endpoint data;

    @EndpointInject("mock:JmsToHttpWithOnExceptionRoute")
    private MockEndpoint rollback;

    // the ok response to expect
    private String ok = "<?xml version=\"1.0\"?><reply><status>ok</status></reply>";
    private String noAccess = "<?xml version=\"1.0\"?><reply><status>Access denied</status></reply>";

    @Test
    void test404() {
        // use requestBody to force a InOut message exchange pattern ( = request/reply)
        // will send and wait for a response
        Object out = template.requestBodyAndHeader(data,
                "<?xml version=\"1.0\"?><request><status id=\"123\"/></request>", "user", "unknown");

        // compare response
        assertEquals(noAccess, out);
    }

    @Test
    void testRollback() throws Exception {
        // will rollback forever so we run 3 times or more
        rollback.expectedMinimumMessageCount(3);

        // use requestBody to force a InOut message exchange pattern ( = request/reply)
        // will send and wait for a response
        try {
            template.requestBodyAndHeader(data,
                    "<?xml version=\"1.0\"?><request><status id=\"123\"/></request>", "user", "guest");
            fail("Should throw an exception");
        } catch (RuntimeCamelException e) {
            assertTrue(e.getCause() instanceof ExchangeTimedOutException, "Should timeout");
        }

        rollback.assertIsSatisfied();
    }

    @Test
    void testOK() {
        // use requestBody to force a InOut message exchange pattern ( = request/reply)
        // will send and wait for a response
        Object out = template.requestBodyAndHeader(data,
                "<?xml version=\"1.0\"?><request><status id=\"123\"/></request>", "user", "Claus");

        // compare response
        assertEquals(ok, out);
    }

}
