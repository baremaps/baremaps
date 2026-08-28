<#--
Licensed under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
<#--
This template is used to list the dependencies and their licenses at the end of the LICENSE file.
-->
<#function formatLicenses licenses>
    <#assign result = ""/>
    <#list licenses as license>
        <#if result?length != 0>
            <#assign result = result + "; "/>
        </#if>
        <#assign result = result + license />
    </#list>
    <#return result>
</#function>
<#function formatProject project>
    <#return project.artifactId + "-" + project.version>
</#function>

   Dependencies produced outside this project that are included in the
   distribution of this product is subject to the following
   additional license terms.

<#if dependencyMap?size != 0>
    <#list dependencyMap as dependency>
        <#assign project = dependency.getKey()/>
        <#assign licenses = dependency.getValue()/>
       - ${formatProject(project)}, ${formatLicenses(licenses)}
    </#list>
</#if>
