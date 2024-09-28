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
import org.smooks.cartridges.javabean.Bean;
import org.smooks.cartridges.templating.MockOutStreamResource;
import org.smooks.cartridges.templating.MyBean;
import org.smooks.cartridges.templating.TemplatingConfiguration;
import org.smooks.engine.DefaultApplicationContextBuilder;
import org.smooks.engine.resource.visitor.smooks.NestedSmooksVisitor;
import org.smooks.io.sink.StringSink;
import org.smooks.io.source.ReaderSource;
import org.smooks.io.source.StringSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public class FreeMarkerProgramaticConfigTest {

    @Test
    public void testFreeMarkerTrans_01() throws SAXException, IOException {
        Smooks nestedSmooks = new Smooks(new DefaultApplicationContextBuilder().withSystemResources(false).build());
        nestedSmooks.addVisitor(new FreeMarkerTemplateProcessor(new TemplatingConfiguration("/org/smooks/cartridges/templating/freemarker/test-template.ftl")), "c");

        NestedSmooksVisitor nestedSmooksVisitor = new NestedSmooksVisitor();
        nestedSmooksVisitor.setAction(Optional.of(NestedSmooksVisitor.Action.REPLACE));
        nestedSmooksVisitor.setNestedSmooks(nestedSmooks);

        Smooks smooks = new Smooks();
        Bean bean = new Bean(MyBean.class, "myBeanData", "c", smooks.getApplicationContext().getRegistry());
        smooks.addVisitors(bean.bindTo("x", "c/@x"));
        smooks.addVisitor(nestedSmooksVisitor, "c");

        test_ftl(smooks, "<a><b><c x='xvalueonc1'/><c x='xvalueonc2'/></b></a>", "<a><b><mybean>xvalueonc1</mybean><mybean>xvalueonc2</mybean></b></a>");
        // Test transformation via the <context-object/> by transforming the root element using StringTemplate.
        test_ftl(smooks, "<c x='xvalueonc1'/>", "<mybean>xvalueonc1</mybean>");
    }

    @Test
    public void test_nodeModel_1() {
        Smooks nestedSmooks = new Smooks(new DefaultApplicationContextBuilder().withSystemResources(false).build());
        nestedSmooks.addVisitor(new FreeMarkerTemplateProcessor(new TemplatingConfiguration("<#foreach c in a.b.c>'${c}'</#foreach>")), "a");

        NestedSmooksVisitor nestedSmooksVisitor = new NestedSmooksVisitor();
        nestedSmooksVisitor.setMaxNodeDepth(Integer.MAX_VALUE);
        nestedSmooksVisitor.setAction(Optional.of(NestedSmooksVisitor.Action.REPLACE));
        nestedSmooksVisitor.setNestedSmooks(nestedSmooks);

        Smooks smooks = new Smooks();
        smooks.addVisitor(nestedSmooksVisitor, "a");

        StringSink sink = new StringSink();
        smooks.filterSource(new StringSource("<a><b><c>cvalue1</c><c>cvalue2</c><c>cvalue3</c></b></a>"), sink);
        assertEquals("'cvalue1''cvalue2''cvalue3'", sink.getResult());
    }

    @Test
    public void test_nodeModel_2() throws IOException, SAXException {
        Smooks nestedSmooks = new Smooks(new DefaultApplicationContextBuilder().withSystemResources(false).build());
        nestedSmooks.addVisitor(new FreeMarkerTemplateProcessor(new TemplatingConfiguration("<x>'${c}'</x>")), "c");

        NestedSmooksVisitor nestedSmooksVisitor = new NestedSmooksVisitor();
        nestedSmooksVisitor.setMaxNodeDepth(Integer.MAX_VALUE);
        nestedSmooksVisitor.setAction(Optional.of(NestedSmooksVisitor.Action.REPLACE));
        nestedSmooksVisitor.setNestedSmooks(nestedSmooks);

        Smooks smooks = new Smooks();
        smooks.addVisitor(nestedSmooksVisitor, "c");

        test_ftl(smooks, "<a><b><c>cvalue1</c><c>cvalue2</c><c>cvalue3</c></b></a>", "<a><b><x>'cvalue1'</x><x>'cvalue2'</x><x>'cvalue3'</x></b></a>");
    }

    @Test
    public void testFreeMarkerTrans_bind() {
        StringReader input;
        ExecutionContext context;

        Smooks nestedSmooks = new Smooks(new DefaultApplicationContextBuilder().withSystemResources(false).build());
        nestedSmooks.addVisitor(new FreeMarkerTemplateProcessor(new TemplatingConfiguration("<mybean>${myBeanData.x}</mybean>")), "c");

        NestedSmooksVisitor nestedSmooksVisitor = new NestedSmooksVisitor();
        nestedSmooksVisitor.setMaxNodeDepth(Integer.MAX_VALUE);
        nestedSmooksVisitor.setAction(Optional.of(NestedSmooksVisitor.Action.BIND_TO));
        nestedSmooksVisitor.setBindIdOptional(Optional.of("mybeanTemplate"));
        nestedSmooksVisitor.setNestedSmooks(nestedSmooks);

        Smooks smooks = new Smooks();
        Bean bean = new Bean(MyBean.class, "myBeanData", "c", smooks.getApplicationContext().getRegistry());
        smooks.addVisitors(bean.bindTo("x", "c/@x"));
        smooks.addVisitor(nestedSmooksVisitor, "c");

        context = smooks.createExecutionContext();
        input = new StringReader("<a><b><c x='xvalueonc2'/></b></a>");
        smooks.filterSource(context, new ReaderSource<>(input), null);

        assertEquals("<mybean>xvalueonc2</mybean>", context.getBeanContext().getBean("mybeanTemplate"));

        context = smooks.createExecutionContext();
        input = new StringReader("<c x='xvalueonc1'/>");
        smooks.filterSource(context, new ReaderSource<>(input), null);
        assertEquals("<mybean>xvalueonc1</mybean>", context.getBeanContext().getBean("mybeanTemplate"));
    }

    @Test
    public void testInsertBefore() throws SAXException, IOException {
        Smooks nestedSmooks = new Smooks(new DefaultApplicationContextBuilder().withSystemResources(false).build());
        nestedSmooks.addVisitor(new FreeMarkerTemplateProcessor(new TemplatingConfiguration("/org/smooks/cartridges/templating/freemarker/test-template.ftl")), "c");

        NestedSmooksVisitor nestedSmooksVisitor = new NestedSmooksVisitor();
        nestedSmooksVisitor.setAction(Optional.of(NestedSmooksVisitor.Action.PREPEND_BEFORE));
        nestedSmooksVisitor.setNestedSmooks(nestedSmooks);

        Smooks smooks = new Smooks();
        Bean bean = new Bean(MyBean.class, "myBeanData", "b", smooks.getApplicationContext().getRegistry());
        smooks.addVisitors(bean.bindTo("x", "b/@x"));
        smooks.addVisitor(nestedSmooksVisitor, "c");

        test_ftl(smooks, "<a><b x='xvalueonc1'/><c/><d/></a>",
                "<a><b x=\"xvalueonc1\"></b><mybean>xvalueonc1</mybean><c></c><d></d></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-insert-before.xml"));
        test_ftl(smooks, "<a><b x='xvalueonc1'/><c/><d/></a>",
                "<a><b x=\"xvalueonc1\"/><mybean>xvalueonc1</mybean><c/><d/></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-insert-before.xml"));
        smooks.setFilterSettings(new FilterSettings(StreamFilterType.SAX_NG).setDefaultSerializationOn(false));
        test_ftl(smooks, "<a><b x='xvalueonc1'/><c>11<f/>11</c><d/></a>", "<mybean>xvalueonc1</mybean>");
    }

    @Test
    public void testInsertAfter() throws SAXException, IOException {
        Smooks nestedSmooks = new Smooks(new DefaultApplicationContextBuilder().withSystemResources(false).build());
        nestedSmooks.addVisitor(new FreeMarkerTemplateProcessor(new TemplatingConfiguration("/org/smooks/cartridges/templating/freemarker/test-template.ftl")), "c");

        NestedSmooksVisitor nestedSmooksVisitor = new NestedSmooksVisitor();
        nestedSmooksVisitor.setAction(Optional.of(NestedSmooksVisitor.Action.APPEND_AFTER));
        nestedSmooksVisitor.setNestedSmooks(nestedSmooks);

        Smooks smooks = new Smooks();
        Bean bean = new Bean(MyBean.class, "myBeanData", "b", smooks.getApplicationContext().getRegistry());
        smooks.addVisitors(bean.bindTo("x", "b/@x"));
        smooks.addVisitor(nestedSmooksVisitor, "c");

        test_ftl(smooks, "<a><b x='xvalueonc1'/><c/><d/></a>", "<a><b x=\"xvalueonc1\"/><c></c><mybean>xvalueonc1</mybean><d/></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-insert-after.xml"));
        test_ftl(smooks, "<a><b x='xvalueonc1'/><c/><d/></a>", "<a><b x=\"xvalueonc1\"/><c></c><mybean>xvalueonc1</mybean><d/></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-insert-after.xml"));
        smooks.setFilterSettings(new FilterSettings(StreamFilterType.SAX_NG).setDefaultSerializationOn(false));
        test_ftl(smooks, "<a><b x='xvalueonc1'/><c>11<f/>11</c><d/></a>", "<mybean>xvalueonc1</mybean>");
    }

    @Test
    public void testAddTo() throws SAXException, IOException {
        Smooks nestedSmooks = new Smooks(new DefaultApplicationContextBuilder().withSystemResources(false).build());
        nestedSmooks.addVisitor(
                new FreeMarkerTemplateProcessor(
                        new TemplatingConfiguration("/org/smooks/cartridges/templating/freemarker/test-template.ftl")
                ),
                "c"
        );

        NestedSmooksVisitor nestedSmooksVisitor = new NestedSmooksVisitor();
        nestedSmooksVisitor.setAction(Optional.of(NestedSmooksVisitor.Action.PREPEND_AFTER));
        nestedSmooksVisitor.setNestedSmooks(nestedSmooks);

        Smooks smooks = new Smooks();
        Bean bean = new Bean(MyBean.class, "myBeanData", "b", smooks.getApplicationContext().getRegistry());
        smooks.addVisitors(bean.bindTo("x", "b/@x"));
        smooks.addVisitor(nestedSmooksVisitor, "c");

        test_ftl(smooks, "<a><b x='xvalueonc1'/><c/><d/></a>", "<a><b x=\"xvalueonc1\"/><c><mybean>xvalueonc1</mybean></c><d/></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-addto.xml"));
        test_ftl(smooks, "<a><b x='xvalueonc1'/><c/><d/></a>",
                "<a><b x=\"xvalueonc1\"/><c><mybean>xvalueonc1</mybean></c><d/></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-addto.xml"));
        test_ftl(smooks, "<a><b x='xvalueonc1'/><c>1111</c><d/></a>",
                "<a><b x=\"xvalueonc1\"/><c>1111<mybean>xvalueonc1</mybean></c><d/></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-addto.xml"));
        test_ftl(smooks, "<a><b x='xvalueonc1'/><c><f/></c><d/></a>",
                "<a><b x=\"xvalueonc1\"/><c><f/><mybean>xvalueonc1</mybean></c><d/></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-addto.xml"));
        test_ftl(smooks, "<a><b x='xvalueonc1'/><c>11<f/>11</c><d/></a>",
                "<a><b x=\"xvalueonc1\"/><c>11<f/>11<mybean>xvalueonc1</mybean></c><d/></a>");

        smooks = new Smooks(getClass().getResourceAsStream("test-configs-addto.xml"));
        smooks.setFilterSettings(new FilterSettings(StreamFilterType.SAX_NG).setDefaultSerializationOn(false));
        test_ftl(smooks, "<a><b x='xvalueonc1'/><c>11<f/>11</c><d/></a>",
                "<mybean>xvalueonc1</mybean>");
    }

    @Test
    public void test_outputTo_Stream() {
        Smooks nestedSmooks = new Smooks(new DefaultApplicationContextBuilder().withSystemResources(false).build());
        nestedSmooks.addVisitor(new FreeMarkerTemplateProcessor(new TemplatingConfiguration("data to outstream")), "#document");

        NestedSmooksVisitor nestedSmooksVisitor = new NestedSmooksVisitor();
        nestedSmooksVisitor.setAction(Optional.of(NestedSmooksVisitor.Action.OUTPUT_TO));
        nestedSmooksVisitor.setNestedSmooks(nestedSmooks);
        nestedSmooksVisitor.setOutputStreamResourceOptional(Optional.of("outRes"));

        Smooks smooks = new Smooks();
        smooks.addVisitor(new MockOutStreamResource("outRes"), "#document");
        smooks.addVisitor(nestedSmooksVisitor);

        ExecutionContext context = smooks.createExecutionContext();

        MockOutStreamResource.outputStream = new ByteArrayOutputStream();
        smooks.filterSource(context, new StringSource("<a/>"), null);

        assertEquals("data to outstream", MockOutStreamResource.outputStream.toString());
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
