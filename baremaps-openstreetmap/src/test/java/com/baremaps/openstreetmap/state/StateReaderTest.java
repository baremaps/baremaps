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

package com.baremaps.openstreetmap.state;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

class StateReaderTest {

  @Test
  @Ignore("Disabled because of the dependency on the internet")
  void getStateFromTimestamp() {
    var reader = new StateReader();
    var state = reader.getStateFromTimestamp(LocalDateTime.now().minusDays(10));
    assertTrue(state.isPresent());
  }
}
