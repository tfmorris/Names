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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Remove names from a similar-names file, so that each name has a max of N similar names
 * Names are removed using a greedy algorithm, removing the lowest-scoring similar names from the name with the most names
 * until all names have no greater than N similar names
 */
public class SimilarNameRemover {
   private static Logger logger = Logger.getLogger("org.folg.names.score");

   @Option(name="-i", required=true, usage="similar names in")
   private File similarNamesInFile = null;

   @Option(name="-o", required=true, usage="similar names out")
   private File similarNamesOutFile = null;

   @Option(name="-s", required=false, usage="is surname")
   private boolean isSurname = false;

   @Option(name="-m", required=false, usage="max number of names")
   private int namesToKeep = 50;

   private Scorer scorer;

   private SimilarNameRemover() {
       scorer = isSurname ? Scorer.getSurnameInstance() : Scorer.getGivennameInstance();
   }

   private String getNameWithMostSimilarNames(Map<String,Set<String>> similarNamesMap) {
      String result = null;
      int maxNames = 0;
      for (String name : similarNamesMap.keySet()) {
         int cnt = similarNamesMap.get(name).size();
         if (cnt > maxNames) {
            maxNames = cnt;
            result = name;
         }
      }
      return result;
   }

   private Set<String> getLeastSimilarNames(String name, Set<String> similarNames, int cnt) {
      Set<String> leastSimilarNames = new HashSet<String>();

      Map<String,Double> nameScores = new HashMap<String,Double>();
      for (String similarName : similarNames) {
         nameScores.put(similarName, scorer.scoreNamePair(name, similarName));
      }

      while (leastSimilarNames.size() < cnt) {
         // get least similar name
         double leastScore = Double.POSITIVE_INFINITY;
         String leastSimilarName = null;
         for (String similarName : nameScores.keySet()) {
            double score = nameScores.get(similarName);
            if (score < leastScore) {
               leastScore = score;
               leastSimilarName = similarName;
            }
         }

         // add it to result; remove it from further consideration
         leastSimilarNames.add(leastSimilarName);
         nameScores.remove(leastSimilarName);
      }

      return leastSimilarNames;
   }

   private void doMain() {
      BufferedReader similarNamesReader = null;
      PrintWriter similarNamesWriter = null;
      Map<String,Set<String>> similarNamesMap = new LinkedHashMap<String, Set<String>>();

      try {
         similarNamesReader = new BufferedReader(new FileReader(similarNamesInFile));
         similarNamesWriter = new PrintWriter(similarNamesOutFile);
         String line;

         // read similar names
         while ((line = similarNamesReader.readLine()) != null) {
            // line is "name","similar names"
            String[] fields = line.split(",", 2);
            String name = fields[0].substring(1, fields[0].length() - 1);
            Set<String> similarNames = new TreeSet<String>();
            if (fields[1].length() > 2) {
               similarNames.addAll(Arrays.asList(fields[1].substring(1, fields[1].length() - 1).split(" ")));
            }
            similarNamesMap.put(name, similarNames);
         }

         // greedily remove names above threshold
         while (true) {
            String name = getNameWithMostSimilarNames(similarNamesMap);
            Set<String> similarNames = similarNamesMap.get(name);
            if (similarNames.size() <= namesToKeep) {
               break;
            }
            Set<String> notSimilarNames = getLeastSimilarNames(name, similarNames, similarNames.size() - namesToKeep);
            for (String notSimilarName : notSimilarNames) {
               similarNames.remove(notSimilarName);
               Set<String> notSimilarNameSimilarNames = similarNamesMap.get(notSimilarName);
               if (notSimilarNameSimilarNames == null) {
                  logger.warning("Not found: "+notSimilarName+" for name="+name);
               }
               if (!notSimilarNameSimilarNames.remove(name)) {
                  logger.warning("Name "+name+" not found in "+notSimilarName);
               }
            }
         }

         // write similar names
         for (String name : similarNamesMap.keySet()) {
            similarNamesWriter.println("\""+name+"\",\""+Utils.join(" ",similarNamesMap.get(name))+"\"");
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
      SimilarNameRemover self = new SimilarNameRemover();
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
