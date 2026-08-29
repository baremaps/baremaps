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

package com.baremaps.openstreetmap.model;

import java.time.LocalDateTime;

/**
 * The editing metadata of an element: its version, the moment it was written, the changeset it
 * belongs to and the id of the user who wrote it.
 *
 * @param version the version of the element
 * @param timestamp the moment the version was written, or null if the file does not say
 * @param changeset the changeset the version belongs to, or -1 if the file does not say
 * @param uid the id of the user who wrote the version, or -1 if the file does not say
 */
public record Info(int version, LocalDateTime timestamp, long changeset, int uid) {

  /**
   * The metadata of an element written by a file that carries none. Both formats can leave it out:
   * PBF files written with omitmeta, and XML files exported without the history.
   */
  public static final Info NO_INFO = new Info(0, null, -1, -1);
}
