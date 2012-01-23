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

package org.folg.names.eval;

import org.folg.names.score.Scorer;
import org.folg.names.score.Utils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Create a similar names file by including all names scoring above a threshold
 * If you want to augment (expand) a similar-names file with additional names on existing lines, use SimilarNameAugmenter.
 */
public class SimilarNameAdder {
   private static Logger logger = Logger.getLogger("org.folg.names.score");

   @Option(name="-i", required=true, usage="common names in")
   private File commonNamesFile = null;

   @Option(name="-o", required=true, usage="similar names out")
   private File similarNamesFile = null;

   @Option(name="-t", required=false, usage="min score threshold")
   private double threshold = 0.0;

   @Option(name="-s", required=false, usage="is surname")
   private boolean isSurname = false;

   @Option(name="-b", required=false, usage="beginning name to generate")
   private int begin = 0;

   @Option(name="-n", required=false, usage="number of names to generate")
   private int maxNames = Integer.MAX_VALUE;

   private void doMain() {
      Scorer scorer = isSurname ? Scorer.getSurnameInstance() : Scorer.getGivennameInstance();
      BufferedReader commonNamesReader = null;
      PrintWriter similarNamesWriter = null;

      try {
         // read common names into a list (assume common names are already normalized)
         commonNamesReader = new BufferedReader(new FileReader(commonNamesFile));
         List<String> commonNames = new ArrayList<String>();
         String line;
         while ((line = commonNamesReader.readLine()) != null) {
            commonNames.add(line);
         }

         // for each common name, find other common names scoring above threshold and print
         similarNamesWriter = new PrintWriter(similarNamesFile);
         int cnt = 0;
         for (String name : commonNames) {
            if (cnt >= begin && cnt < begin + maxNames) {
               Set<String> similarNames = new TreeSet<String>();
               for (String otherName : commonNames) {
                  // Test only otherNames that this name is less than
                  // We'll run SimilarNameAugmenter later to add the reverse relationships
                  if (name.compareTo(otherName) < 0) {
                     double score = scorer.scoreNamePair(name, otherName);
                     if (score >= threshold) {
                        similarNames.add(otherName);
                     }
                  }
               }
               similarNamesWriter.println("\""+name+"\",\""+ Utils.join(" ", similarNames)+"\"");
               if (cnt % 1000 == 0) {
                  System.out.print(".");
               }
            }
            cnt++;
         }
         System.out.println();
      }
      catch (IOException e) {
         logger.warning("IO Exception: "+e.getMessage());
      }
      finally {
         try {
            if (commonNamesReader != null) {
               commonNamesReader.close();
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
      SimilarNameAdder self = new SimilarNameAdder();
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
