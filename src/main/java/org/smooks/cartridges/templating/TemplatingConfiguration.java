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

import org.smooks.assertion.AssertArgument;

/**
 * Templating Configuration.
 * <p/>
 * Allow programmatic configuration of a {@link org.smooks.cartridges.templating.AbstractTemplateProcessor}
 * implementation.
 *
 * @author <a href="mailto:tom.fennelly@jboss.com">tom.fennelly@jboss.com</a>
 */
public class TemplatingConfiguration {

    private String template;
    private boolean applyBefore = false;

    /**
     * Public constructor.
     *
     * @param template The template.  This can be a URI referencing the template resource,
     *                 or can be an inlined template.
     */
    public TemplatingConfiguration(String template) {
        AssertArgument.isNotNullAndNotEmpty(template, "template");
        this.template = template;
    }

    /**
     * Get the template.
     *
     * @return The template.  This can be a URI referencing the template resource,
     * or can be an inlined template.
     */
    protected String getTemplate() {
        return template;
    }

    /**
     * Should the template be applied at the start of the fragment and before processing
     * any of the fragment's child content.
     *
     * @return True if the template is to be applied at the start of the fragment, otherwise false.
     */
    protected boolean applyBefore() {
        return applyBefore;
    }

    /**
     * Set whether or not the template should be applied at the start of the fragment and before processing
     * any of the fragment's child content.
     *
     * @param applyBefore True if the template is to be applied at the start of the fragment, otherwise false.
     * @return This instance.
     */
    public TemplatingConfiguration setApplyBefore(boolean applyBefore) {
        this.applyBefore = applyBefore;
        return this;
    }
}
