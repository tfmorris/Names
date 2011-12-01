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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Daitch-Mokotov soundex algorithm, following instructions at http://www.jewishgen.org/infofiles/soundex.html
 * However, that page recommends generating multiple codes for certain letter combinations (ch, ck, c, j, rs, and rz)
 * In order to return just a single code, the algorithm below generates the first code in all cases
 */
public class DMSoundex implements org.apache.commons.codec.StringEncoder
{
   public static final int MAX_CODE_LEN = 7;

   public static final char[] VOWELS = {'a', 'e', 'i', 'o', 'u'};

   public static final String[][] START_CODE_ARRAY = {
      {"0", "ai", "aj", "ay", "au", "a", "ei", "ej", "ey", "e", "i", "oi", "oj", "oy","o", "ui", "uj", "uy", "u", "ue"},
      {"1", "eu", "ia", "ie", "io", "iu", "j", "y"},
      {"2", "schtsch", "schtsh", "schtch", "shtch", "shch", "shtsh", "sht", "scht", "schd", "stch",
       "stsch", "sc", "strz", "strs", "stsh", "st", "szcz", "szcs", "szt", "shd", "szd", "sd",
       "zdz", "zdzh", "zhdzh", "zd", "zhd"},
      {"3", "d", "dt", "th", "t"},
      {"4", "cz", "czs", "csz", "drz", "drs", "ds", "dsh", "dsz", "dz", "dzh", "dzs", "sch", "sh", "sz", "s",
       "tch", "ttch", "ttsch", "trz", "trs", "tsch", "tsh", "ts", "tts", "ttsz", "tc", "tz", "ttz", "tzs", "tsz",
       "zh", "zs", "zsch", "zsh", "z"},
      {"5", "chs", "ch", "ck", "c", "g", "h", "ks", "kh", "k", "q", "x"},
      {"6", "m", "n"},
      {"7", "b", "fb", "f", "p", "pf", "ph", "v", "w"},
      {"8", "l"},
      {"9", "r"},
      {"94", "rz", "rs"},
   };

   public static final String[][] BEFORE_VOWEL_CODE_ARRAY = {
      {"1", "ai", "aj", "ay", "ei", "ej", "ey", "eu", "oi", "oj", "oy", "ui", "uj", "uy", },
      {"3", "d", "dt", "th", "t"},
      {"4", "cz", "czs", "csz", "drz", "drs", "ds", "dsh", "dsz", "dz", "dzh", "dzs", "sch", "sh", "sz", "s",
       "schtsch", "schtsh", "schtch", "shtch", "shch", "shtsh", "stch", "stsch", "sc", "strz", "strs", "stsh",
       "szcz", "szcs", "zdz", "zdzh", "zhdzh",
       "tch", "ttch", "ttsch", "trz", "trs", "tsch", "tsh", "ts", "tts", "ttsz", "tc", "tz", "ttz", "tzs", "tsz",
       "zh", "zs", "zsch", "zsh", "z"},
      {"5", "ch", "ck", "c", "g", "h", "kh", "k", "q", },
      {"6", "m", "n"},
      {"7", "au", "b", "fb", "f", "p", "pf", "ph", "v", "w"},
      {"8", "l", },
      {"9", "r"},
      {"43", "sht", "scht", "schd", "st", "szt", "shd", "szd", "sd", "zd", "zhd"},
      {"54", "chs", "ks", "x"},
      {"66", "mn", "nm"},
      {"94", "rz", "rs"},
   };

   public static final String[][] OTHER_CODE_ARRAY = {
      {"3", "d", "dt", "th", "t"},
      {"4", "cz", "czs", "csz", "drz", "drs", "ds", "dsh", "dsz", "dz", "dzh", "dzs", "sch", "sh", "sz", "s",
       "schtsch", "schtsh", "schtch", "shtch", "shch", "shtsh", "stch", "stsch", "sc", "strz", "strs", "stsh",
       "szcz", "szcs", "zdz", "zdzh", "zhdzh",
       "tch", "ttch", "ttsch", "trz", "trs", "tsch", "tsh", "ts", "tts", "ttsz", "tc", "tz", "ttz", "tzs", "tsz",
       "zh", "zs", "zsch", "zsh", "z"},
      {"5", "ch", "ck", "c", "g", "kh", "k", "q", },
      {"6", "m", "n"},
      {"7", "b", "fb", "f", "p", "pf", "ph", "v", "w"},
      {"8", "l", },
      {"9", "r"},
      {"43", "sht", "scht", "schd", "st", "szt", "shd", "szd", "sd", "zd", "zhd"},
      {"54", "chs", "ks", "x"},
      {"66", "mn", "nm"},
      {"94", "rz", "rs"},
   };

   private Map<String,String>[] startCodeMaps;
   private Map<String,String>[] beforeVowelCodeMaps;
   private Map<String,String>[] otherCodeMaps;
   private int maxCodeLength;

   @SuppressWarnings("unchecked")
   private Map<String,String>[] generateMaps(String[][] codeArray) {
      Map<String,String>[] maps = new HashMap[MAX_CODE_LEN];

      for (int i = 0; i < MAX_CODE_LEN; i++) {
         maps[i] = new HashMap<String,String>();
      }
      for (String[] codes : codeArray) {
         String code = codes[0];
         for (int i = 1; i < codes.length; i++) {
            int len = codes[i].length();
            maps[len-1].put(codes[i], code);
         }
      }

      return maps;
   }

   public DMSoundex() {
      this(6);
   }

   public DMSoundex(int maxCodeLength) {
      startCodeMaps = generateMaps(START_CODE_ARRAY);
      beforeVowelCodeMaps = generateMaps(BEFORE_VOWEL_CODE_ARRAY);
      otherCodeMaps = generateMaps(OTHER_CODE_ARRAY);
      this.maxCodeLength = maxCodeLength;
   }

   public Object encode(Object o) {
      return encode((String)o);
   }

   /**
    *
    * @param s must be romanized and in lower case
    * @return
    */
   public String encode(String s) {
      StringBuilder buf = new StringBuilder();
      int pos = 0;
      boolean atBegin = true;
      boolean prevSkipped = false;
      while (pos < s.length() && buf.length() < maxCodeLength) {
         boolean found = false;
         for (int len = Math.min(s.length() - pos, MAX_CODE_LEN); len > 0; len--) {
            int nextPos = pos+len;
            String token = s.substring(pos, nextPos);
            String code = null;
            if (atBegin) {
               code = startCodeMaps[len-1].get(token);
            }
            else if (nextPos < s.length() && Arrays.binarySearch(VOWELS, s.charAt(nextPos)) >= 0) {
               code = beforeVowelCodeMaps[len-1].get(token);
            }
            else {
               code = otherCodeMaps[len-1].get(token);
            }
            if (code != null) {
               if (prevSkipped || code.length() != 1 || buf.length() == 0 || buf.charAt(buf.length()-1) != code.charAt(0)) {
                  buf.append(code);
               }
               pos = nextPos;
               found = true;
               break;
            }
         }
         atBegin = false;
         if (found) {
            prevSkipped = false;
         }
         else {
            prevSkipped = true;
            pos++;
         }
      }
      if (buf.length() > maxCodeLength) {
         buf.setLength(maxCodeLength);
      }
      return buf.toString();
   }
}
