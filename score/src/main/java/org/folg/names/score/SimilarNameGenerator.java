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

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generate a list of similar names from the common names
 */
public class SimilarNameGenerator {
   public static final double GIVENNAME_DEFAULT_CLASSIFIER_THRESHOLD = 2.3;
   public static final double GIVENNAME_DEFAULT_CLUSTER_THRESHOLD = -0.75;
   public static final double SURNAME_DEFAULT_CLASSIFIER_THRESHOLD = 0.7;
   public static final double SURNAME_DEFAULT_CLUSTER_THRESHOLD = -2.0;

   private final boolean useClusters;
   private final double defaultClassifierThreshold;
   private final double defaultClusterThreshold;
   private final FeaturesGenerator featuresGenerator;
   private final FeaturesScorer featuresScorer;
   private Cluster[] clusters;
   private Codes[] clusterCodes;
   private String[] commonNames;
   private Codes[] commonNameCodes;

   private static class Cluster {
      String name;
      String[] memberNames;

      Cluster(String name, String[] memberNames) {
         this.name = name;
         this.memberNames = memberNames;
      }
   }

   public SimilarNameGenerator(final boolean isSurname, final boolean useClusters) {
      this.useClusters = useClusters;
      InputStream clustersStream = null;

      try {
         this.defaultClassifierThreshold = (isSurname ? SURNAME_DEFAULT_CLASSIFIER_THRESHOLD : GIVENNAME_DEFAULT_CLASSIFIER_THRESHOLD);
         this.defaultClusterThreshold = (isSurname ? SURNAME_DEFAULT_CLUSTER_THRESHOLD : GIVENNAME_DEFAULT_CLUSTER_THRESHOLD);
         this.featuresGenerator = new FeaturesGenerator(isSurname);
         this.featuresScorer = new FeaturesScorer(isSurname);

         // read clusters if they are available
         String prefix = isSurname ? "surname" : "givenname";
         clustersStream = getClass().getClassLoader().getResourceAsStream(prefix + "Clusters.txt");
         if (clustersStream != null) {
            setClusters(new InputStreamReader(clustersStream, "UTF8"));
         }
      }
      catch (IOException e) {
         throw new RuntimeException("Error reading file:" + e.getMessage());
      }
      finally {
         try {
            if (clustersStream != null) {
               clustersStream.close();
            }
         }
         catch (IOException e) {
            // ignore
         }
      }
   }

   /**
    * Read the clusters file
    * You would not normally call this function. Used in testing
    * @param reader containing clusters to use
    * @throws IOException reading reader
    */
   public void setClusters(Reader reader) throws IOException {
      BufferedReader bufReader = new BufferedReader(reader);
      List<Cluster> clusterList = new ArrayList<Cluster>();
      String line;
      int cnt = 0;
      while ((line = bufReader.readLine()) != null) {
         // line is cluster name : member names
         String[] fields = line.split("[: ]+",2);
         String clusterName = fields[0];
         String[] memberNames;
         if (fields.length > 1) {
            memberNames = fields[1].split("[, ]+");
         }
         else {
            memberNames = new String[0];
         }
         Cluster cluster = new Cluster(clusterName, memberNames);
         clusterList.add(cluster);
         cnt += 1 + memberNames.length;
      }

      // we'll iterate over these names a lot; let's optimize
      if (useClusters) {
         clusters = clusterList.toArray(new Cluster[clusterList.size()]);
         clusterCodes = new Codes[clusters.length];
         for (int i = 0; i < clusterCodes.length; i++) {
            clusterCodes[i] = featuresGenerator.getCodes(clusters[i].name);
         }
      }
      else {
         commonNames = new String[cnt];
         commonNameCodes = new Codes[cnt];
         int i = 0;
         for (Cluster c : clusterList) {
            commonNames[i] = c.name;
            commonNameCodes[i] = featuresGenerator.getCodes(c.name);
            i++;
            for (String name : c.memberNames) {
               commonNames[i] = name;
               commonNameCodes[i] = featuresGenerator.getCodes(name);
               i++;
            }
         }
      }
   }

   private void testName(String name, Codes codes, String testName, Features features, List<NameScore> similarNames, double classifierThreshold) {
      if (!name.equals(testName)) {
         Codes testCodes = featuresGenerator.getCodes(testName);
         featuresGenerator.setFeatures(name, codes, testName, testCodes, features);
         double score = featuresScorer.score(features);
         if (score >= classifierThreshold) {
            similarNames.add(new NameScore(testName,score));
         }
      }
   }

   private void testCluster(String name, Codes codes, Cluster cluster, Features features, List<NameScore> similarNames, double classifierThreshold) {
      testName(name, codes, cluster.name, features, similarNames, classifierThreshold);
      for (String memberName : cluster.memberNames) {
         testName(name, codes, memberName, features, similarNames, classifierThreshold);
      }
   }

   static class NameScore implements Comparable {
      String name;
      double score;

      NameScore(String name, double score) {
         this.name = name;
         this.score = score;
      }

      public int compareTo(Object o) {
         NameScore that = (NameScore)o;
         if (this.score < that.score) {
            return 1;
         }
         else if (this.score > that.score) {
            return -1;
         }
         else {
            return this.name.compareTo(that.name);
         }
      }
   }

   /**
    * Generate names that are similar to (within threshold of) a specified name
    * @param name specified name
    * @param classifierThreshold return names >= this threshold
    * @param clusterThreshold if using clusters, check clusters >= this threshold
    * @return array of similar names, sorted by score
    */
   public String[] generateSimilarNames(String name, double classifierThreshold, double clusterThreshold, int maxNames) {
      List<NameScore> similarNames = new ArrayList<NameScore>();
      Features features = new Features();
      Codes codes = featuresGenerator.getCodes(name);

      // compare to common names in nearby clusters
      if (useClusters) {
         Cluster closestCluster = null;
         double closestClusterScore = Double.NEGATIVE_INFINITY;
         for (int i = 0; i < clusters.length; i++) {
            Cluster cluster = clusters[i];
            String clusterName = cluster.name;
            Codes clusterNameCodes = clusterCodes[i];
            featuresGenerator.setFeatures(name, codes, clusterName, clusterNameCodes, features);
            double score = featuresScorer.score(features);
            if (score > closestClusterScore) {
               closestCluster = cluster;
               closestClusterScore = score;
            }
            // test all clusters closer than min cluster threshold
            if (score >= clusterThreshold) {
               testCluster(name, codes, cluster, features, similarNames, classifierThreshold);
            }
         }
         // if no clusters have been tested yet, test the closest one
         if (closestClusterScore < clusterThreshold && closestCluster != null) {
            testCluster(name, codes, closestCluster, features, similarNames, classifierThreshold);
         }
      }
      else {
         // compare to each common name
         for (int i = 0; i < commonNames.length; i++) {
            String commonName = commonNames[i];
            if (!name.equals(commonName)) {
               Codes commonCodes = commonNameCodes[i];
               featuresGenerator.setFeatures(name, codes, commonName, commonCodes, features);
               double score = featuresScorer.score(features);
               if (score >= classifierThreshold) {
                  similarNames.add(new NameScore(commonName, score));
               }
            }
         }
      }

      // convert list to array
      NameScore[] nameScores = similarNames.toArray(new NameScore[similarNames.size()]);

      // sort to get the highest-scoring names
      Arrays.sort(nameScores);

      // filter to return up to maxNames
      String[] result = new String[Math.min(nameScores.length, maxNames)];
      for (int i = 0; i < nameScores.length && i < maxNames; i++) {
         result[i] = nameScores[i].name;
      }

      return result;
   }

   public String[] generateSimilarNames(String name, double classifierThreshold, double clusterThreshold) {
      return generateSimilarNames(name, classifierThreshold, clusterThreshold, Integer.MAX_VALUE);
   }
   public String[] generateSimilarNames(String name, double classifierThreshold) {
      return generateSimilarNames(name, classifierThreshold, defaultClusterThreshold, Integer.MAX_VALUE);
   }

   public String[] generateSimilarNames(String name) {
      return generateSimilarNames(name, defaultClassifierThreshold, defaultClusterThreshold, Integer.MAX_VALUE);
   }
}
