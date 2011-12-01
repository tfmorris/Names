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

import com.sun.speech.freetts.lexicon.LetterToSound;
import com.sun.speech.freetts.lexicon.LetterToSoundImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Tokenize a name in preparation for running the weighted edit distance algorithm
 */
public class WeightedEditDistanceTokenizer {
   private static Logger logger = Logger.getLogger("org.folg.names.score");

   private static final String[] PHONEME_ARRAY = {
      "b", "v", "p",
      "f",
      "hh",
      "jh",
      "k", "ch", "g",
      "l",
      "m",
      "n", "ng",
      "r",
      "er", "er1",
      "s", "sh", "z", "zh",
      "t", "th", "d", "dh",
      "w",
      "y",
      "ae", "ae1", "ey", "ey1",
      "eh", "eh1", "iy", "iy1",
      "ih", "ih1", "ay", "ay1",
      "aa", "aa1", "ow", "ow1", "ao", "ao1", "ax", "oy", "oy1", "aw", "aw1",
      "ah", "ah1", "uw", "uw1", "uh", "uh1",
   };
   private static final Map<String,Integer> PHONEME_MAP = new HashMap<String,Integer>();
   static {
      for (int i = 0; i < PHONEME_ARRAY.length; i++) {
         PHONEME_MAP.put(PHONEME_ARRAY[i], i);
      }
   }

   private static final String[][] PHONEME_SUBST_ARRAY = { // form is: {name, phoneme_1, phoneme_2, ..., phoneme_n}
           {"e", "iy1"},
           {"ae", "ey1"},
           {"h", "hh"},
           {"hh", "hh"}
   };
   private static final Map<String,String[]> PHONEME_SUBST;
   static {
      PHONEME_SUBST = new HashMap<String,String[]>();
      for (String[] substArray : PHONEME_SUBST_ARRAY) {
         String[] subst = new String[substArray.length - 1];
         System.arraycopy(substArray, 1, subst, 0, subst.length);
         PHONEME_SUBST.put(substArray[0], subst);
      }
   }

   public static final int EMPTY_TOKEN = PHONEME_ARRAY.length;
   public static final int NUM_TOKENS = EMPTY_TOKEN+1;

//   private static final Pattern DOUBLE_CONSONANT_PATTERN = Pattern.compile("(b|c|d|f|g|h|j|k|l|m|n|p|q|r|s|t|v|w|x|y|z)\\1");
//   private static final Pattern Y_CONSONANT_PATTERN = Pattern.compile("^y([^aeiou])");

   private final LetterToSound lts;

   // get the token id for a string token
   public static int getTokenId(String token) {
      if (token.length() == 0) {
         return EMPTY_TOKEN;
      }
      return PHONEME_MAP.get(token);
   }

   public static String getToken(int tokenId) {
      if (tokenId == EMPTY_TOKEN) return "";
      return PHONEME_ARRAY[tokenId];
   }

   public WeightedEditDistanceTokenizer() {
      try {
         //read cmulex_lts.bin
         lts = new LetterToSoundImpl(getClass().getClassLoader().getResource("cmulex_lts.bin"), true);
      } catch (Exception e) {
         throw new RuntimeException("cmulex_lts.bin not found", e);
      }
   }

   public String[] getPhonemes(String s) {
      // LTS needs to have double-consonants turned into single consonants or it tends to miss them altogether
      // seems to be causing more trouble than it's worth - removed 8/23/10 dwq
      // m = DOUBLE_CONSONANT_PATTERN.matcher(s);
      // s = m.replaceAll("$1");
      // LTS doesn't handle spanish y very well, so let's help it: replace y followed by a consonant with i
      if (s.length() > 2 && s.charAt(0) == 'y') {
         char c = s.charAt(1);
         if (c != 'a' && c != 'e' && c != 'i' && c != 'o' && c != 'u') {
            s = "i"+s.substring(1);
         }
      }

      String[] phonemes = lts.getPhones(s, null);
      // certain 1-2 character strings like e and h don't get assigned phonemes for some reason
      if (phonemes == null || phonemes.length == 0) {
         phonemes = PHONEME_SUBST.get(s);
         if (phonemes == null) {
            phonemes = new String[0];
            logger.warning("0-length phoneme string="+s);
         }
      }

      // fix up names like aragon
      if (s.length() > 0 &&
              (s.charAt(0) == 'a' || s.charAt(0) == 'o') &&
              phonemes.length > 0 &&
              phonemes[0].equals("r")) {
         String[] temp = new String[phonemes.length+1];
         temp[0] = "ax";
         System.arraycopy(phonemes, 0, temp, 1, phonemes.length);
         phonemes = temp;
      }

      return phonemes;
   }

   /**
    * Tokenize a (cleaned) string into a sequence of tokens.
    * @param word to tokenize
    * @return token array
    */
   public int[] tokenize(String word) {
      // Get phonemes
      String[] phonemes = getPhonemes(word);
      int[] tokens = new int[phonemes.length];
      for (int i = 0; i < tokens.length; i++) {
         tokens[i] = PHONEME_MAP.get(phonemes[i]);
      }
      return tokens;
   }
}
