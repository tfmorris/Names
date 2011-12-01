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

import org.folg.names.search.Normalizer;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.Soundex;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Augments (expands) a similar-names file with additional names.
 * This program adds new names to existing lines.  If you want a program to add new lines to a similar-names file, use SimilarNameAdder
 */
public class SimilarNameAugmenter {
   private static Logger logger = Logger.getLogger("org.folg.names.score");

   @Option(name="-i", required=true, usage="similar names in")
   private File similarNamesInFile = null;

   @Option(name="-o", required=true, usage="similar names out")
   private File similarNamesOutFile = null;

   @Option(name="-n", required=true, usage="names to add in")
   private File namesFile = null;

   @Option(name="-s", required=false, usage="is surname")
   private boolean isSurname = false;

   @Option(name="-a", required=false, usage="add all name combinations")
   private boolean allCombos = false;

   @Option(name="-p", required=false, usage="add pair-wise combinations")
   private boolean pairwiseCombos = false;

   private int uncommonTargets = 0;
   StringEncoder coder = new Soundex();
   Collection<String> commonNames = new HashSet<String>();

   private boolean add(Map<String,Set<String>> namesToAdd, String target, String source) {
      Normalizer normalizer = Normalizer.getInstance();
      List<String> targets = normalizer.normalize(target, isSurname);
      List<String> sources = normalizer.normalize(source, isSurname);
      if (targets.size() == 1 && sources.size() == 1) {
         target = targets.get(0);
         source = sources.get(0);
         try {
            if (!source.equals(target)) {
               String sourceCode = coder.encode(source);
               String targetCode = coder.encode(target);
               // only add name to target if it is common
               if (commonNames.contains(target)) {
                  // only add source if it is common, or if its soundex doesn't match target's soundex
                  if (commonNames.contains(source) || !sourceCode.equals(targetCode)) {
                     Set<String> names = namesToAdd.get(target);
                     if (names == null) {
                        names = new HashSet<String>();
                        namesToAdd.put(target, names);
                     }
                     names.add(source);
                  }
               }
               else if (!sourceCode.equals(targetCode)) {
                  uncommonTargets++;
                  logger.info("Source "+source+" not added to uncommon target: "+target);
               }
            }
            return true;
         }
         catch (EncoderException e) {
            logger.warning("EncoderException "+e.getMessage());
         }
      }
      return false;
   }

   private void doMain() {
      BufferedReader similarNamesReader = null;
      BufferedReader namesReader = null;
      PrintWriter similarNamesWriter = null;
      String line;
      Map<String,Set<String>> namesToAdd = new HashMap<String,Set<String>>();

      try {
         // read existing similar names file to get list of all "common" names
         similarNamesReader = new BufferedReader(new FileReader(similarNamesInFile));
         while ((line = similarNamesReader.readLine()) != null) {
            // line is "name","similar names"
            String[] fields = line.split(",",2);
            String name = fields[0].substring(1, fields[0].length() - 1);
            commonNames.add(name);
         }
         similarNamesReader.close();
         similarNamesReader = null;

         // read names to add and construct a name->similar names table
         namesReader = new BufferedReader(new FileReader(namesFile));
         while ((line = namesReader.readLine()) != null) {
            String[] fields = line.split("[:, ]+");
            boolean result = true;
            for (int i = 1; i < fields.length; i++) {
               result = result && add(namesToAdd, fields[0], fields[i]);
               if (pairwiseCombos || allCombos) {
                  result = result && add(namesToAdd, fields[i], fields[0]);
                  if (allCombos) {
                     for (int j = 1; j < fields.length; j++) {
                        if (i != j) {
                           result = result && add(namesToAdd, fields[i], fields[j]);
                        }
                     }
                  }
               }
            }
            if (!result) {
               logger.warning("Invalid line: "+line);
            }
         }
         logger.info("Total uncommon targets: "+uncommonTargets);

         // read existing similar names file
         similarNamesReader = new BufferedReader(new FileReader(similarNamesInFile));
         similarNamesWriter = new PrintWriter(similarNamesOutFile);
         while ((line = similarNamesReader.readLine()) != null) {
            // line is "name","similar names"
            String[] fields = line.split(",",2);
            String name = fields[0].substring(1, fields[0].length() - 1);
            Set<String> similarNames = new TreeSet<String>();
            for (String similarName : fields[1].substring(1, fields[1].length() - 1).split(" ")) {
               similarNames.add(similarName);
            }

            // add additional similar names
            Set<String> names = namesToAdd.get(name);
            if (names != null) {
               for (String similarName : names) {
                  similarNames.add(similarName);
               }
            }

            // write line
            similarNamesWriter.println("\""+name+"\",\""+Utils.join(" ",similarNames)+"\"");
         }
      }
      catch (IOException e) {
         logger.warning("IO Exception: "+e.getMessage());
      }
      finally {
         try {
            if (similarNamesReader != null) {
               similarNamesReader.close();
            }
            if (namesReader != null) {
               namesReader.close();
            }
            if (similarNamesWriter != null) {
               similarNamesWriter.close();
            }
         }
         catch (IOException e) {
            // ignore
         }
      }
   }

   public static void main(String[] args) throws IOException {
      SimilarNameAugmenter self = new SimilarNameAugmenter();
      CmdLineParser parser = new CmdLineParser(self);
      try {
         parser.parseArgument(args);
         self.doMain();
      }
      catch (CmdLineException e) {
         // handling of wrong arguments
         System.err.println(e.getMessage());
         parser.printUsage(System.err);
      }
   }
}
