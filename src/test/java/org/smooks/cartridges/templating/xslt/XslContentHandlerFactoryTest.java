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
import org.smooks.SmooksException;
import org.smooks.SmooksUtil;
import org.smooks.cartridges.templating.util.CharUtils;
import org.smooks.cdr.ResourceConfig;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.container.ExecutionContext;
import org.smooks.payload.StringResult;
import org.smooks.payload.StringSource;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 * @author tfennelly
 */
public class XslContentHandlerFactoryTest {

    @Test
    public void testXslUnitTrans_filebased_replace() {
        Smooks smooks = new Smooks();
        ResourceConfig resourceConfig = new ResourceConfig("p", "org/smooks/cartridges/templating/xslt/xsltransunit.xsl");

        System.setProperty("javax.xml.transform.TransformerFactory", org.apache.xalan.processor.TransformerFactoryImpl.class.getName());
        smooks.getApplicationContext().getRegistry().registerResourceConfig(resourceConfig);

        InputStream stream = getClass().getResourceAsStream("htmlpage.html");
        ExecutionContext context = smooks.createExecutionContext();
        String transResult = SmooksUtil.filterAndSerialize(context, stream, smooks);
    
        CharUtils.assertEquals("XSL Comparison Failure - See xsltransunit.expected1.", "/org/smooks/cartridges/templating/xslt/xsltransunit.expected1", transResult);
    }

    @Test
    public void testXslUnitTrans_parambased() {
        testXslUnitTrans_parambased("INSERT_BEFORE", "xsltransunit.expected2");
        testXslUnitTrans_parambased("INSERT_AFTER", "xsltransunit.expected3");
        testXslUnitTrans_parambased("ADD_TO", "xsltransunit.expected4");
        testXslUnitTrans_parambased("REPLACE", "xsltransunit.expected5");
    }

    public void testXslUnitTrans_parambased(String action, String expectedFileName) {
        Smooks smooks = new Smooks();
        ResourceConfig res = new ResourceConfig("p", "<z id=\"{@id}\">Content from template!!</z>");
        String transResult = null;

        System.setProperty("javax.xml.transform.TransformerFactory", org.apache.xalan.processor.TransformerFactoryImpl.class.getName());

        res.setResourceType("xsl");
        res.setParameter(XslContentHandlerFactory.IS_XSLT_TEMPLATELET, "true");
        res.setParameter("action", action);
        smooks.getApplicationContext().getRegistry().registerResourceConfig(res);
        
        try {
            InputStream stream = getClass().getResourceAsStream("htmlpage.html");
            ExecutionContext context = smooks.createExecutionContext();
            transResult = SmooksUtil.filterAndSerialize(context, stream, smooks);
        } catch (SmooksException e) {
            e.printStackTrace();
            fail("unexpected exception: " + e.getMessage());
        }
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
        smooks.filterSource(context, new StreamSource(input), null);
    
        assertEquals("<bind/>", context.getBeanContext().getBean("mybeanTemplate"));

        input = new StringReader("<c/>");
        context = smooks.createExecutionContext();
        smooks.filterSource(context, new StreamSource(input), null);
        assertEquals("<bind/>", context.getBeanContext().getBean("mybeanTemplate"));
    }

    @Test
    public void test_inline_01() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("inline-01.xml"));
        StringResult result = new StringResult();

        smooks.filterSource(new StringSource("<a/>"), result);
        assertEquals("<xxxxxx/>", result.getResult());
    }

    @Test
    public void test_inline_xsl_function() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("inline-xsl.xml"));
        StringResult result = new StringResult();

        smooks.filterSource(new StringSource("<a name='kalle'/>"), result);
        assertEquals("<x>kalle</x>", result.getResult());
    }

    @Test
    public void test_inline_02() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("inline-02.xml"));
        StringResult result = new StringResult();

        smooks.filterSource(new StringSource("<a/>"), result);
        assertEquals("Hi there!", result.getResult());
    }

    @Test
    public void test_inline_03() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("inline-03.xml"));
        StringResult result = new StringResult();

        smooks.filterSource(new StringSource("<a/>"), result);
        assertEquals("<xxxxxx/>", result.getResult());
    }

    @Test
    public void test_badxsl() throws IOException, SAXException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("bad-xsl-config.xml"));

        try {
            smooks.filterSource(smooks.createExecutionContext(), new StreamSource(new StringReader("<doc/>")), null);
            fail("Expected SmooksConfigurationException.");
        } catch (SmooksConfigurationException e) {
            assertEquals("Error loading Templating resource: Target Profile: [[org.smooks.profile.Profile#default_profile]], Selector: [#document], Selector Namespace URI: [null], Resource: [/org/smooks/cartridges/templating/xslt/bad-stylesheet.xsl], Num Params: [0]", e.getCause().getMessage());
        }
    }
}
