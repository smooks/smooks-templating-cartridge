<!--
  ========================LICENSE_START=================================
  smooks-templating-cartridge
  %%
  Copyright (C) 2020 Smooks
  %%
  Licensed under the terms of the Apache License Version 2.0, or
  the GNU Lesser General Public License version 3.0 or later.
  
  SPDX-License-Identifier: Apache-2.0 OR LGPL-3.0-or-later
  
  ======================================================================
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  
  ======================================================================
  
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.
  
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
  
  You should have received a copy of the GNU Lesser General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  =========================LICENSE_END==================================
  -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:smooks-bean="xalan://org.smooks.cartridges.templating.xslt.XalanJavabeanExtension"
				extension-element-prefixes="smooks-bean"
				version="1.0">

	<!-- Root template  -->
	<xsl:template match="*">
		<trackingNumbers>
			<xsl:call-template name="outputTrackingNumber"/>
    	</trackingNumbers>
	</xsl:template>

	<xsl:variable name="trackingNumberCount" select="smooks-bean:select('history.trackingNumbers.length')"/>

	<!-- Recursively called template for outputting the trackingNumber elements -->
	<xsl:template name="outputTrackingNumber">
		<xsl:param name="i" select="0"/>

		<xsl:if test="$i &lt; $trackingNumberCount">
			<trackingNumber>
				<shipperID>
					<xsl:variable name="ognl" select="concat('history.trackingNumbers[', $i, '].shipperID')" />
					<xsl:value-of select="smooks-bean:select($ognl)" />
				</shipperID>
				<shipmentNumber>
					<xsl:variable name="ognl" select="concat('history.trackingNumbers[', $i, '].shipmentNumber')" />
					<xsl:value-of select="smooks-bean:select($ognl)" />
				</shipmentNumber>
			</trackingNumber>

			<xsl:call-template name="outputTrackingNumber">
				<xsl:with-param name="i" select="$i + 1"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>		
