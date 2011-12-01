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
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Add one or more names to a similar-names file.
 * This program adds new lines to a similar-names file. The names should not already exist in the file.
 * If you want to augment (expand) a similar-names file with additional names on existing lines, use SimilarNameAugmenter.
 */
public class SimilarNameAdder {
   private static Logger logger = Logger.getLogger("org.folg.names.score");

   @Option(name="-i", required=true, usage="similar names in")
   private File similarNamesInFile = null;

   @Option(name="-o", required=true, usage="similar names out")
   private File similarNamesOutFile = null;

   @Option(name="-s", required=false, usage="is surname")
   private boolean isSurname = false;

   @Option(name="-n", required=true, usage="names to add in")
   private File namesFile = null;

   @Option(name="-m", required=false, usage="max number of names to add")
   private int namesToAdd = Integer.MAX_VALUE;

   private void doMain() {
      BufferedReader similarNamesReader = null;
      BufferedReader namesReader = null;
      PrintWriter similarNamesWriter = null;
      Set<String> seenNames = new HashSet<String>();
      Normalizer normalizer = Normalizer.getInstance();
      SimilarNameGenerator generator = new SimilarNameGenerator(isSurname, false);

      try {
         similarNamesReader = new BufferedReader(new FileReader(similarNamesInFile));
         namesReader = new BufferedReader(new FileReader(namesFile));
         similarNamesWriter = new PrintWriter(similarNamesOutFile);
         String line;

         // read similar names into memory, and write them out
         while ((line = similarNamesReader.readLine()) != null) {
            // line is "name","similar names"
            String[] fields = line.split(",",2);
            seenNames.add(fields[0].substring(1, fields[0].length() - 1));
            similarNamesWriter.println(line);
         }

         // for each name from namesReader
         System.out.print("Processing");
         int cnt = 0;
         while (cnt < namesToAdd && (line = namesReader.readLine()) != null) {
            // line is name,other stuff
            String[] fields = line.split("[:,]+",2); // ignore stuff after the first comma or colon
            // normalize the name into possibly multiple name pieces
            for (String name : normalizer.normalize(fields[0], isSurname)) {
               // skip 1-character names and names we've already seen
               if (name.length() > 1 && seenNames.add(name)) {
                  Collection<String> similarNames = new TreeSet<String>();
                  // generate the similar names, in alphabetical order
                  for (String similarName : generator.generateSimilarNames(name)) {
                     similarNames.add(similarName);
                  }
                  // write them out
                  similarNamesWriter.println("\""+name+"\",\""+Utils.join(" ",similarNames)+"\"");
                  cnt++;
                  if (cnt % 1000 == 0) {
                     System.out.print(".");
                  }
               }
            }
         }
         System.out.println();
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
