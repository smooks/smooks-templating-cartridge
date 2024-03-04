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
package org.smooks.cartridges.templating.soapshipping;

import org.junit.Test;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.cartridges.templating.util.CharUtils;
import org.smooks.engine.profile.DefaultProfileSet;
import org.smooks.support.SmooksUtil;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public abstract class ShippingIntegTestBase {

    @Test
    public void testTransform() throws SAXException, IOException {
        Smooks smooks = new Smooks();

        // Configure Smooks
        SmooksUtil.registerProfileSet(new DefaultProfileSet("shipping-request"), smooks);
        SmooksUtil.registerProfileSet(new DefaultProfileSet("shipping-response"), smooks);
        smooks.addResourceConfigs("trans-request.xml", getClass().getResourceAsStream("trans-request.xml"));
        smooks.addResourceConfigs("trans-response.xml", getClass().getResourceAsStream("trans-response.xml"));
                
        InputStream requestStream = getClass().getResourceAsStream("/org/smooks/cartridges/templating/soapshipping/request.xml");
        ExecutionContext context = smooks.createExecutionContext("shipping-request");
        String requestResult = SmooksUtil.filterAndSerialize(context, requestStream, smooks);
		CharUtils.assertEquals("Template test failed.", "/org/smooks/cartridges/templating/soapshipping/request.xml.tran.expected", requestResult);

        InputStream responseStream = getClass().getResourceAsStream("/org/smooks/cartridges/templating/soapshipping/response.xml");
        context = smooks.createExecutionContext("shipping-response");
        String responseResult = SmooksUtil.filterAndSerialize(context, responseStream, smooks);
		CharUtils.assertEquals("Template test failed.", "/org/smooks/cartridges/templating/soapshipping/response.xml.tran.expected", responseResult);
    }
}
