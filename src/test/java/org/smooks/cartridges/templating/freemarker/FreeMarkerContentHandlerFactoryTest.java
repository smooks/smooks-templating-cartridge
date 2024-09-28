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
package org.smooks.cartridges.templating.freemarker;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.smooks.FilterSettings;
import org.smooks.Smooks;
import org.smooks.StreamFilterType;
import org.smooks.api.ExecutionContext;
import org.smooks.cartridges.templating.MyBean;
import org.smooks.io.sink.WriterSink;
import org.smooks.io.source.JavaSource;
import org.smooks.io.sink.StringSink;
import org.smooks.io.source.ReaderSource;
import org.smooks.io.source.StringSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author tfennelly
 */
public class FreeMarkerContentHandlerFactoryTest {

    @Test
    public void testFreeMarkerTrans_01() throws SAXException, IOException {
        Smooks smooks = new Smooks("/org/smooks/cartridges/templating/freemarker/test-configs-01.xml");

        test_ftl(smooks, "<a><b><c x='xvalueonc1' /><c x='xvalueonc2' /></b></a>", "<a><b><mybean>xvalueonc1</mybean><mybean>xvalueonc2</mybean></b></a>");
        // Test transformation via the <context-object /> by transforming the root element using StringTemplate.
        test_ftl(smooks, "<c x='xvalueonc1' />", "<mybean>xvalueonc1</mybean>");
    }

    @Test
    public void test_nodeModel_1() throws IOException, SAXException {
        Smooks smooks = new Smooks("/org/smooks/cartridges/templating/freemarker/test-configs-05.xml");

        StringSink sink = new StringSink();
        smooks.filterSource(new StringSource("<a><b><c>cvalue1</c><c>cvalue2</c><c>cvalue3</c></b></a>"), sink);
        assertEquals("'cvalue1''cvalue2''cvalue3'", sink.toString());
    }


    @Test
    public void test_nodeModel_2() throws IOException, SAXException {
        Smooks smooks = new Smooks("/org/smooks/cartridges/templating/freemarker/test-configs-06.xml");
        test_ftl(smooks, "<a><b><c>cvalue1</c><c>cvalue2</c><c>cvalue3</c></b></a>", "<a><b><x>'cvalue1'</x><x>'cvalue2'</x><x>'cvalue3'</x></b></a>");
    }


    @Test
    public void test_nodeModel_3() throws IOException, SAXException {
        Smooks smooks = new Smooks("/org/smooks/cartridges/templating/freemarker/test-configs-07.xml");

        StringSink sink = new StringSink();
        smooks.filterSource(new StringSource("<a><b javabind='javaval'><c>cvalue1</c><c>cvalue2</c><c>cvalue3</c></b></a>"), sink);
        assertEquals("'cvalue1''cvalue2''cvalue3' javaVal=javaval", sink.toString());
    }

    @Test
    public void testFreeMarkerTrans_01_NS() throws SAXException, IOException {
        Smooks smooks = new Smooks("/org/smooks/cartridges/templating/freemarker/test-configs-01-NS.xml");

        test_ftl(smooks, "<a xmlns:x=\"http://x\"><b><x:c x='xvalueonc1' /><c x='xvalueonc2' /></b></a>", "<a xmlns:x=\"http://x\"><b><mybean>xvalueonc1</mybean><c x=\"xvalueonc2\"></c></b></a>");
    }

    @Test
    public void testFreeMarkerTrans_02() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("test-configs-02.xml"));

        test_ftl(smooks, "<a><b><c x='xvalueonc1' /><c x='xvalueonc2' /></b></a>", "<a><b><mybean>xvalueonc1</mybean><mybean>xvalueonc2</mybean></b></a>");
        // Test transformation via the <context-object /> by transforming the root element using StringTemplate.
        test_ftl(smooks, "<c x='xvalueonc1' />", "<mybean>xvalueonc1</mybean>");
    }

    @Test
    public void testFreeMarkerTrans_03() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("test-configs-03.xml"));

        // Initialise the input bean map...
        Map<String, Object> myBeans = new HashMap<String, Object>();
        MyBean myBean = new MyBean();
        myBean.setX("xxxxxxx");
        myBeans.put("myBeanData", myBean);

        JavaSource source = new JavaSource(myBeans);
        source.setEventStreamRequired(false);

        // Create the output writer for the transform and run it...
        StringWriter myTransformResult = new StringWriter();
        smooks.filterSource(smooks.createExecutionContext(), source, new WriterSink<>(myTransformResult));

        // Check it...
        assertEquals("<mybean>xxxxxxx</mybean>", myTransformResult.toString());
    }

    @Test
    public void testFreeMarkerTrans_bind() throws SAXException, IOException {
        testFreeMarkerTrans_bind("test-configs-04.xml");
    }

    public void testFreeMarkerTrans_bind(String config) throws SAXException, IOException {
        Smooks smooks = new Smooks("/org/smooks/cartridges/templating/freemarker/" + config);
        StringReader input;
        ExecutionContext context;

        context = smooks.createExecutionContext();
        input = new StringReader("<a><b><c x='xvalueonc2' /></b></a>");
        smooks.filterSource(context, new ReaderSource<>(input));

        assertEquals("<mybean>xvalueonc2</mybean>", context.getBeanContext().getBean("mybeanTemplate"));

        context = smooks.createExecutionContext();
        input = new StringReader("<c x='xvalueonc1' />");
        smooks.filterSource(context, new ReaderSource<>(input));
        assertEquals("<mybean>xvalueonc1</mybean>", context.getBeanContext().getBean("mybeanTemplate"));
    }

    @Test
    public void test_template_include() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("test-configs-include.xml"));

        test_ftl(smooks, "<a><c/></a>",
                "<maintemplate><included>blah</included></maintemplate>");
    }

    @Test
    public void testInsertBefore() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("test-configs-insert-before.xml"));

        test_ftl(smooks, "<a><b x='xvalueonc1' /><c/><d/></a>",
                "<a><b x=\"xvalueonc1\"></b><mybean>xvalueonc1</mybean><c></c><d></d></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-insert-before.xml"));
        smooks.setFilterSettings(new FilterSettings(StreamFilterType.SAX_NG).setDefaultSerializationOn(false));
        test_ftl(smooks, "<a><b x='xvalueonc1' /><c>11<f/>11</c><d/></a>",
                "<mybean>xvalueonc1</mybean>");
    }

    @Test
    public void testInsertAfter() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("test-configs-insert-after.xml"));

        test_ftl(smooks, "<a><b x='xvalueonc1' /><c/><d/></a>",
                "<a><b x=\"xvalueonc1\"></b><c></c><mybean>xvalueonc1</mybean><d></d></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-insert-after.xml"));
        smooks.setFilterSettings(new FilterSettings(StreamFilterType.SAX_NG).setDefaultSerializationOn(false));
        test_ftl(smooks, "<a><b x='xvalueonc1' /><c>11<f/>11</c><d/></a>",
                "<mybean>xvalueonc1</mybean>");
    }

    @Test
    public void testAddTo() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("test-configs-addto.xml"));
        test_ftl(smooks, "<a><b x='xvalueonc1' /><c/><d/></a>",
                "<a><b x=\"xvalueonc1\" /><c><mybean>xvalueonc1</mybean></c><d /></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-addto.xml"));
        test_ftl(smooks, "<a><b x='xvalueonc1' /><c>1111</c><d/></a>",
                "<a><b x=\"xvalueonc1\" /><c>1111<mybean>xvalueonc1</mybean></c><d /></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-addto.xml"));
        test_ftl(smooks, "<a><b x='xvalueonc1' /><c><f/></c><d/></a>",
                "<a><b x=\"xvalueonc1\" /><c><f /><mybean>xvalueonc1</mybean></c><d /></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-addto.xml"));
        test_ftl(smooks, "<a><b x='xvalueonc1' /><c>11<f/>11</c><d/></a>",
                "<a><b x=\"xvalueonc1\" /><c>11<f />11<mybean>xvalueonc1</mybean></c><d /></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-addto.xml"));
        smooks.setFilterSettings(new FilterSettings(StreamFilterType.SAX_NG).setDefaultSerializationOn(false));
        test_ftl(smooks, "<a><b x='xvalueonc1' /><c>11<f/>11</c><d/></a>",
                "<mybean>xvalueonc1</mybean>");
    }

    @Test
    public void testReplace() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("test-configs-replace.xml"));

        test_ftl(smooks, "<a><b x='xvalueonc1' /><c/><d/></a>",
                "<a><b x=\"xvalueonc1\"></b><mybean>xvalueonc1</mybean><d></d></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-replace.xml"));
        smooks.setFilterSettings(new FilterSettings(StreamFilterType.SAX_NG).setDefaultSerializationOn(false));
        test_ftl(smooks, "<a><b x='xvalueonc1' /><c>11<f/>11</c><d/></a>",
                "<mybean>xvalueonc1</mybean>");
    }

    @Test
    public void test_no_default_ser() throws SAXException, IOException {
        Smooks smooks = new Smooks(getClass().getResourceAsStream("test-configs-no-default-ser.xml"));

        StringSink sink = new StringSink();
        smooks.filterSource(new StringSource("<a><e><b x='xvalueonc1' /><c/><d/><b x='xvalueonc2' /></e></a>"), sink);
        assertEquals("<mybean>xvalueonc1</mybean><d/><mybean>xvalueonc2</mybean>", sink.toString());
    }

    private void test_ftl(Smooks smooks, String input, String expected) throws IOException, SAXException {
        ExecutionContext context = smooks.createExecutionContext();
        test_ftl(smooks, context, input, expected);
    }

    private void test_ftl(Smooks smooks, ExecutionContext context, String input, String expected) throws IOException, SAXException {
        StringSink sink = new StringSink();

        smooks.filterSource(context, new StringSource(input), sink);
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert.assertXMLEqual(expected, sink.getResult());
    }
}
