/*-
 * ========================LICENSE_START=================================
 * smooks-templating-cartridge
 * %%
 * Copyright (C) 2020 Smooks
 * %%
 * Licensed under the terms of the Apache License Version 2.0, or
 * the GNU Lesser General Public License version 3.0 or later.
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
 *
 * ======================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ======================================================================
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * =========================LICENSE_END==================================
 */
package org.smooks.cartridges.templating.xslt;

import org.junit.Test;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.cartridges.templating.util.CharUtils;
import org.smooks.engine.DefaultApplicationContextBuilder;
import org.smooks.engine.resource.config.DefaultResourceConfig;
import org.smooks.engine.resource.visitor.smooks.NestedSmooksVisitor;
import org.smooks.io.sink.StringSink;
import org.smooks.io.source.ReaderSource;
import org.smooks.io.source.StringSource;
import org.smooks.support.SmooksUtil;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author tfennelly
 */
public class XslContentHandlerFactoryTest {

    @Test
    public void testXslUnitTrans_filebased_replace() {
        Smooks smooks = new Smooks();
        ResourceConfig resourceConfig = new DefaultResourceConfig("p", new Properties(), "org/smooks/cartridges/templating/xslt/xsltransunit.xsl");

        System.setProperty("javax.xml.transform.TransformerFactory", org.apache.xalan.processor.TransformerFactoryImpl.class.getName());
        smooks.getApplicationContext().getRegistry().registerResourceConfig(resourceConfig);

        InputStream stream = getClass().getResourceAsStream("htmlpage.html");
        ExecutionContext context = smooks.createExecutionContext();
        String transResult = SmooksUtil.filterAndSerialize(context, stream, smooks);

        CharUtils.assertEquals("XSL Comparison Failure - See xsltransunit.expected1.", "/org/smooks/cartridges/templating/xslt/xsltransunit.expected1", transResult);
    }

    @Test
    public void testXslUnitTrans_parambased() {
        testXslUnitTrans_parambased(NestedSmooksVisitor.Action.PREPEND_BEFORE, "xsltransunit.expected2");
        testXslUnitTrans_parambased(NestedSmooksVisitor.Action.APPEND_AFTER, "xsltransunit.expected3");
        testXslUnitTrans_parambased(NestedSmooksVisitor.Action.APPEND_BEFORE, "xsltransunit.expected4");
        testXslUnitTrans_parambased(NestedSmooksVisitor.Action.REPLACE, "xsltransunit.expected5");
    }

    public void testXslUnitTrans_parambased(NestedSmooksVisitor.Action action, String expectedFileName) {
        ResourceConfig res = new DefaultResourceConfig("p", new Properties(), "<z id=\"{@id}\">Content from template!!</z>");
        res.setResourceType("xsl");
        res.setParameter(XslContentHandlerFactory.IS_XSLT_TEMPLATELET, "true");

        Smooks nestedSmooks = new Smooks(new DefaultApplicationContextBuilder().withSystemResources(false).build());
        nestedSmooks.addResourceConfig(res);

        NestedSmooksVisitor nestedSmooksVisitor = new NestedSmooksVisitor();
        nestedSmooksVisitor.setAction(Optional.of(action));
        nestedSmooksVisitor.setNestedSmooks(nestedSmooks);

        Smooks smooks = new Smooks();
        smooks.addVisitor(nestedSmooksVisitor, "p");

        System.setProperty("javax.xml.transform.TransformerFactory", org.apache.xalan.processor.TransformerFactoryImpl.class.getName());

        InputStream stream = getClass().getResourceAsStream("htmlpage.html");
        ExecutionContext context = smooks.createExecutionContext();
        String transResult = SmooksUtil.filterAndSerialize(context, stream, smooks);
        CharUtils.assertEquals("XSL Comparison Failure.  action=" + action + ".  See " + expectedFileName, "/org/smooks/cartridges/templating/xslt/" + expectedFileName, transResult);
    }

    @Test
    public void test_xsl_bind() throws SAXException, IOException {
        test_xsl_bind("test-configs-bind.xml");
        test_xsl_bind("test-configs-bind-ext.xml");
    }

    public void test_xsl_bind(String config) throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream(config));
        StringReader input;
        ExecutionContext context;

        input = new StringReader("<a><b><c/></b></a>");
        context = smooks.createExecutionContext();
        smooks.filterSource(context, new ReaderSource<>(input), null);

        assertEquals("<bind/>", context.getBeanContext().getBean("mybeanTemplate"));

        input = new StringReader("<c/>");
        context = smooks.createExecutionContext();
        smooks.filterSource(context, new ReaderSource<>(input), null);
        assertEquals("<bind/>", context.getBeanContext().getBean("mybeanTemplate"));
    }

    @Test
    public void test_inline_01() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("inline-01.xml"));
        StringSink sink = new StringSink();

        smooks.filterSource(new StringSource("<a/>"), sink);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><xxxxxx/>", sink.getResult());
    }

    @Test
    public void test_inline_xsl_function() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("inline-xsl.xml"));
        StringSink sink = new StringSink();

        smooks.filterSource(new StringSource("<a name='kalle'/>"), sink);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><x>kalle</x>", sink.getResult());
    }

    @Test
    public void test_inline_02() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("inline-02.xml"));
        StringSink sink = new StringSink();

        smooks.filterSource(new StringSource("<a/>"), sink);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>Hi there!", sink.getResult());
    }

    @Test
    public void test_inline_03() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("inline-03.xml"));
        StringSink sink = new StringSink();

        smooks.filterSource(new StringSource("<a/>"), sink);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><xxxxxx/>", sink.getResult());
    }

    @Test
    public void test_badxsl() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("bad-xsl-config.xml"));

        try {
            smooks.filterSource(smooks.createExecutionContext(), new ReaderSource<>(new StringReader("<doc/>")), null);
            fail("Expected SmooksConfigurationException.");
        } catch (SmooksConfigException e) {
            assertEquals("Error loading Templating resource: Target Profile: [[org.smooks.api.profile.Profile#default_profile]], Selector: [/*], Resource: [/org/smooks/cartridges/templating/xslt/bad-stylesheet.xsl], Num Params: [0]", e.getCause().getMessage());
        }
    }
}
