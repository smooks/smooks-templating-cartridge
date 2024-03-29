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

import ognl.MemberAccess;
import ognl.Ognl;
import ognl.OgnlException;
import org.apache.xalan.extensions.XSLProcessorContext;
import org.apache.xalan.templates.AVT;
import org.apache.xalan.templates.ElemExtensionCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.ExecutionContext;

import java.util.Hashtable;
import java.util.Map;

/**
 * Javabean access <a href="http://xml.apache.org/xalan-j/">Xalan</a> XSLT extension for XSLT templating.
 * <p/>
 * Provides XSLT template population using <a href="http://www.ognl.org/">OGNL</a> expressions
 * embedded in an XSLT element or function extension.  The <a href="http://www.ognl.org/">OGNL</a> expressions
 * are targeted at the Javabean data gathered through use of the
 * <a href="http://milyn.codehaus.org/downloads">Smooks JavaBean Cartridge</a>.
 * <p/>
 * <h3 id="usage">Usage</h3>
 * <pre>
 * &lt;xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 *                 xmlns:xalan="http://xml.apache.org/xalan"
 * 		xmlns:smooks-bean="org.smooks.cartridges.templating.xslt.XalanJavabeanExtension"
 * 		extension-element-prefixes="smooks-bean"
 * 		version="1.0"&gt;
 *
 * 	&lt;xsl:template match="*"&gt;
 * 		&lt;!-- Using the XSLT extension element... --&gt;
 * 		&lt;smooks-bean:select ognl="<a href="http://www.ognl.org/">ognl-expression</a>" /&gt;
 *
 * 		&lt;!-- Using the XSLT extension function... --&gt;
 * 		&lt;xsl:value-of select="smooks-bean:select('<a href="http://www.ognl.org/">ognl-expression</a>')"/&gt;
 *
 * 	&lt;/xsl:template&gt;
 *
 * &lt;/xsl:stylesheet&gt;</pre>
 *
 * @author tfennelly
 */
public class XalanJavabeanExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(XalanJavabeanExtension.class);
    private static final MemberAccess MEMBER_ACCESS = new DefaultMemberAccess();

    /**
     * Static cache of preparsed expressions.
     */
    private static Hashtable<String, Object> expressionCache = new Hashtable<String, Object>();

    /**
     * Support OGNL based bean value injection via an XSLT extension element.
     * <p/>
     * The <a href="http://www.ognl.org/">OGNL</a> expression is expected to be specified in
     * the "ognl" attribute.
     * <p/>
     * See <a href="#usage">Usage</a>.
     *
     * @param context Processor context.
     * @param element Extension element instance.
     * @return The bean value, or null if the bean is unknown.
     * @throws OgnlException Extension element syntax is incorrectly formed, or the
     *                       <a href="http://www.ognl.org/">OGNL</a> expression is unspecified or its
     *                       syntax is incorrectly formed.
     */
    public Object select(XSLProcessorContext context, ElemExtensionCall element) throws OgnlException {
        AVT ognlAVT = element.getLiteralResultAttribute("ognl");

        if (ognlAVT == null) {
            throw new OgnlException("'ognl' expression attribute not specified.");
        }

        return select(ognlAVT.getSimpleString());
    }

    /**
     * Support OGNL based bean value injection via an XSLT extension function.
     * <p/>
     * The <a href="http://www.ognl.org/">OGNL</a> expression is expected to be specified in
     * the function call.
     * <p/>
     * See <a href="#usage">Usage</a>.
     *
     * @param ognlExpression <a href="http://www.ognl.org/">OGNL</a> expression.
     * @return The bean value, or null if the bean is unknown.
     * @throws OgnlException <a href="http://www.ognl.org/">OGNL</a> expression is unspecified or its
     *                       syntax is incorrectly formed.
     */
    public Object select(String ognlExpression) throws OgnlException {
        if (ognlExpression == null || (ognlExpression = ognlExpression.trim()).equals("")) {
            throw new OgnlException("'ognl' expression not specified, or is blank.");
        }

        ExecutionContext activeRequest = XslTemplateProcessor.executionContextThreadLocal.get();

        if (activeRequest == null) {
            String message = getClass().getName() + " can only be used within the context of a SmooksDOMFilter operation..";
            LOGGER.error(message);
            throw new IllegalStateException(message);
        }

        Map<String, Object> beans = activeRequest.getBeanContext().getBeanMap();
        Object parsedExpression = expressionCache.get(ognlExpression);

        if (parsedExpression == null) {
            try {
                // Parse and store the expression...
                parsedExpression = Ognl.parseExpression(ognlExpression);
                expressionCache.put(ognlExpression, parsedExpression);
            } catch (OgnlException e) {
                LOGGER.error("Exception parsing OGNL expression [" + ognlExpression + "].  Make sure the expression is properly constructed (http://www.ognl.org).", e);
                throw e;
            }
        }

        try {
            return Ognl.getValue(parsedExpression, Ognl.createDefaultContext(beans, MEMBER_ACCESS), beans);
        } catch (OgnlException e) {
            LOGGER.error("Unexpected exception using OGNL expression [" + ognlExpression + "] on Smooks Javabean cache.", e);
            throw e;
        }
    }
}
