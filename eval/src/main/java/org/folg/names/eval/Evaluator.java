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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Evaluate P/R against an ancestry labeled file
 * User: dallan
 */
public abstract class Evaluator {
   private static Logger logger = Logger.getLogger("org.folg.names.eval");

   protected abstract boolean match(String name1, String name2);

   protected void evaluate(File labeledFile) throws IOException {
      int truePos = 0;
      int falsePos = 0;
      int trueNeg = 0;
      int falseNeg = 0;
      BufferedReader reader = null;
      Normalizer normalizer = Normalizer.getInstance();

      try {
         reader = new BufferedReader(new FileReader(labeledFile));

         // read header line
         String line = reader.readLine();
         String[] fields = line.split(",");
         boolean hasNicknameField = "\"Nickname\"".equals(fields[1]);

         while ((line = reader.readLine()) != null) {
            fields = line.split(",");
            String label = fields[0];
            // ignore any pairs not definitely marked as match or not-match
            if (label.length() == 0 || "1".equals(label)) {
               boolean labeledMatch = (label.length() == 0);
               String name1 = fields[hasNicknameField ? 2 : 1].replace("\"", "");
               String name2 = fields[hasNicknameField ? 3 : 2].replace("\"", "");
               // names were normalized, but not all non-roman characters were romanized, so normalize again
               List<String> names1 = normalizer.normalize(name1,false);
               List<String> names2 = normalizer.normalize(name2,false);
               if (names1.size() != 1 || names2.size() != 1) {
                  logger.warning("invalid line: "+line+" name1="+name1+" name2="+name2);
                  continue;
               }
               name1 = names1.get(0);
               name2 = names2.get(0);

               boolean match = match(name1, name2);
               if (labeledMatch) {
                  if (match) {
                     truePos++;
                  }
                  else {
                     falseNeg++;
                  }
               }
               else {
                  if (match) {
                     falsePos++;
                  }
                  else {
                     trueNeg++;
                  }
               }
            }
         }
      }
      catch (IOException e) {
         logger.severe("IO error: "+e);
         e.printStackTrace();
      }
      finally {
         if (reader != null) {
            reader.close();
         }
      }

      System.out.println("RESULTS");
      System.out.println("TruePos ="+truePos+" FalseNeg="+falseNeg);
      System.out.println("FalsePos="+falsePos+"  TrueNeg="+trueNeg);
      double prec = truePos*1.0/(truePos+falsePos);
      double recall = truePos*1.0/(truePos+falseNeg);
      double f = 1.0/(.5*(1/prec + 1/recall));
      System.out.println("Precision="+prec);
      System.out.println("Recall="+recall);
      System.out.println("F="+f);
   }
}
