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

public class NormalizerTest extends TestCase {
   public NormalizerTest(String name) {
      super(name);
   }

   public void testNormalizer() throws Exception {
      Normalizer normalizer = Normalizer.getInstance();
      assertEquals("mcdonald", Utils.join(normalizer.normalize("Mc Donald", true)));
      assertEquals("olson", Utils.join(normalizer.normalize("Olsdatter", true)));
      assertEquals("alberte", Utils.join(normalizer.normalize("Alberte{1}", false)));
   }
}
