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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smooks.api.ExecutionContext;
import org.smooks.api.SmooksConfigException;
import org.smooks.api.SmooksException;
import org.smooks.api.delivery.Filter;
import org.smooks.api.delivery.FilterBypass;
import org.smooks.api.delivery.ordering.Consumer;
import org.smooks.api.io.Sink;
import org.smooks.api.io.Source;
import org.smooks.api.resource.config.ResourceConfig;
import org.smooks.api.resource.visitor.VisitAfterReport;
import org.smooks.api.resource.visitor.VisitBeforeReport;
import org.smooks.cartridges.templating.AbstractTemplateProcessor;
import org.smooks.engine.delivery.AbstractParser;
import org.smooks.engine.delivery.dom.serialize.GhostElementSerializerVisitor;
import org.smooks.engine.resource.config.ParameterAccessor;
import org.smooks.io.sink.DOMSink;
import org.smooks.io.sink.StreamSink;
import org.smooks.io.sink.WriterSink;
import org.smooks.io.source.DOMSource;
import org.smooks.io.source.ReaderSource;
import org.smooks.io.source.StreamSource;
import org.smooks.support.ClassUtils;
import org.smooks.support.DomUtils;
import org.smooks.support.StreamUtils;
import org.smooks.support.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

/**
 * XSLT template application ProcessingUnit.
 *
 * @author tfennelly
 */
@VisitBeforeReport(condition = "false")
@VisitAfterReport(summary = "Applied XSL Template.", detailTemplate = "reporting/XslTemplateProcessor_After.html")
public class XslTemplateProcessor extends AbstractTemplateProcessor implements Consumer, FilterBypass {

    static final ThreadLocal<ExecutionContext> executionContextThreadLocal = new ThreadLocal<>();

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XslTemplateProcessor.class);

    /**
     * XSL as a String.
     */
    private String xslString;
    /**
     * XSL template to be applied to the visited element.
     */
    private Templates xslTemplate;
    /**
     * Is this processor processing an XSLT <a href="#templatelets">Templatelet</a>.
     */
    private boolean isTemplatelet;
    /**
     * This Visitor implements the {@link FilterBypass} interface.  This config param allows
     * the user to enable/disable the bypass.
     */
    @Inject
    private Boolean enableFilterBypass = true;

    /**
     * Is the Smooks configuration, for which this visitor is a part, targeted at an XML message stream.
     * We know if it is by the XML reader configured (or not configured).
     */
    private volatile Boolean isXMLTargetedConfiguration;

    /**
     * Is the template application synchronized or not.
     * <p/>
     * Xalan v2.7.0 has/had a threading issue - kick-on effect being that template application
     * must be synchronized.
     */
    private final boolean isSynchronized = Boolean.getBoolean(XslContentHandlerFactory.ORG_MILYN_TEMPLATING_XSLT_SYNCHRONIZED);
    private final DomErrorHandler logErrorHandler = new DomErrorHandler();


    @Override
    protected void loadTemplate(ResourceConfig resourceConfig) throws IOException, TransformerConfigurationException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        javax.xml.transform.stream.StreamSource xslStreamSource;
        boolean isInlineXSL = resourceConfig.isInline();
        byte[] xslBytes = resourceConfig.getBytes();

        xslString = new String(xslBytes, getEncoding().name());

        // If it's not a full XSL template, we need to make it so by wrapping it...
        isTemplatelet = isTemplatelet(isInlineXSL, new String(xslBytes));
        if (isTemplatelet) {
            String templateletWrapper = new String(StreamUtils.readStream(ClassUtils.getResourceAsStream("doc-files/templatelet.xsl", getClass())));
            String templatelet = new String(xslBytes);

            templateletWrapper = templateletWrapper.replace("@@@templatelet@@@", templatelet);
            xslBytes = templateletWrapper.getBytes();
            xslString = new String(xslBytes, getEncoding().name());
        }

        boolean failOnWarning = resourceConfig.getParameterValue("failOnWarning", Boolean.class, true);

        xslStreamSource = new javax.xml.transform.stream.StreamSource(new StringReader(xslString));
        transformerFactory.setErrorListener(new XslErrorListener(failOnWarning));
        xslTemplate = transformerFactory.newTemplates(xslStreamSource);
    }

    private boolean isTemplatelet(boolean inlineXSL, String templateCode) {
        try {
            Document xslDoc = XmlUtils.parseStream(new StringReader(templateCode), logErrorHandler);
            Element rootElement = xslDoc.getDocumentElement();
            String rootElementNS = rootElement.getNamespaceURI();

            return (inlineXSL && !(rootElementNS != null && rootElementNS.equals("http://www.w3.org/1999/XSL/Transform") && DomUtils.getName(rootElement).equals("stylesheet")));
        } catch (ParserConfigurationException | IOException e) {
            throw new SmooksConfigException("Unable to parse XSL Document (Stylesheet/Templatelet).", e);
        } catch (SAXException e) {
            return inlineXSL;
        }
    }

    @Override
    public boolean consumes(Object object) {
        return xslString.contains(object.toString());
    }

    @Override
    protected void applyTemplate(Element element, ExecutionContext executionContext, Writer writer) {
        Document ownerDoc = element.getOwnerDocument();
        Element ghostElement = GhostElementSerializerVisitor.createElement(ownerDoc);

        try {
            if (isSynchronized) {
                synchronized (xslTemplate) {
                    performTransform(element, ghostElement, ownerDoc, executionContext);
                }
            } else {
                performTransform(element, ghostElement, ownerDoc, executionContext);
            }
        } catch (TransformerException e) {
            throw new SmooksException("Error applying XSLT to node [" + executionContext.getDocumentSource() + ":" + DomUtils.getXPath(element) + "]", e);
        }

        try {
            writer.write(XmlUtils.serialize(ghostElement.getChildNodes(), Boolean.parseBoolean(ParameterAccessor.getParameterValue(Filter.CLOSE_EMPTY_ELEMENTS, String.class, "false", executionContext.getContentDeliveryRuntime().getContentDeliveryConfig()))));
        } catch (IOException e) {
            throw new SmooksException(e.getMessage(), e);
        }
    }

    private void performTransform(Element element, Element transRes, Document ownerDoc, ExecutionContext executionContext) throws TransformerException {
        Transformer transformer = xslTemplate.newTransformer();

        try {
            executionContextThreadLocal.set(executionContext);
            if (element == ownerDoc.getDocumentElement()) {
                transformer.transform(new javax.xml.transform.dom.DOMSource(ownerDoc), new DOMResult(transRes));
            } else {
                transformer.transform(new javax.xml.transform.dom.DOMSource(element), new DOMResult(transRes));
            }
        } finally {
            executionContextThreadLocal.remove();
        }
    }

    @Override
    public boolean bypass(ExecutionContext executionContext, Source source, Sink sink) throws SmooksException {
        if (!enableFilterBypass) {
            return false;
        }
        if (!isXMLTargetedConfiguration(executionContext)) {
            return false;
        }
        javax.xml.transform.Source xmlSource = null;
        Result outputTarget = null;
        if (source instanceof StreamSource) {
            xmlSource = new javax.xml.transform.stream.StreamSource(((StreamSource<?>) source).getInputStream());
        } else if (source instanceof ReaderSource) {
            xmlSource = new javax.xml.transform.stream.StreamSource(((ReaderSource<?>) source).getReader());
        } else if (source instanceof DOMSource) {
            xmlSource = new javax.xml.transform.dom.DOMSource(((DOMSource) source).getNode());
        }
        if (sink instanceof StreamSink) {
            outputTarget = new StreamResult(((StreamSink<?>) sink).getOutputStream());
        } else if (sink instanceof WriterSink) {
            outputTarget = new StreamResult(((WriterSink<?>) sink).getWriter());
        } else if (sink instanceof DOMSink) {
            outputTarget = new DOMResult(((DOMSink) sink).getNode());
        }
        if (xmlSource != null && outputTarget != null) {
            try {
                Transformer transformer = xslTemplate.newTransformer();
                transformer.transform(xmlSource, outputTarget);

                return true;
            } catch (TransformerException e) {
                throw new SmooksException("Error applying XSLT.", e);
            }
        }

        return false;
    }

    private boolean isXMLTargetedConfiguration(ExecutionContext executionContext) {
        if (isXMLTargetedConfiguration == null) {
            synchronized (this) {
                if (isXMLTargetedConfiguration == null) {
                    ResourceConfig readerConfiguration = AbstractParser.getSAXParserConfiguration(executionContext.getContentDeliveryRuntime().getContentDeliveryConfig());
                    if (readerConfiguration != null) {
                        // We have an reader config, if the class is not configured, we assume
                        // the expected Source to be XML...
                        isXMLTargetedConfiguration = (readerConfiguration.getResource() == null);
                    } else {
                        // If no reader config is present at all, we assume the expected Source is XML...
                        isXMLTargetedConfiguration = true;
                    }
                }
            }
        }

        return isXMLTargetedConfiguration;
    }

    private static class XslErrorListener implements ErrorListener {
        private final boolean failOnWarning;

        public XslErrorListener(boolean failOnWarning) {
            this.failOnWarning = failOnWarning;
        }

        public void warning(TransformerException exception) throws TransformerException {
            if (failOnWarning) {
                throw exception;
            } else {
                LOGGER.debug("XSL Warning.", exception);
            }
        }

        public void error(TransformerException exception) throws TransformerException {
            throw exception;
        }

        public void fatalError(TransformerException exception) throws TransformerException {
            throw exception;
        }
    }

    /**
     * Simple ErrorHandler that only reports errors, fatals, and warnings
     * at a debug log level.
     * <p/>
     *
     * @author <a href="mailto:daniel.bevenius@gmail.com">Daniel Bevenius</a>
     */
    private static class DomErrorHandler implements ErrorHandler {
        public void error(final SAXParseException exception) throws SAXException {
            LOGGER.debug("SaxParseException error was reported : ", exception);
        }

        public void fatalError(final SAXParseException exception) throws SAXException {
            LOGGER.debug("SaxParseException fatal error was reported : ", exception);
        }

        public void warning(final SAXParseException exception) throws SAXException {
            LOGGER.debug("SaxParseException warning error was reported : ", exception);
        }
    }
}