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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.SmooksException;
import org.smooks.assertion.AssertArgument;
import org.smooks.cdr.ResourceConfig;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.container.ApplicationContext;
import org.smooks.container.ExecutionContext;
import org.smooks.delivery.Filter;
import org.smooks.delivery.Fragment;
import org.smooks.delivery.interceptor.WriterInterceptor;
import org.smooks.delivery.ordering.Producer;
import org.smooks.delivery.sax.ng.ElementVisitor;
import org.smooks.delivery.sax.ng.SaxNgSerializerVisitor;
import org.smooks.io.AbstractOutputStreamResource;
import org.smooks.io.NullWriter;
import org.smooks.javabean.repository.BeanId;
import org.smooks.util.CollectionsUtil;
import org.w3c.dom.Element;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

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
public abstract class AbstractTemplateProcessor implements ElementVisitor, Producer {

    /**
     * Template split point processing instruction.
     */
    public static final String TEMPLATE_SPLIT_PI = "<\\?TEMPLATE-SPLIT-PI\\?>";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTemplateProcessor.class);

    protected enum Action {
        REPLACE,
        ADD_TO,
        INSERT_BEFORE,
        INSERT_AFTER,
        BIND_TO,
    }

    private TemplatingConfiguration templatingConfiguration;
    private SaxNgSerializerVisitor targetWriter;
    private BeanId bindBeanId;

    @Inject
    private Boolean applyTemplateBefore = false;

    @Inject
    private Action action = Action.REPLACE;

    @Inject
    private Charset encoding = StandardCharsets.UTF_8;

    @Inject
    private Optional<String> bindId;

    @Inject
    private Optional<String> outputStreamResource;

    @Inject
    private ResourceConfig smooksResourceConfiguration;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    @Named(Filter.ENTITIES_REWRITE)
    private Boolean rewriteEntities = true;
    
    @PostConstruct
    public void postConstruct() {
        if (templatingConfiguration != null) {
            ResourceConfig config = new ResourceConfig();

            config.setResource(templatingConfiguration.getTemplate());

            Usage resultUsage = templatingConfiguration.getUsage();
            if (resultUsage == Inline.ADD_TO) {
                action = Action.ADD_TO;
            } else if (resultUsage == Inline.REPLACE) {
                action = Action.REPLACE;
            } else if (resultUsage == Inline.INSERT_BEFORE) {
                action = Action.INSERT_BEFORE;
            } else if (resultUsage == Inline.INSERT_AFTER) {
                action = Action.INSERT_AFTER;
            } else if (resultUsage instanceof BindTo) {
                action = Action.BIND_TO;
                bindId = Optional.of(((BindTo) resultUsage).getBeanId());
                bindBeanId = applicationContext.getBeanIdStore().register(bindId.get());
            } else if (resultUsage instanceof OutputTo) {
                outputStreamResource = Optional.ofNullable(((OutputTo) resultUsage).getOutputStreamResource());
            }

            try {
                loadTemplate(config);
            } catch (Exception e) {
                throw new SmooksConfigurationException("Error loading Templating resource: " + config, e);
            }
        } else if (smooksResourceConfiguration != null) {
            if (smooksResourceConfiguration.getResource() == null) {
                throw new SmooksConfigurationException("Templating resource undefined in resource configuration: " + smooksResourceConfiguration);
            }

            try {
                loadTemplate(smooksResourceConfiguration);
            } catch (Exception e) {
                throw new SmooksConfigurationException("Error loading Templating resource: " + smooksResourceConfiguration, e);
            }

            if (action == Action.BIND_TO) {
                if (!bindId.isPresent()) {
                    throw new SmooksConfigurationException("'BIND_TO' templating action configurations must also specify a 'bindId' configuration for the Id under which the result is bound to the ExecutionContext");
                } else {
                    bindBeanId = applicationContext.getBeanIdStore().register(bindId.get());
                }
            }

            targetWriter = new SaxNgSerializerVisitor();
            targetWriter.setRewriteEntities(Optional.of(rewriteEntities));
            targetWriter.setCloseEmptyElements(Optional.of(false));
            targetWriter.postConstruct();
        } else {
            throw new SmooksConfigurationException(getClass().getSimpleName() + " not configured.");
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

    public Set<String> getProducts() {
        return outputStreamResource.map(CollectionsUtil::toSet).orElseGet(CollectionsUtil::toSet);
    }

    protected Action getAction() {
        return action;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public String getBindId() {
        return bindId.orElse(null);
    }

    public String getOutputStreamResource() {
        return outputStreamResource.orElse(null);
    }

    @Override
    public void visitBefore(Element element, ExecutionContext executionContext) throws SmooksException {
        String outputStreamResourceName = getOutputStreamResource();
        if (outputStreamResourceName != null) {
            if (applyTemplateBefore()) {
                Writer writer = AbstractOutputStreamResource.getOutputWriter(outputStreamResourceName, executionContext);
                applyTemplateToOutputStream(element, outputStreamResourceName, executionContext, writer);
            }
        } else {
            if (getAction() == Action.INSERT_BEFORE) {
                // apply the template...
                beforeApplyTemplate(element, executionContext, executionContext.getWriter());
                // write the start of the element...
                if (executionContext.getDeliveryConfig().isDefaultSerializationOn()) {
                    targetWriter.visitBefore(element, executionContext);
                }
            } else if (getAction() == Action.REPLACE) {
                Writer currentWriter = executionContext.getWriter();
                if (currentWriter instanceof WriterInterceptor.ExclusiveWriter) {
                    ((WriterInterceptor.ExclusiveWriter) currentWriter).acquire();
                }

                if (!beforeApplyTemplate(element, executionContext, currentWriter) && executionContext.getDeliveryConfig().isDefaultSerializationOn()) {
                    // If Default Serialization is on, we want to block output to the
                    // output stream...
                    executionContext.setWriter(new NullWriter(currentWriter));
                }
            } else if (getAction() != Action.REPLACE && getAction() != Action.BIND_TO) {
                // write the start of the element...
                if (executionContext.getDeliveryConfig().isDefaultSerializationOn()) {
                    targetWriter.visitBefore(element, executionContext);
                }
            } else {
                // Just acquire ownership of the writer, but only do so if the action is not a BIND_TO
                // and default serialization is on.  BIND_TO will not use the writer, so no need to
                // acquire it for that action...
                if (getAction() != Action.BIND_TO && executionContext.getDeliveryConfig().isDefaultSerializationOn()) {
                    Writer currentWriter = executionContext.getWriter();
                    if (currentWriter instanceof WriterInterceptor.ExclusiveWriter) {
                        ((WriterInterceptor.ExclusiveWriter) currentWriter).acquire();
                    }
                }
            }
        }
    }

    @Override
    public void visitChildText(Element element, ExecutionContext executionContext) throws SmooksException {
        if(getOutputStreamResource() == null) {
            if (getAction() != Action.REPLACE && getAction() != Action.BIND_TO) {
                if (executionContext.getDeliveryConfig().isDefaultSerializationOn()) {
                    targetWriter.visitChildText(element, executionContext);
                }
            }
        }
    }

    @Override
    public void visitChildElement(Element childElement, ExecutionContext executionContext) throws SmooksException {
        if(getOutputStreamResource() == null) {
            if (getAction() != Action.REPLACE && getAction() != Action.BIND_TO) {
                if (executionContext.getDeliveryConfig().isDefaultSerializationOn()) {
                    targetWriter.visitChildElement(childElement, executionContext);
                }
            }
        }
    }

    protected abstract void applyTemplateToOutputStream(Element element, String outputStreamResourceName, ExecutionContext executionContext, Writer writer);

    protected abstract boolean beforeApplyTemplate(Element element, ExecutionContext executionContext, Writer writer);

    protected abstract boolean afterApplyTemplate(Element element, ExecutionContext executionContext, Writer writer);

    @Override
    public void visitAfter(Element element, ExecutionContext executionContext) throws SmooksException {
        String outputStreamResourceName = getOutputStreamResource();
        if (outputStreamResourceName != null) {
            if (!applyTemplateBefore()) {
                Writer writer = AbstractOutputStreamResource.getOutputWriter(outputStreamResourceName, executionContext);
                applyTemplateToOutputStream(element, outputStreamResourceName, executionContext, writer);
            }
        } else {
            if (getAction() == Action.ADD_TO) {
                if (executionContext.getDeliveryConfig().isDefaultSerializationOn()) {
                    targetWriter.writeStartElement(element, executionContext);
                }
                // apply the template...
                afterApplyTemplate(element, executionContext, executionContext.getWriter());
                // write the end of the element...
                if (executionContext.getDeliveryConfig().isDefaultSerializationOn()) {
                    targetWriter.visitAfter(element, executionContext);
                }
            } else if (getAction() == Action.INSERT_BEFORE) {
                // write the end of the element...
                if (executionContext.getDeliveryConfig().isDefaultSerializationOn()) {
                    targetWriter.visitAfter(element, executionContext);
                }
            } else if (getAction() == Action.INSERT_AFTER) {
                // write the end of the element...
                if (executionContext.getDeliveryConfig().isDefaultSerializationOn()) {
                    targetWriter.visitAfter(element, executionContext);
                }
                // apply the template...
                afterApplyTemplate(element, executionContext, executionContext.getWriter());
            } else if (getAction() == Action.REPLACE) {
                // Reset the writer and then apply the template...
                Writer writer;
                if (executionContext.getWriter() instanceof NullWriter) {
                    writer = ((NullWriter) executionContext.getWriter()).getParentWriter();
                } else {
                    writer = executionContext.getWriter();
                }

                afterApplyTemplate(element, executionContext, writer);
            } else if (getAction() == Action.BIND_TO) {
                // just apply the template...
                Writer ftlWriter = new StringWriter();
                afterApplyTemplate(element, executionContext, ftlWriter);
                try {
                    ftlWriter.flush();
                } catch (IOException e) {
                    throw new SmooksException(e.getMessage(), e);
                }
                executionContext.getBeanContext().addBean(getBindBeanId(), ftlWriter.toString(), new Fragment(element));
            }
        }
    }
    
	/**
	 * @return the bindBeanId
	 */
	public BeanId getBindBeanId() {
		return bindBeanId;
	}
}