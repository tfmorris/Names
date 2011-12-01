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

import java.io.*;

/**
 * Represents a set of possible edits and their associated costs.
 */
public class WeightedEdits {
   public static final int MAX_EDIT_COST = 100;
   private static final double COST_MULTIPLIER = 8;

   private final int[][] edits;

   public WeightedEdits() {
      // init edits to some default values
      edits = new int[WeightedEditDistanceTokenizer.NUM_TOKENS][WeightedEditDistanceTokenizer.NUM_TOKENS];
      this.reset();
   }

   /**
    * Reset all costs to zero.
    */
   public void reset() {
      for (int x = 0; x < edits.length; x++) {
         for (int y = 0; y < edits[x].length; y++) {
            edits[x][y] = 0;
         }
      }
   }

   /**
    * Load a weighted edits object from a file
    * @param file weighted edits file
    * @throws java.io.IOException if file is unreadable
    */
   public void load(File file) throws IOException {
      load(new FileReader(file));
   }

   public void load(InputStream is) throws IOException {
      load(new InputStreamReader(is, "UTF8"));
   }

   public void load(Reader r) throws IOException {
      BufferedReader reader = new BufferedReader(r);
      try {
         while (reader.ready()) {
            String line = reader.readLine();
            if (line == null) {
               continue;
            }
            String[] fields = line.split(",");
            String[] tokens = fields[0].split("\\|");
            int cost = Integer.parseInt(fields[1]);
            edits[tokens.length < 1 ? WeightedEditDistanceTokenizer.EMPTY_TOKEN : WeightedEditDistanceTokenizer.getTokenId(tokens[0])]
                 [tokens.length < 2 ? WeightedEditDistanceTokenizer.EMPTY_TOKEN : WeightedEditDistanceTokenizer.getTokenId(tokens[1])] = cost;
         }
      }
      finally {
         reader.close();
      }
   }

   /**
    * Save a weighted edits object to a file
    * @param f file to save into
    * @throws java.io.IOException if file cannot be written
    */
   public void save(File f) throws IOException {
      PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(f)));
      for (int i = 0; i < edits.length; i++) {
         for (int j = i; j < edits[i].length; j++) {
            writer.println(WeightedEditDistanceTokenizer.getToken(i)+"|"+ WeightedEditDistanceTokenizer.getToken(j)+","+edits[i][j]);
         }
      }
      writer.close();
   }

   /**
    * Get the cost of a src/tar edit
    * @param sourceToken source position
    * @param targetToken target position
    * @return cost
    */
   public int getCost(int sourceToken, int targetToken) {
      if (targetToken < sourceToken) {
         int swap = sourceToken;
         sourceToken = targetToken;
         targetToken = swap;
      }
      return edits[sourceToken][targetToken];
   }

   /**
    * Add count instances to the src/tar edit
    * @param sourceToken source position
    * @param targetToken target position
    * @param count increment count
    */
   public void addCount(int sourceToken, int targetToken, int count) {
      if (targetToken < sourceToken) {
         int swap = sourceToken;
         sourceToken = targetToken;
         targetToken = swap;
      }
      edits[sourceToken][targetToken] += count;
   }

   /**
    * Calculate weighted costs from counts.  Call this after all addCount calls, and before any getCost calls.
    * @param smooth boolean indicating whether or not to smooth cost score by adding 1 to all costs
    */
   public void calcCosts(boolean smooth) {
      // first go through and sum the counts
      int sum = 0;
      for (int i = 0; i < edits.length; i++) {
         for (int j = i; j < edits[i].length; j++) {
            if (smooth) {
               edits[i][j]++;
            }
            sum += edits[i][j];
         }
      }

      // now calculate the costs as the -log(count/sum), but clip it so it doesn't go above a max edit cost
      for (int i = 0; i < edits.length; i++) {
         for (int j = i; j < edits[i].length; j++) {
            int cost;
            if (edits[i][j] < 1) {
               cost = MAX_EDIT_COST;
            }
            else {
               cost = (int)Math.min(MAX_EDIT_COST, -Math.log(edits[i][j] * 1.0 / sum) * COST_MULTIPLIER);
            }
            edits[i][j] = cost;
         }
      }
   }

   /**
    * Calculate the absolute difference between this WeightedEdits and another.
    * Note: Edits in the passed-in WeightedEdits that are not also found in this WeightedEdits are not included in
    * the calculation.
    * @param we WeightedEdits object
    * @return difference
    */
   public int difference(WeightedEdits we) {
      int diff = 0;

      for (int i = 0; i < we.edits.length; i++) {
         for (int j = i; j < we.edits[i].length; j++) {
            diff += Math.abs(edits[i][j] - we.edits[i][j]);
         }
      }

      return diff;
   }
}
