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

import org.folg.names.search.Normalizer;
import org.folg.names.search.Searcher;
import org.apache.commons.codec.EncoderException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Evaluate the P/R of a table against Ancestry's labeled data
 */
public class TableEvaluator extends Evaluator {
   private static Logger logger = Logger.getLogger("org.folg.names.eval");

   @Option(name="-l", required=true, usage="ancestry labeled data in")
   private File labeledFile = null;

   @Option(name="-t", required=false, usage="table (csv) in (uses default if not provided)")
   private File tableFile = null;

   @Option(name="-s", required=false, usage="surnames")
   private boolean isSurname;

   private Searcher searcher;
   private Normalizer normalizer;

   @Override
   protected boolean match(String name1, String name2) {
      List<String> names1 = normalizer.normalize(name1, isSurname);
      List<String> names2 = normalizer.normalize(name2, isSurname);
      if (names1.size() > 0) {
         name1 = names1.get(0);
      }
      if (names2.size() > 0) {
         name2 = names2.get(0);
      }
      // are we searching on the exact name?
      Collection<String> tokens1 = searcher.getAdditionalSearchTokens(name1);
      if (name1.equals(name2) || tokens1.contains(name2)) {
         return true;
      }
      // are we searching on an additional index token?
      for (String token2 : searcher.getAdditionalIndexTokens(name2)) {
         if (name1.equals(token2) || tokens1.contains(token2)) {
            return true;
         }
      }
      return false;
   }

   public void doMain() throws IOException {
      normalizer = Normalizer.getInstance();
      if (isSurname) {
         searcher = Searcher.getSurnameInstance();
      }
      else {
         searcher = Searcher.getGivennameInstance();
      }
      if (tableFile != null) {
         logger.info("Reading "+tableFile.getAbsolutePath());
         searcher.readSimilarNames(new FileReader(tableFile));
      }
      evaluate(labeledFile);
   }

   public static void main(String[] args) throws IOException, EncoderException {
      TableEvaluator self = new TableEvaluator();
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
