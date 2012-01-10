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

import com.wcohen.ss.*;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.RefinedSoundex;
import org.apache.commons.codec.language.Soundex;

import java.util.logging.Logger;

/**
 * Generate features used in socring a name
 */
public class FeaturesGenerator {
   private static Logger logger = Logger.getLogger("org.folg.names.score");

   private final boolean isSurname;

   private final WeightedEditDistance wed;
   private final Soundex soundex = new Soundex();
   private final RefinedSoundex refinedSoundex = new RefinedSoundex();
   private final DMSoundex dmSoundex = new DMSoundex();
   private final Nysiis nysiis = new Nysiis();
   private final Levenstein levenstein = new Levenstein();

   /**
    * Create a new FeaturesGenerator
    * @param isSurname - true if generating features for surnames
    */
   public FeaturesGenerator(final boolean isSurname) {
      this.isSurname = isSurname;
      wed = new WeightedEditDistance(isSurname);
   }

   private double codeScorer(String code1, String code2) {
      return code1.equals(code2) ? 1.0 : 0.0;
   }

   public Codes getCodes(String name) {
      Codes codes = new Codes();
      codes.wedTokens = wed.tokenize(name);
      if (isSurname) {
         codes.nysCode = nysiis.encode(name);
      }
      codes.refSdxCode = refinedSoundex.encode(name);
      codes.dmSdxCode = dmSoundex.encode(name);
      codes.sdxCode = soundex.encode(name);
      return codes;
   }

   /**
    * Set the features for a name-bucket pair
    * @param name1 head-name to test
    * @param codes1 codes for head-name to test
    * @param name2 name to test
    * @param codes2 codes for name to test
    * @param features Features struct to set
    */
   public void setFeatures(String name1, Codes codes1, String name2, Codes codes2, Features features) {
      try {
         features.weightedEditDistance = wed.getScore(codes1.wedTokens, codes2.wedTokens);
         // See comment in FST.java to understand why we do this
         double reverseDistance = wed.getScore(codes2.wedTokens, codes1.wedTokens);
         if (features.weightedEditDistance > reverseDistance) {
            features.weightedEditDistance = reverseDistance;
         }
         if (isSurname) {
            features.nysiis = codeScorer(codes1.nysCode, codes2.nysCode);
         }
         features.soundex = codeScorer(codes1.sdxCode, codes2.sdxCode);
         features.refinedSoundex = codeScorer(codes1.refSdxCode, codes2.refSdxCode);
         features.dmSoundex = codeScorer(codes1.dmSdxCode, codes2.dmSdxCode);
         features.levenstein = levenstein.score(name1, name2);
      }
      catch (IllegalArgumentException e) {
         logger.severe("Illegal argument for pair: "+name1+","+name2+" "+e);
      }
   }

   /**
    * Easy-to-call but slow
    * @param name1 head-name
    * @param name2 name
    * @param features features to set
    * @throws EncoderException should never happen
    */
   public void setFeatures(String name1, String name2, Features features) throws EncoderException {
      Codes codes1 = getCodes(name1);
      Codes codes2 = getCodes(name2);
      setFeatures(name1, codes1, name2, codes2, features);
   }
}
