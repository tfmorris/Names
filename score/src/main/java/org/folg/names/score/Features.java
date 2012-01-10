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

/**
 * Features used in scoring a name pair
 */
public class Features {
   public double weightedEditDistance; // weighted edit distance

   public double nysiis; // nysiis
   public double soundex; // soundex
   public double refinedSoundex; // refined soundex
   public double dmSoundex; // daitch-mokotoff soundex
   public double levenstein; // levenstein

   public Features() {
      weightedEditDistance = nysiis = soundex = refinedSoundex = dmSoundex = levenstein =
              0.0;
   }
}
