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

import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.cdr.annotation.AppContext;
import org.smooks.cdr.annotation.Configurator;
import org.smooks.container.ApplicationContext;
import org.smooks.delivery.ContentHandler;
import org.smooks.delivery.ContentHandlerFactory;
import org.smooks.delivery.annotation.Resource;
import org.smooks.javabean.context.BeanContext;

/**
 * <a href="http://freemarker.org/">FreeMarker</a> templating {@link org.smooks.delivery.Visitor} Creator class.
 * <p/>
 * This templating solution relies on the <a href="http://milyn.codehaus.org/downloads">Smooks JavaBean Cartridge</a>
 * to perform the JavaBean population that's required by <a href="http://freemarker.org/">FreeMarker</a> (for the data model).
 *
 * <h2>Targeting "ftl" Templates</h2>
 * The following is the basic configuration specification for FreeMarker resources:
 * <pre>
 * &lt;resource-config selector="<i>target-element</i>"&gt;
 *     &lt;resource&gt;<b>FreeMarker Resource - Inline or {@link org.smooks.resource.URIResourceLocator URI}</b>&lt;/resource&gt;
 *
 *     &lt;!-- (Optional) The action to be applied on the template content. Should the content
 *          generated by the template:
 *          1. replace ("replace") the target element, or
 *          2. be added to ("addto") the target element, or
 *          3. be inserted before ("insertbefore") the target element, or
 *          4. be inserted after ("insertafter") the target element.
 *          5. be bound to ("bindto") a {@link BeanContext} variable named by the "bindId" param.
 *          Default "replace".--&gt;
 *     &lt;param name="<b>action</b>"&gt;<i>replace/addto/insertbefore/insertafter</i>&lt;/param&gt;
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
 *     &lt;!-- (Optional) bindId when "action" is "bindto".
 *     &lt;param name="<b>bindId</b>"&gt;<i>xxxx</i>&lt;/param&gt;
 *
 * &lt;/resource-config&gt;
 * </pre>
 * <p/>
 * <i><u>Example - URI based FreeMarker spec</u></i>:
 * <pre>
 * &lt;resource-config selector="<i>target-element</i>"&gt;
 *     &lt;!-- 1. See {@link org.smooks.resource.URIResourceLocator} --&gt;
 *     &lt;resource&gt;/com/acme/order-transform.ftl&lt;/resource&gt;
 * &lt;/resource-config&gt;
 * </pre>
 * <p/>
 * <i><u>Example - Inlined FreeMarker spec</u></i>:
 * <pre>
 * &lt;resource-config selector="<i>target-element</i>"&gt;
 *     &lt;!-- 1. Note how we have to specify the resource type when it's inlined. --&gt;
 *     &lt;!-- 2. Note how the inlined FreeMarker template is wrapped as an XML Comment. CDATA Section wrapping also works. --&gt;
 *     &lt;resource <b color="red">type="ftl"</b>&gt;
 *         &lt;!--
 *            <i>Inline FreeMarker Template....</i>
 *         --&gt;
 *     &lt;/resource&gt;
 * &lt;/resource-config&gt;
 * </pre>
 *
 * @author tfennelly
 */
@Resource(type="ftl")
public class FreeMarkerContentHandlerFactory implements ContentHandlerFactory {

	@AppContext
	private ApplicationContext applicationContext;

    /**
	 * Create a FreeMarker based ContentHandler.
     * @param resourceConfig The SmooksResourceConfiguration for the FreeMarker.
     * @return The FreeMarker {@link org.smooks.delivery.ContentHandler} instance.
	 */
	public synchronized ContentHandler create(SmooksResourceConfiguration resourceConfig) throws SmooksConfigurationException, InstantiationException {
        try {
            return Configurator.configure(new FreeMarkerTemplateProcessor(), resourceConfig, applicationContext);
        } catch (SmooksConfigurationException e) {
            throw e;
        } catch (Exception e) {
			InstantiationException instanceException = new InstantiationException("FreeMarker resource [" + resourceConfig.getResource() + "] not loadable.  FreeMarker resource invalid.");
			instanceException.initCause(e);
			throw instanceException;
		}
	}

}
