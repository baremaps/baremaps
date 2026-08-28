/*
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baremaps.workflow;



import com.baremaps.tasks.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A task is a unit of work executed in a step of a workflow.
 */
@JsonSerialize
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @Type(value = CleanContextCache.class, name = "CleanContextCache"),
    @Type(value = CleanContextData.class, name = "CleanContextData"),
    // @Type(value = CreateGeonamesIndex.class, name = "CreateGeonamesIndex"),
    // @Type(value = CreateIplocIndex.class, name = "CreateIplocIndex"),
    @Type(value = DecompressFile.class, name = "DecompressFile"),
    @Type(value = DownloadUrl.class, name = "DownloadUrl"),
    @Type(value = ExecuteCommand.class, name = "ExecuteCommand"),
    @Type(value = ExecuteSql.class, name = "ExecuteSql"),
    @Type(value = ExecuteSqlScript.class, name = "ExecuteSqlScript"),
    @Type(value = ExportVectorTiles.class, name = "ExportVectorTiles"),
    @Type(value = ImportDaylightFeatures.class, name = "ImportDaylightFeatures"),
    @Type(value = ImportDaylightTranslations.class, name = "ImportDaylightTranslations"),
    @Type(value = ImportGeoPackage.class, name = "ImportGeoPackage"),
    @Type(value = ImportGeoParquet.class, name = "ImportGeoParquet"),
    @Type(value = ImportOsmOsc.class, name = "ImportOsmOsc"),
    @Type(value = ImportOsmPbf.class, name = "ImportOsmPbf"),
    @Type(value = ImportShapefile.class, name = "ImportShapefile"),
    @Type(value = LogMessage.class, name = "LogMessage"),
    @Type(value = RefreshMaterializedViews.class, name = "RefreshMaterializedViews"),
    @Type(value = UpdateOsmDatabase.class, name = "UpdateOsmDatabase"),
})
public interface Task {

  /**
   * Executes the task.
   *
   * @param context the context of the workflow
   * @throws Exception if an error occurs during the execution of the task
   */
  @SuppressWarnings("java:S112")
  void execute(WorkflowContext context) throws Exception;

}
