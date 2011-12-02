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

public class ScorerTest extends TestCase {
   public ScorerTest(String name) {
      super(name);
   }

   public void testScorer() throws Exception {
      Scorer scorer = Scorer.getGivennameInstance();
      assertEquals(1527, (int)Math.floor(scorer.scoreNamePair("dallan","allan")*1000));
      assertEquals(-1010, (int)Math.floor(scorer.scoreNamePair("ann","roseanne")*1000));

      scorer = Scorer.getSurnameInstance();
      assertEquals(937, (int)Math.floor(scorer.scoreNamePair("quass","quast")*1000));
   }
}
