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
package org.smooks.cartridges.templating.stringtemplate;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.smooks.SmooksException;
import org.smooks.cartridges.templating.AbstractTemplateProcessor;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.injector.Scope;
import org.smooks.registry.lookup.LifecycleManagerLookup;
import org.smooks.container.ApplicationContext;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.ContentHandler;
import org.smooks.delivery.ContentHandlerFactory;
import org.smooks.delivery.ordering.Consumer;
import org.smooks.event.report.annotation.VisitAfterReport;
import org.smooks.event.report.annotation.VisitBeforeReport;
import org.smooks.lifecycle.phase.PostConstructLifecyclePhase;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * StringTemplate {@link org.smooks.delivery.dom.DOMElementVisitor} Creator class.
 * <p/>
 * Creates {@link org.smooks.delivery.dom.DOMElementVisitor} instances for applying
 * <a href="http://www.stringtemplate.org/">StringTemplate</a> transformations (i.e. ".st" files).
 * <p/>
 * This templating solution relies on the <a href="http://milyn.codehaus.org/downloads">Smooks JavaBean Cartridge</a>
 * to perform the JavaBean population that's required by <a href="http://www.stringtemplate.org/">StringTemplate</a>.
 *
 * <h2>Targeting ".st" Files for Transformation</h2>
 * <pre>
 * &lt;resource-config selector="<i>target-element</i>"&gt;
 *     &lt;!-- See {@link org.smooks.resource.URIResourceLocator} --&gt;
 *     &lt;resource&gt;<b>/com/acme/AcmeStringTemplate.st</b>&lt;/resource&gt;
 *
 *     &lt;!-- (Optional) The action to be applied on the template content. Should the content
 *          generated by the template:
 *          1. replace ("REPLACE") the target element, or
 *          2. be added to ("ADD_TO") the target element, or
 *          3. be inserted before ("INSERT_BEFORE") the target element, or
 *          4. be inserted after ("INSERT_AFTER") the target element.
 *          5. be bound to ("BIND_TO") a {@link org.smooks.javabean.context.BeanContext} variable named by the "bindId" param.
 *          Default "replace".--&gt;
 *     &lt;param name="<b>action</b>"&gt;<i>REPLACE/ADD_TO/INSERT_BEFORE/INSERT_AFTER</i>&lt;/param&gt;
 *
 *     &lt;!-- (Optional) Should the template be applied before (true) or
 *             after (false) Smooks visits the child elements of the target element.
 *             Default "false".--&gt;
 *     &lt;param name="<b>applyTemplateBefore</b>"&gt;<i>true/false</i>&lt;/param&gt;
 *
 *     &lt;!-- (Optional) The name of the {@link org.smooks.io.AbstractOutputStreamResource OutputStreamResource}
 *             to which the result should be written. If set, the "action" param is ignored. --&gt;
 *     &lt;param name="<b>outputStreamResource</b>"&gt;<i>xyzResource</i>&lt;/param&gt;
 *
 *     &lt;!-- (Optional) Template encoding.
 *          Default "UTF-8".--&gt;
 *     &lt;param name="<b>encoding</b>"&gt;<i>encoding</i>&lt;/param&gt;
 *
 *     &lt;!-- (Optional) bindId when "action" is "BIND_TO".
 *     &lt;param name="<b>bindId</b>"&gt;<i>xxxx</i>&lt;/param&gt;
 *
 * &lt;/resource-config&gt;
 * </pre>
 *
 * @author tfennelly
 */
public class StringTemplateContentHandlerFactory implements ContentHandlerFactory {

	@Inject
	private ApplicationContext applicationContext;

	/**
	 * Create a StringTemplate based ContentHandler.
     * @param resourceConfig The SmooksResourceConfiguration for the StringTemplate.
     * @return The StringTemplate {@link org.smooks.delivery.ContentHandler} instance.
	 */
	public synchronized ContentHandler create(SmooksResourceConfiguration resourceConfig) throws SmooksConfigurationException {
        final StringTemplateTemplateProcessor stringTemplateTemplateProcessor = new StringTemplateTemplateProcessor();
        try {
            applicationContext.getRegistry().lookup(new LifecycleManagerLookup()).applyPhase(stringTemplateTemplateProcessor, new PostConstructLifecyclePhase(new Scope(applicationContext.getRegistry(), resourceConfig, stringTemplateTemplateProcessor)));
            return stringTemplateTemplateProcessor;
        } catch (SmooksConfigurationException e) {
            throw e;
        } catch (Exception e) {
			InstantiationException instanceException = new InstantiationException("StringTemplate ProcessingUnit resource [" + resourceConfig.getResource() + "] not loadable.  StringTemplate resource invalid.");
			instanceException.initCause(e);
            throw new SmooksException(instanceException.getMessage(), instanceException);
		}
	}

    @Override
    public String getType() {
        return "st";
    }

    /**
	 * StringTemplate template application ProcessingUnit.
	 * @author tfennelly
	 */
    @VisitBeforeReport(condition = "false")
    @VisitAfterReport(summary = "Applied StringTemplate Template.", detailTemplate = "reporting/StringTemplateTemplateProcessor_After.html")
	private static class StringTemplateTemplateProcessor extends AbstractTemplateProcessor implements Consumer {

        private StringTemplate template;

        @Override
		protected void loadTemplate(SmooksResourceConfiguration config) {
            String path = config.getResource();

            if(path.charAt(0) == '/') {
                path = path.substring(1);
            }
            if(path.endsWith(".st")) {
                path = path.substring(0, path.length() - 3);
            }

            StringTemplateGroup templateGroup = new StringTemplateGroup(path);
            templateGroup.setFileCharEncoding(getEncoding().displayName());
            template = templateGroup.getInstanceOf(path);
        }

        protected void applyTemplate(ExecutionContext executionContext, Writer writer) {
            // First thing we do is clone the template for this transformation...
            StringTemplate thisTransTemplate = template.getInstanceOf();
            Map<String, Object> beans = executionContext.getBeanContext().getBeanMap();
            String templatingResult;

            // Set the document data beans on the template and apply it...
            thisTransTemplate.setAttributes(beans);
            templatingResult = thisTransTemplate.toString();

            try {
                writer.write(templatingResult);
            } catch (IOException e) {
                throw new SmooksException(e.getMessage(), e);
            }
        }

        public boolean consumes(Object object) {
            return template.getTemplate().contains(object.toString());
        }

        @Override
        protected void applyTemplateToOutputStream(Element element, String outputStreamResourceName, ExecutionContext executionContext, Writer writer) {
            applyTemplate(executionContext, writer);
        }

        @Override
        protected boolean beforeApplyTemplate(Element element, ExecutionContext executionContext, Writer writer) {
            if (applyTemplateBefore() || getAction().equals(Action.INSERT_BEFORE)) {
                applyTemplate(executionContext, writer);
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected boolean afterApplyTemplate(Element element, ExecutionContext executionContext, Writer writer) {
            if (!applyTemplateBefore()) {
                applyTemplate(executionContext, writer);
                return true;
            } else {
                return false;
            }
        }
    }
}
