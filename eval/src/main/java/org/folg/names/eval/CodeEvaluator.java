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

import org.folg.names.score.DMSoundex;
import org.folg.names.score.Nysiis;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.Soundex;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Evaluate the P/R of coder against Ancestry's labeled data
 */
public class CodeEvaluator extends Evaluator {
   private static Logger logger = Logger.getLogger("org.folg.names.eval");

   @Option(name="-l", required=true, usage="ancestry labeled data in")
   private File labeledFile = null;

   @Option(name="-c", required=false, usage="coder: nysiis, dmsoundex, soundex")
   private String coderString = null;

   private StringEncoder coder;

   @Override
   protected boolean match(String name1, String name2) {
      try {
         return coder.encode(name1).equals(coder.encode(name2));
      }
      catch (EncoderException e) {
         logger.warning("Encoder exception on "+name1+" or "+name2+" : "+e.getMessage());
      }
      return false;
   }

   public void doMain() throws IOException {
      if ("nysiis".equals(coderString)) {
         coder = new Nysiis();
      }
      else if ("dmsoundex".equals(coderString)) {
         coder = new DMSoundex();
      }
      else if ("soundex".equals(coderString) || coderString == null) {
         coder = new Soundex();
      }
      else {
         throw new RuntimeException("Unknown coder: "+coderString);
      }
      evaluate(labeledFile);
   }

   public static void main(String[] args) throws IOException, EncoderException {
      CodeEvaluator self = new CodeEvaluator();
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
