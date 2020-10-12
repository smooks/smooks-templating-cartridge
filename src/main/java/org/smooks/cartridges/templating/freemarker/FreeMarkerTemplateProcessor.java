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

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.URLTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.smooks.SmooksException;
import org.smooks.cartridges.templating.AbstractTemplateProcessor;
import org.smooks.cartridges.templating.TemplatingConfiguration;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.ordering.Consumer;
import org.smooks.event.report.annotation.VisitAfterReport;
import org.smooks.event.report.annotation.VisitBeforeReport;
import org.smooks.util.FreeMarkerTemplate;
import org.smooks.util.FreeMarkerUtils;
import org.smooks.xml.DomUtils;
import org.w3c.dom.Element;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

/**
 * <a href="http://freemarker.org/">FreeMarker</a> template application ProcessingUnit.
 * <p/>
 * See {@link org.smooks.cartridges.templating.freemarker.FreeMarkerContentHandlerFactory}.
 * <p/>
 * <b>NOTE</b> that this visitor supports the extra "<b>useNodeModel</b>" parameter when
 * using DOM based filtering.  When set to true (default=false), the targeted
 * DOM element will be attached to the model that is passed to the FreeMarker
 * templating engine.  This allows the DOM model to be referenced from within
 * the FreeMarker template, with the targeted element name being the "root"
 * name when forming expressions.  See <a href="http://freemarker.org">freemarker.org</a>
 * for more info.
 *
 * @author tfennelly
 */
@VisitBeforeReport(summary = "FreeMarker Template - See Detail.", detailTemplate = "reporting/FreeMarkerTemplateProcessor_before.html")
@VisitAfterReport(summary = "FreeMarker Template - See Detail.", detailTemplate = "reporting/FreeMarkerTemplateProcessor_After.html")
public class FreeMarkerTemplateProcessor extends AbstractTemplateProcessor implements Consumer {
    
    @Inject
    @Named("templating.freemarker.defaultNumberFormat")
    private String defaultNumberFormat = FreeMarkerTemplate.DEFAULT_MACHINE_READABLE_NUMBER_FORMAT;

    private Template defaultTemplate;
    private Template templateBefore;
    private Template templateAfter;
    private SmooksResourceConfiguration config;

    /**
     * Default constructor.
     */
    protected FreeMarkerTemplateProcessor() {
    }

    /**
     * Programmatically configure the FreeMarker Templating Visitor.
     * @param templatingConfiguration The templating configuration.
     * @return This Visitor instance.
     */
    public FreeMarkerTemplateProcessor(TemplatingConfiguration templatingConfiguration) {
        super.setTemplatingConfiguration(templatingConfiguration);
    }

    @Override
	protected void loadTemplate(SmooksResourceConfiguration config) throws IOException {
        this.config = config;

        Configuration configuration = new Configuration(Configuration.VERSION_2_3_21);

        configuration.setSharedVariable("serialize", new NodeModelSerializer());
        configuration.setNumberFormat(defaultNumberFormat);

        if (config.isInline()) {
            byte[] templateBytes = config.getBytes();
            String[] templates = (new String(templateBytes)).split(AbstractTemplateProcessor.TEMPLATE_SPLIT_PI);

            if(templates.length == 1) {
                defaultTemplate = new Template("free-marker-template", new StringReader(templates[0]), configuration);
            } else if(templates.length == 2) {
                if(getAction() != Action.REPLACE) {
                    throw new UnsupportedOperationException("Split templates only supported on the REPLACE action.");
                }
                templateBefore = new Template("free-marker-template-before", new StringReader(templates[0]), configuration);
                templateAfter = new Template("free-marker-template-after", new StringReader(templates[1]), configuration);
            } else {
                throw new IOException("Invalid FreeMarker template config.  Zero split tokens.");
            }
        } else {
            TemplateLoader[] loaders = new TemplateLoader[]{new FileTemplateLoader(), new ContextClassLoaderTemplateLoader()};
            MultiTemplateLoader multiLoader = new MultiTemplateLoader(loaders);

            configuration.setTemplateLoader(multiLoader);
            defaultTemplate = configuration.getTemplate(config.getResource());
        }
    }

    public boolean consumes(Object object) {
        if(defaultTemplate != null && defaultTemplate.toString().contains(object.toString())) {
            return true;
        } else if(templateBefore != null && templateBefore.toString().contains(object.toString())) {
            return true;
        } else {
            return templateAfter != null && templateAfter.toString().contains(object.toString());
        }
    }

    @Override
    protected void applyTemplateToOutputStream(Element element, String outputStreamResourceName, ExecutionContext executionContext, Writer writer) {
        applyTemplate(defaultTemplate, element, executionContext, writer);
    }

    @Override
    protected boolean beforeApplyTemplate(Element element, ExecutionContext executionContext, Writer writer) {
        if (getAction().equals(Action.REPLACE)) {
            if (templateBefore != null) {
                applyTemplate(templateBefore, element, executionContext, writer);
            } else {
                return false;
            }
        } else {
            applyTemplate(defaultTemplate, element, executionContext, writer);
        }

        return true;
    }

    @Override
    protected boolean afterApplyTemplate(Element element, ExecutionContext executionContext, Writer writer) {
        if (getAction().equals(Action.ADD_TO) || getAction().equals(Action.BIND_TO) || getAction().equals(Action.INSERT_AFTER)) {
            applyTemplate(defaultTemplate, element, executionContext, writer);
        } else if (getAction().equals(Action.REPLACE)) {
            if(templateAfter != null) {
                applyTemplate(templateAfter, element, executionContext, writer);
            } else {
                applyTemplate(defaultTemplate, element, executionContext, writer);
            }
        }
        
        return true;
    }

    protected void applyTemplate(Template template, Element element, ExecutionContext executionContext, Writer writer) throws SmooksException {
        try {
            Map<String, Object> model = FreeMarkerUtils.getMergedModel(executionContext);
            template.process(model, writer);
        } catch (TemplateException | IOException e) {
            throw new SmooksException("Failed to apply FreeMarker template to fragment '" + DomUtils.getXPath(element) + "'.  Resource: " + config, e);
        }
    }

    private static class ContextClassLoaderTemplateLoader extends URLTemplateLoader {
        @Override
		protected URL getURL(String name) {
            return Thread.currentThread().getContextClassLoader().getResource(name);
        }
    }
}