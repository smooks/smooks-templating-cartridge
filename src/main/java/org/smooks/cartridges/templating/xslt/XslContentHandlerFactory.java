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

import org.smooks.SmooksException;
import org.smooks.cdr.SmooksConfigurationException;
import org.smooks.cdr.SmooksResourceConfiguration;
import org.smooks.injector.Scope;
import org.smooks.registry.lookup.LifecycleManagerLookup;
import org.smooks.container.ApplicationContext;
import org.smooks.delivery.ContentHandler;
import org.smooks.delivery.ContentHandlerFactory;
import org.smooks.javabean.context.BeanContext;
import org.smooks.lifecycle.phase.PostConstructLifecyclePhase;

import javax.inject.Inject;


/**
 * XSL {@link org.smooks.delivery.dom.DOMElementVisitor} Creator class.
 * <p/>
 * Creates {@link org.smooks.delivery.dom.DOMElementVisitor} instances for performing node/element level
 * <a href="http://www.w3.org/Style/XSL/">XSL</a> templating (aka XSLT).
 * <p/>
 * Template application can be done in a synchronized or unsynchronized fashion by setting
 * the system property "org.smooks.cartridges.templating.xslt.synchronized".  According to the spec,
 * this should not be necessary.  However, Xalan 2.7.0 (for one) has a bug which results in
 * unsynchronized template application causing invalid transforms.
 * <p/>
 * <h2>Targeting "xsl" Templates</h2>
 * The following is the basic configuration specification for XSL resources:
 * <pre>
 * &lt;resource-config selector="<i>target-element</i>"&gt;
 *     &lt;resource&gt;<b>XSL Resource - Inline or {@link org.smooks.resource.URIResourceLocator URI}</b>&lt;/resource&gt;
 *
 *     &lt;!-- (Optional) The action to be applied on the template content. Should the content
 *          generated by the template:
 *          1. replace ("replace") the target element, or
 *          2. be added to ("ADD_TO") the target element, or
 *          3. be inserted before ("INSERT_BEFORE") the target element, or
 *          4. be inserted after ("INSERT_AFTER") the target element.
 *          5. be bound to ("BIND_TO") a {@link BeanContext} variable named by the "bindId" param.
 *          Default "replace".--&gt;
 *     &lt;param name="<b>action</b>"&gt;<i>REPLACE/ADD_TO/INSERT_BEFORE/INSERT_AFTER/BIND_TO</i>&lt;/param&gt;
 *
 *     &lt;!-- (Optional) Is this XSL template resource a complete XSL template, or just a <a href="#templatelets">"Template<u>let</u></a>".
 *          Only relevant for inlined XSL resources.  URI based resource are always assumed to NOT be templatelets.
 *          Default "false" (for inline resources).--&gt;
 *     &lt;param name="<b>is-xslt-templatelet</b>"&gt;<i>true/false</i>&lt;/param&gt;
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
 *     &lt;!-- (Optional) Fail on XSL Transformer Warning.
 *          Default "true".--&gt;
 *     &lt;param name="<b>failOnWarning</b>"&gt;false&lt;/param&gt;</b> &lt;!-- Default "true" --&gt;
 *
 * &lt;/resource-config&gt;
 * </pre>
 * <p/>
 * <i><u>Example - URI based XSLT spec</u></i>:
 * <pre>
 * &lt;resource-config selector="<i>target-element</i>"&gt;
 *     &lt;!-- 1. See {@link org.smooks.resource.URIResourceLocator} --&gt;
 *     &lt;resource&gt;/com/acme/order-transform.xsl&lt;/resource&gt;
 * &lt;/resource-config&gt;
 * </pre>
 * <p/>
 * <i><u>Example - Inlined XSLT spec</u></i>:
 * <pre>
 * &lt;resource-config selector="<i>target-element</i>"&gt;
 *     &lt;!-- 1. Note how we have to specify the resource type when it's inlined. --&gt;
 *     &lt;!-- 2. Note how the inlined XSLT is wrapped as an XML Comment. CDATA Section wrapping also works. --&gt;
 *     &lt;!-- 3. Note if the inlined XSLT is a <a href="#templatelets">templatelet</a>, is-xslt-templatelet=true must be specified. --&gt;
 *     &lt;resource <b color="red">type="xsl"</b>&gt;
 *         &lt;!--
 *            <i>Inline XSLT....</i>
 *         --&gt;
 *     &lt;/resource&gt;
 *     <b color="red">&lt;param name="is-xslt-templatelet"&gt;true&lt;/param&gt;</b>
 * &lt;/resource-config&gt;
 * </pre>
 * <p/>
 * <h3 id="templatelets">Templatelets</h3>
 * Templatelets are a convenient way of specifying an XSL Stylesheet.  When using "Templatelets", you simply specify the
 * body of an XSL template. This creator then wraps that body to make a complete XSL Stylesheet with a single template matched to the
 * element targeted by the Smooks resource configuration in question.  This feature only applies
 * to inlined XSL resources and in this case, it's <u>OFF</u> by default.  To use this feature,
 * you must specify the "is-xslt-templatelet" parameter with a value of "true".
 * <p/>
 * This feature will not work in all situations since you'll often need to specify a full stylesheet in order to
 * specify namespaces etc.  It's just here for convenience.
 * <p/>
 * <a href="doc-files/templatelet.xsl" type="text/plain">See the template used to wrap the templatelet</a>.
 * <p/>
 * <h3>JavaBean Support</h3>
 * Support for injection of JavaBean values populated by the
 * <a href="http://milyn.codehaus.org/downloads">Smooks JavaBean Cartridge</a> is supported through the
 * <a href="http://xml.apache.org/xalan-j/">Xalan</a> extension {@link org.smooks.cartridges.templating.xslt.XalanJavabeanExtension}.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class XslContentHandlerFactory implements ContentHandlerFactory {

    /**
     * Parameter name for templating feature.
     */
    public static final String IS_XSLT_TEMPLATELET = "is-xslt-templatelet";
    /**
     * Synchonized template application system property key.
     */
    public static final String ORG_MILYN_TEMPLATING_XSLT_SYNCHRONIZED = "org.smooks.cartridges.templating.xslt.synchronized";

    @Inject
    private ApplicationContext applicationContext;

    /**
     * Create an XSL based ContentHandler instance ie from an XSL byte streamResult.
     *
     * @param resourceConfig The SmooksResourceConfiguration for the XSL {@link org.smooks.delivery.ContentHandler}
     *                       to be created.
     * @return XSL {@link org.smooks.delivery.ContentHandler} instance.
     * @see org.smooks.delivery.JavaContentHandlerFactory
     */
    public synchronized ContentHandler create(SmooksResourceConfiguration resourceConfig) throws SmooksConfigurationException {
        final XslTemplateProcessor xslTemplateProcessor = new XslTemplateProcessor();
        try {
            applicationContext.getRegistry().lookup(new LifecycleManagerLookup()).applyPhase(xslTemplateProcessor, new PostConstructLifecyclePhase(new Scope(applicationContext.getRegistry(), resourceConfig, xslTemplateProcessor)));
            return xslTemplateProcessor;
        } catch(SmooksConfigurationException e) {
            throw e;
        } catch (Exception e) {
            InstantiationException instanceException = new InstantiationException("XSL ProcessingUnit resource [" + resourceConfig.getResource() + "] not loadable.");
            instanceException.initCause(e);
            throw new SmooksException(instanceException.getMessage(), instanceException);
        }
    }

    @Override
    public String getType() {
        return "xsl";
    }

}
