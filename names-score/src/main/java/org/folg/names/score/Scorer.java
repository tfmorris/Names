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

import java.util.logging.Logger;

/**
 * Compute the similarity score between two names
 */
public class Scorer {
   private static Logger logger = Logger.getLogger("org.folg.names.score");

   private final FeaturesGenerator featuresGenerator;
   private final FeaturesScorer featuresScorer;

   public Scorer(final boolean isSurname) {
      this.featuresGenerator = new FeaturesGenerator(isSurname);
      this.featuresScorer = new FeaturesScorer(isSurname);
   }

   /**
    * Score two name pieces to see how close they are
    * @param namePiece1 normalized name piece
    * @param namePiece2 another normalized name piece
    * @return score, higher value indicates more-similar names
    */
   public double scoreNamePair(String namePiece1, String namePiece2) {
      Codes codes1 = featuresGenerator.getCodes(namePiece1);
      Codes codes2 = featuresGenerator.getCodes(namePiece2);
      Features features = new Features();
      featuresGenerator.setFeatures(namePiece1, codes1, namePiece2, codes2, features, false);
      return featuresScorer.score(features);
   }

}
