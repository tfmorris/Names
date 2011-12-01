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

package org.folg.names.search;

import junit.framework.TestCase;

import java.util.TreeSet;

public class SearcherTest extends TestCase {
   public SearcherTest(String name) {
      super(name);
   }

   public void testSearcher() throws Exception {
      Searcher searcher = Searcher.getGivennameInstance();
      assertEquals("", Utils.join(searcher.getAdditionalIndexTokens("dallan")));
      assertEquals("D450", Utils.join(searcher.getAdditionalIndexTokens("dalan")));
      assertEquals("D450 dalana daleen dalen dalena dalene dalin dalla dallas dallen dallin " +
                  "dallon dalma dalon dalson dalvin dalyn dalynn daylan daylene daylon delain " +
                  "delaina delaine delan delana delane delaney delania delanie delano delany " +
                  "delaon delaun delayne delean delena delene deleno deleon deliana delin delino " +
                  "delion dellene dellon delon delona delone delyn delynn dilan dillan dillen " +
                  "dillian dillin dillion dillon dolan dolen dolena doolin dulane dulaney dulany " +
                  "dulin dylan dyllan dylon talan tallon",
                 Utils.join(new TreeSet<String>(searcher.getAdditionalSearchTokens("dallan"))));
      searcher = Searcher.getSurnameInstance();
      assertEquals("", Utils.join(searcher.getAdditionalIndexTokens("quass")));
      assertEquals("Q200", Utils.join(searcher.getAdditionalIndexTokens("quas")));
      assertEquals("Q200 cass casse catts kass kasse quaas quack quash quasie quast " +
                 "quates quatsie quatsy quessy quijas quish",
                 Utils.join(new TreeSet<String>(searcher.getAdditionalSearchTokens("quass"))));
   }
}
