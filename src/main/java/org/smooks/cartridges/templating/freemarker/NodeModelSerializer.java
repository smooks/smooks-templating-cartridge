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

import freemarker.core.Environment;
import freemarker.ext.dom.NodeModel;
import freemarker.template.*;
import org.smooks.xml.XmlUtil;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.Map;

/**
 * Serialize a NodeModel variable to the template stream.
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public class NodeModelSerializer implements TemplateDirectiveModel {

    public void execute(Environment environment, Map map, TemplateModel[] templateModels, TemplateDirectiveBody templateDirectiveBody) throws TemplateException, IOException {
        TemplateModel nodeModelVariable = (TemplateModel) map.get("nodeModel");
        if (nodeModelVariable == null) {
            throw new TemplateModelException("'nodeModel' variable not defined on 'serializer' directive.");
        } else if (nodeModelVariable instanceof NodeModel) {
            Element element = (Element) ((NodeModel) nodeModelVariable).getWrappedObject();
            TemplateModel format = (TemplateModel) map.get("format");

            if (format instanceof TemplateBooleanModel) {
                XmlUtil.serialize(element, ((TemplateBooleanModel) format).getAsBoolean(), environment.getOut(), false);
            } else {
                XmlUtil.serialize(element, false, environment.getOut(), false);
            }
        } else {
            throw new TemplateModelException("Invalid NodeModel variable reference.  Not a NodeModel.");
        }
    }
}
