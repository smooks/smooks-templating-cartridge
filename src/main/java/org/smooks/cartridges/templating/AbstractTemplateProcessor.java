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
package org.smooks.cartridges.templating;

import org.smooks.api.ApplicationContext;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.visitor.sax.ng.AfterVisitor;
import org.smooks.api.resource.visitor.sax.ng.BeforeVisitor;
import org.smooks.assertion.AssertArgument;
import org.smooks.engine.resource.config.DefaultResourceConfig;
import org.smooks.io.Stream;
import org.w3c.dom.Element;

import jakarta.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Abstract template processing unit.
 * <p/>
 * Defines abstract methods for loading the template in question, as well as convienience methods for
 * processing the template action against the templating result (REPLACE, ADD_TO, INSERT_BEFORE and INSERT_AFTER).
 * <p/>
 * See implementations.
 * @author tfennelly
 */
@SuppressWarnings("unchecked")
public abstract class AbstractTemplateProcessor implements BeforeVisitor, AfterVisitor {

    /**
     * Template split point processing instruction.
     */
    public static final String TEMPLATE_SPLIT_PI = "<\\?TEMPLATE-SPLIT-PI\\?>";
    
    private TemplatingConfiguration templatingConfiguration;

    @Inject
    private Boolean applyTemplateBefore = false;

    @Inject
    private Charset encoding = StandardCharsets.UTF_8;

    @Inject
    private ResourceConfig resourceConfig;

    @Inject
    private ApplicationContext applicationContext;
    
    @PostConstruct
    public void postConstruct() {
        if (templatingConfiguration != null) {
            ResourceConfig config = new DefaultResourceConfig();

            config.setResource(templatingConfiguration.getTemplate());
            
            try {
                loadTemplate(config);
            } catch (Exception e) {
                throw new SmooksConfigException("Error loading Templating resource: " + config, e);
            }
        } else if (resourceConfig != null) {
            if (resourceConfig.getResource() == null) {
                throw new SmooksConfigException("Templating resource undefined in resource configuration: " + resourceConfig);
            }

            try {
                loadTemplate(resourceConfig);
            } catch (Exception e) {
                throw new SmooksConfigException("Error loading Templating resource: " + resourceConfig, e);
            }
        } else {
            throw new SmooksConfigException(getClass().getSimpleName() + " not configured.");
        }
    }

    protected void setTemplatingConfiguration(TemplatingConfiguration templatingConfiguration) {
        AssertArgument.isNotNull(templatingConfiguration, "templatingConfiguration");
        this.templatingConfiguration = templatingConfiguration;
    }

    protected abstract void loadTemplate(ResourceConfig resourceConfig) throws IOException, TransformerConfigurationException;

    public boolean applyTemplateBefore() {
        return applyTemplateBefore;
    }
    
    public Charset getEncoding() {
        return encoding;
    }
    
    @Override
    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        if (applyTemplateBefore()) {
            applyTemplate(element, executionContext, Stream.out(executionContext));
        }
    }
    
    protected abstract void applyTemplate(Element element, ExecutionContext executionContext, Writer writer);
    
    @Override
    public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
        if (!applyTemplateBefore()) {
            applyTemplate(element, executionContext, Stream.out(executionContext));
        }
    }
}
