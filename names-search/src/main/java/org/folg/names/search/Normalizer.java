/*
 * Copyright 2011 Ancestry.com and Foundation for On-Line Genealogy, Inc.
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Logger;

/**
 * Normalize a name
 * Call normalize() before calling other functions in the library
 */
public class Normalizer {
   private static Logger logger = Logger.getLogger("org.folg.names.search");
   private static final String DTR_SUFFIX = "(sdt?r?|s?d(a|aa|o)?(t|tt)[eioa]?r)$";
   private static final Normalizer normalizer = new Normalizer();

   private int maxTerms;
   private final Set<Character> delimiters;
   private final Map<Character,String> characterReplacements;
   private final Set<String> deletions;
   private final Set<String> conjunctions;
   private final Set<String> titles;
   private final Set<String> surnamePrefixes;
   private final Map<String,String> abbreviatedGivenNames;

   /**
    * Get the normalizer instance
    */
   public static Normalizer getInstance() {
      return normalizer;
   }

   /**
    * Replace dtr-like suffix with son
    * You don't normally need to call this function; it's called during standardization
    * @param name tokenized name piece
    * @return name with a dtr-like suffix replaced with son
    */
   public static String replaceDtrSuffix(String name) {
      String nameSon = name.replaceFirst(DTR_SUFFIX, "son");
      // don't replace the dtr suffix if the entire name matched the suffix
      return nameSon.equals("son") ? name : nameSon;
   }

   private Normalizer() {
      // read properties file
      try {
         Properties props = new Properties();
         props.load(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("normalizer.properties"), "UTF8"));
         maxTerms = Integer.parseInt(props.getProperty("maxTerms"));
         delimiters = new HashSet<Character>();
         for (char c : props.getProperty("delimiterList").toCharArray()) {
            delimiters.add(c);
         }
         characterReplacements = new HashMap<Character,String>();
         for (String replacement : props.getProperty("characterReplacements").split(",")) {
            characterReplacements.put(replacement.charAt(0), replacement.substring(2));
         }
         deletions = getSet(props.getProperty("deleteList"));
         conjunctions = getSet(props.getProperty("conjunctionList"));
         titles = getSet(props.getProperty("titleList"));
         surnamePrefixes = getSet(props.getProperty("surnamePrefixList"));
         abbreviatedGivenNames = new HashMap<String,String>();
         for (String abbreviation : props.getProperty("abbreviatedGivenNames").split(",")) {
            String[] fields = abbreviation.split("\\|");
            abbreviatedGivenNames.put(fields[0], fields[1]);
         }
      } catch (IOException e) {
         throw new RuntimeException("normalizer.properties not found");
      }
   }

   private Set<String> getSet(String list) {
      Set<String> result = new HashSet<String>();
      result.addAll(Arrays.asList(list.split(",")));
      return result;
   }

   /**
    * Returns whether this word is a surname prefix
    * @param name normalized name
    * @return true if so
    */
   public boolean isSurnamePrefix(String name) {
      return surnamePrefixes.contains(name);
   }

   /**
    * Get all surname prefixes
    * @return surname prefixes
    */
   public Set<String> getSurnamePrefixes() {
      return surnamePrefixes;
   }

   /**
    * Tokenize name: use default delimiters, don't allow wildcards
    * @see #tokenize(String,Set, boolean)
    */
   public List<String> tokenize(String name) {
      return tokenize(name, delimiters, false);
   }

   /**
    * Tokenize name: use default delimiters
    * @see #tokenize(String,Set, boolean)
    */
   public List<String> tokenize(String name, boolean allowWildcards) {
      return tokenize(name, delimiters, allowWildcards);
   }

   /**
    * Tokenize name by removing diacritics, lowercasing, removing ending 's, splitting on delimiters, and removing non a-z characters
    * Normally you would follow this function with a call to normalize the tokens returned
    * , or simply call normalize on the name directly
    * @param name name string
    * @param delimiters array of characters to tokenize on
    * @param allowWildcards if true, allow *? in name pieces
    * @return tokenized name pieces
    */
   public List<String> tokenize(String name, Set<Character> delimiters, boolean allowWildcards) {
      // remove diacritics, lowercase, remove ending 's, split on delimiters, remove non-az
      List<String> pieces = new ArrayList<String>();
      StringBuilder buf = new StringBuilder();

      for (int i = 0; i < name.length(); i++) {
         char c = name.charAt(i);
         String replacement;
         if (delimiters.contains(c)) {
            if (buf.length() > 0) {
               pieces.add(buf.toString());
               buf.setLength(0);
               if (pieces.size() == maxTerms) {
                  break;
               }
            }
         }
         else if ((replacement = characterReplacements.get(c)) != null) {
            buf.append(replacement.toLowerCase());
         }
         else if (c == '\'' && i == name.length()-2 && Character.toLowerCase(name.charAt(i)) == 's') {
            break;
         }
         else if (c >= 'A' && c <= 'Z') {
            buf.append(Character.toLowerCase(c));
         }
         else if (c >= 'a' && c <= 'z') {
            buf.append(c);
         }
         else if (Character.isLetter(c)) {
            // ignore letters > U+0250; they're generally from scripts that don't map well to roman letters
            // ignore 186,170: superscript o and a used in spanish numbers: 1^a and 2^o
            // ignore 440,439: Ezh and reverse-Ezh; the only times they appear in the data is in what appears to be noise
            if (c < 592 && c!=186 && c!=170 && c!=439 && c!=440) {
               logger.warning("Untokenized letter:"+c+" ("+(int)c+") in "+name);
            }
         }
      }
      if (buf.length() > 0 && pieces.size() < maxTerms) {
         pieces.add(buf.toString());
      }
      return pieces;
   }

   /**
    * Normalize the name by tokenizing using default delimiters and normalizing (keeping post-conjunction)
    * @see #tokenize(String,Set, boolean)
    * @see #normalize(List, boolean, boolean)
    */
   public List<String> normalize(String name, boolean isSurname, boolean allowWildcards) {
      List<String> namePieces = tokenize(name, allowWildcards);
      return normalize(namePieces, isSurname, false);
   }

   /**
    * Normalize the name by tokenizing using default delimiters, removing wildcards, and normalizing (keeping post-conjunction)
    * @see #tokenize(String,Set, boolean)
    * @see #normalize(List, boolean, boolean)
    */
   public List<String> normalize(String name, boolean isSurname) {
      List<String> namePieces = tokenize(name, false);
      return normalize(namePieces, isSurname, false);
   }

   private String includePrefix(StringBuilder prefixBuffer, String piece) {
      if (prefixBuffer.length() > 0) {
         prefixBuffer.append(piece);
         piece = prefixBuffer.toString();
         prefixBuffer.setLength(0);
      }
      return piece;
   }

   private void addNormalizedPiece(List<String> normalizedPieces, String piece, boolean isSurname) {
      String expandedGivenName;
      if (isSurname) {
         piece = replaceDtrSuffix(piece);
      }
      else if ((expandedGivenName = abbreviatedGivenNames.get(piece)) != null) {
         piece = expandedGivenName;
      }
      normalizedPieces.add(piece);
   }

   /**
    * Normalize the name by removing noise words, titles, combining surname prefixes, and expanding abbreviated given names
    * @param namePieces tokenized name pieces
    * @param isSurname true if name pieces are a surname
    * @param removePostConjunction if true, remove all pieces after a conjunction (e.g., "Dallan or Allan" would become just Dallan)
    * @return normalized name pieces
    */
   public List<String> normalize(List<String> namePieces, boolean isSurname, boolean removePostConjunction) {
      // remove deletions, remove titles, combine surname prefixes, expand abbreviated given names
      List<String> normalizedPieces = new ArrayList<String>();
      StringBuilder prefixBuffer = new StringBuilder();
      String title = null;
      //String expandedGivenName;

      for (String piece : namePieces) {
         if (deletions.contains(piece)) {
            // omit
         }
         else if (conjunctions.contains(piece) && (normalizedPieces.size() > 0 || title != null)) {
            // add title if we have one waiting; handles "queen or quinn"
            if (title != null && normalizedPieces.size() == 0) {
               addNormalizedPiece(normalizedPieces, title, isSurname);
               title = null;
            }
            if (removePostConjunction) {
               break;
            }
         }
         else if (titles.contains(piece)) {
            if (prefixBuffer.length() > 0) {
               // if we have a prefix, include it and add the name; handles "mc'queen-rodriguez"
               piece = includePrefix(prefixBuffer, piece);
               addNormalizedPiece(normalizedPieces, piece, isSurname);
               title = null;
            }
            // keep the last title
            else {
               title = piece;
            }
         }
         else if (isSurname && surnamePrefixes.contains(piece)) {
            prefixBuffer.append(piece);
         }
         else {
            // if we have a prefix, include it
            piece = includePrefix(prefixBuffer, piece);
            addNormalizedPiece(normalizedPieces, piece, isSurname);
         }
      }
      // add title if that's all we found
      if (normalizedPieces.size() == 0 && title != null) {
         addNormalizedPiece(normalizedPieces, title, isSurname);
      }
      // add prefix if not already included
      if (prefixBuffer.length() > 0) {
         addNormalizedPiece(normalizedPieces, prefixBuffer.toString(), isSurname);
      }
      return normalizedPieces;
   }
}
