/*
 * Copyright 2011 Foundation for On-Line Genealogy, Inc.
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
 */

package org.folg.names.score;

import junit.framework.TestCase;

public class SimilarNameGeneratorTest extends TestCase {
   public SimilarNameGeneratorTest(String name) {
      super(name);
   }

   public void testSimilarNameGenerator() throws Exception {
      SimilarNameGenerator sng = new SimilarNameGenerator(true, true);
      assertEquals("quaas quash quasie quessy quatsy cass kass quatsie quijas casse kasse quish quack catts quates",
              Utils.join(" ", sng.generateSimilarNames("quass")));
   }
}
