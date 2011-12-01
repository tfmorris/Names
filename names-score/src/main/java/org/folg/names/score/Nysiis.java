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
 * Nysiis encoding algorithm
 * Translated from http://www.dropby.com/nysiisOriginal.js
 */
public class Nysiis implements org.apache.commons.codec.StringEncoder {

   public Object encode(Object o) {
      return encode((String)o);
   }

   public String encode(String name) {
      name = name.toUpperCase().trim();

      // remove "JR", "SR", or Roman Numerals from the end of the name
      // (where "Roman Numerals" can be a malformed run of 'I' and 'V' chars)
      name = name.replaceFirst("\\s+([JS]R|[VI]+)$", "");

	   // remove all non-alpha characters
	   name = name.replaceAll("[^A-Z]+", "");

      // BEGIN ALGORITHM *******************************************

      // Transcode first characters of name:
      //	   MAC -> MCC
      //	    KN -> NN
      //	     K -> C
      //	PH, PF -> FF
      //	   SCH -> SSS
      if (name.startsWith("MAC"))
         name = name.replaceFirst("^MAC", "MCC");
      else if (name.startsWith("KN"))
         name = name.replaceFirst("^KN", "NN");
      else if(name.startsWith("K"))
         name = name.replaceFirst("^K", "C");
      else if(name.startsWith("PH"))
         name = name.replaceFirst("^PH", "FF");
      else if(name.startsWith("PF"))
         name = name.replaceFirst("^PF", "FF");
      else if(name.startsWith("SCH"))
         name = name.replaceFirst("^SCH", "SSS");

      // Transcode two-character suffix as follows,
      //	            EE, IE -> Y
      //	DT, RT, RD, NT, ND -> D
      if(name.endsWith("EE"))
         name = name.replaceFirst("EE$", "Y");
      else if(name.endsWith("IE"))
         name = name.replaceFirst("IE$", "Y");
      else if(name.matches("DT$|RT$|RD$|NT$|ND$"))
         name = name.replaceFirst("DT$|RT$|RD$|NT$|ND$", "D");

      // Save first char for later, to be used as first char of key
      String firstChar = name.substring(0,1);
      name = name.substring(1);

      // Translate remaining characters by following these rules, incrementing by one character each time:
      //	EV	->	AF 	else A,E,I,O,U	->	A
      name = name.replace("EV", "AF");
      name = name.replaceAll("[AEIOU]+", "A");
      //	Q	->	G
      name = name.replace("Q", "G");
      //	Z	->	S
      name = name.replace("Z", "S");
      //	M	->	N
      name = name.replace("M", "N");
      //	KN	->	N, else K	->	C
      name = name.replace("KN", "N");
      name = name.replace("K", "C");
      //	SCH	->	SSS
      name = name.replace("SCH", "SSS");
      //	PH	->	FF
      name = name.replace("PH", "FF");
      //	H	->	If previous or next is nonvowel, previous
      name = name.replaceAll("([^AEIOU])H", "$1");
      // DWQ: added $2 so output matches definition of Nysiis at http://www.dropby.com/NYSIIS.html
      name = name.replaceAll("(.)H([^AEIOU])", "$1$2");
      //	W 	->	If previous is vowel, previous
      name = name.replaceAll("[AEIOU]W", "A");

      // If last character is S, remove it
      name = name.replaceFirst("S$", "");

      // If last characters are AY, replace with Y
      name = name.replaceFirst("AY$", "Y");

      // If last character is A, remove it
      name = name.replaceFirst("A$", "");

      // Collapse all strings of repeated characters
      // This is more brute force that it needs to be
      name = name.replaceAll("[AEIOU]+", "A");
      name = name.replaceAll("B+", "B");
      name = name.replaceAll("C+", "C");
      name = name.replaceAll("D+", "D");
      name = name.replaceAll("F+", "F");
      name = name.replaceAll("G+", "G");
      name = name.replaceAll("H+", "H");
      name = name.replaceAll("J+", "J");
      name = name.replaceAll("K+", "K");
      name = name.replaceAll("L+", "L");
      name = name.replaceAll("M+", "M");
      name = name.replaceAll("N+", "N");
      name = name.replaceAll("P+", "P");
      name = name.replaceAll("Q+", "Q");
      name = name.replaceAll("R+", "R");
      name = name.replaceAll("S+", "S");
      name = name.replaceAll("T+", "T");
      name = name.replaceAll("V+", "V");
      name = name.replaceAll("W+", "W");
      name = name.replaceAll("X+", "X");
      name = name.replaceAll("Y+", "Y");
      name = name.replaceAll("Z+", "Z");

      // Use original first char of name as first char of key
      name = firstChar + name;

      // the NYSIIS code is only 6 chars long
      if (name.length() > 6) {
         name = name.substring(0, 6);
      }

      return name;
   }
}
