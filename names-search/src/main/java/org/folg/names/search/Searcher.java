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

package org.folg.names.search;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.Soundex;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Functions for getting additional tokens to index and search
 * Typical use is to use the Normalizer to normalize a name, then call getAdditionalIndexTokens or getAdditionalSearchTokens on each name piece
 */
public class Searcher {
   private static Logger logger = Logger.getLogger("org.folg.names.search");
   private static final Searcher surnameStandardizer = new Searcher(true);
   private static final Searcher givennameStandardizer = new Searcher(false);
   public static Searcher getGivennameInstance() {
      return givennameStandardizer;
   }
   public static Searcher getSurnameInstance() {
      return surnameStandardizer;
   }

   private static ComboPooledDataSource staticDS = null;
   private static synchronized DataSource getDataSource(String driverClass, String jdbcUrl, String user, String password) {
     if (staticDS == null) {
        staticDS = new ComboPooledDataSource();
        try {
           Class.forName(driverClass).newInstance();
           staticDS.setDriverClass(driverClass);
        } catch (Exception e) {
           throw new RuntimeException("Error loading database driver: "+e.getMessage());
        }
        staticDS.setJdbcUrl(jdbcUrl);
        staticDS.setUser(user);
        staticDS.setPassword(password);
        Runtime.getRuntime().addShutdownHook(new Thread() {
           public void run() {
              try {
                 DataSources.destroy(staticDS);
              } catch (SQLException e) {
                 // ignore
              }
           }
        });
     }
     return staticDS;
   }

   private static class DaemonBinaryConnectionFactory extends BinaryConnectionFactory {
      @Override
      public boolean isDaemon() {
         return true;
      }
   }

   private static MemcachedClient staticMC = null;
   private static synchronized MemcachedClient getMemcachedClient(String memcacheAddresses) {
      // assume memcacheAddresses parameter always has the same value
      if (staticMC == null) {
         try {
            staticMC = new MemcachedClient(new DaemonBinaryConnectionFactory(),
                                           AddrUtil.getAddresses(memcacheAddresses));
         } catch (IOException e) {
            logger.warning("Unable to initialize memcache client");
         }
      }
      return staticMC;
   }

   private final Normalizer normalizer; // needed to read basenames and name synonyms, in case they're not normalized
   private final boolean isSurname;
   private Map<String,String[]> codeMap = null;
   private Set<String> commonNames = null;
   private Map<String,String[]> similarNames = null;
   private final StringEncoder coder;
   private Map<String,String> prefixed2base = null;
   private Map<String,List<String>> base2prefixed = null;
   private final List<String> surnameProbablePrefixes;
   private final Set<String> surnameProbablePrefixesStart;
   private DataSource dataSource = null;
   private MemcachedClient memcachedClient = null;
   private String memcacheKeyPrefix = null;
   private int memcacheExpiration = 0;

   private Searcher(final boolean isSurname) {
      this.normalizer = Normalizer.getInstance();
      this.isSurname = isSurname;
      String prefix = isSurname ? "surname" : "givenname";

      Reader similarNamesReader = null;
      Reader codeMapReader = null;
      Reader basenamesReader = null;
      try {
         // read properties
         Properties props = new Properties();
         props.load(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("searcher.properties"), "UTF8"));
         if (isSurname) {
            surnameProbablePrefixes = Arrays.asList(props.getProperty("surnameProbablePrefixList").split(","));
            surnameProbablePrefixesStart = new HashSet<String>(); // let's optimize the probable prefixes lookup just a bit
            for (String probablePrefix : surnameProbablePrefixes) {
               surnameProbablePrefixesStart.add(probablePrefix.substring(0,2));
            }
         }
         else {
            surnameProbablePrefixes = null;
            surnameProbablePrefixesStart = null;
         }

         coder = new Soundex();

         //Properties props = new Properties();
         InputStream propStream = getClass().getClassLoader().getResourceAsStream("db_memcache.properties");
         if (propStream != null) {
            props = new Properties();
            props.load(new InputStreamReader(propStream, "UTF8"));
            // read common similar names, either from the database or from a file
            String databaseDriver = props.getProperty("databaseDriver");
            if (databaseDriver != null) {
               // given and surname Standardizer's share the same dataSource
               dataSource = getDataSource(databaseDriver,
                                         props.getProperty("databaseURL"),
                                         props.getProperty("databaseUser"),
                                         props.getProperty("databasePassword"));

               // given and surname Standardizer's share the same memcachedClient
               String memcacheAddresses = props.getProperty("memcacheAddresses");
               if (memcacheAddresses != null) {
                  memcachedClient = getMemcachedClient(memcacheAddresses);
                  memcacheKeyPrefix = props.getProperty("memcacheKeyPrefix")+(isSurname ? "s|" : "g|");
                  memcacheExpiration = Integer.parseInt(props.getProperty("memcacheExpiration"));
               }
            }
         }

         // if not reading from database, read from file
         if (dataSource == null) {
            similarNamesReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(prefix + "_similar_names.csv"), "UTF8");
            readSimilarNames(similarNamesReader);
         }

         codeMapReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(prefix + "SoundexMap.txt"));
         if (codeMapReader != null) {
            // call after readSimilarNames (if we're reading the whole file into memory)
            readCodeMap(codeMapReader); // also populates commonNames
         }

         if (isSurname) {
            // read the surname prefixes file
            basenamesReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("surnamePrefixedNames.txt"), "UTF8");
            readBasenames(basenamesReader);
         }
      }
      catch (IOException e) {
         throw new RuntimeException("Error reading file:" + e.getMessage());
      }
      finally {
         try {
            if (similarNamesReader != null) {
               similarNamesReader.close();
            }
            if (codeMapReader != null) {
               codeMapReader.close();
            }
            if (basenamesReader != null) {
               basenamesReader.close();
            }
         }
         catch (IOException e) {
            // ignore
         }
      }
   }

   /**
    * Read the similar names file
    * You would not normally call this function.  Used in testing and evaluation
    * @param reader containing similar names to use
    * @throws IOException reading reader
    */
   public void readSimilarNames(Reader reader) throws IOException {
      BufferedReader bufReader = new BufferedReader(reader);
      similarNames = new HashMap<String, String[]>();
      String line;
      String[] empty = new String[0];
      while ((line = bufReader.readLine()) != null) {
         // line is "name","similar names"
         String[] fields = line.split(",");
         // intern strings so they don't take so much memory; similar names are often repeated
         String name = fields[0].substring(1, fields[0].length()-1).intern();
         if (fields.length == 2 && fields[1].length() > 2) {
            String[] names = fields[1].substring(1, fields[1].length()-1).split(" ");
            for (int i = 0; i < names.length; i++) {
               names[i] = names[i].intern();
            }
            similarNames.put(name, names);
         }
         else {
            similarNames.put(name, empty);
         }
      }
   }

   /**
    * Read code map, and populate commonNames
    * The code map file maps (soundex) codes to the common names for each code
    * You would not normally call this function.  Used in testing
    * @param reader containing code map
    * @throws IOException reading reader
    */
   public void readCodeMap(Reader reader) throws IOException {
      codeMap = new HashMap<String, String[]>();
      commonNames = new HashSet<String>();
      BufferedReader bufReader = new BufferedReader(reader);
      String line;
      while ((line = bufReader.readLine()) != null) {
         // line is code: names
         String[] fields = line.split("[: ]+",2);
         String code = fields[0];
         String[] names = fields[1].split("[, ]+");
         for (int i = 0; i < names.length; i++) {
            if (similarNames != null) {
               // intern strings if we're reading the whole similar names file into memory to save memory
               names[i] = names[i].intern();
            }
            commonNames.add(names[i]);
         }
         codeMap.put(code,names);
      }
   }

   /**
    * Read basenames
    * Basenames, aka prefixed surnames, contains a list of prefixedname,basename; e.g., mcwilliams,williams; used only for surnames
    * The file can be user-generated, space or comma-separated
    * You would not normally call this function.  Used in testing
    * @param reader reader
    * @throws IOException if error reading
    */
   public void readBasenames(Reader reader) throws IOException {
      prefixed2base = new HashMap<String, String>();
      base2prefixed = new HashMap<String, List<String>>();
      if (reader != null) {
         BufferedReader bufReader = new BufferedReader(reader);
         String line;
         while ((line = bufReader.readLine()) != null) {
            // line is basename: prefixed names
            String[] fields = line.split("[: ]+", 2);
            String basename = fields[0];
            String[] prefixedNames = fields[1].split("[, ]+");
            List<String> basenamePieces = normalizer.normalize(basename, true);
            if (basenamePieces.size() == 1) {
               basename = basenamePieces.get(0);
               List<String> basenamePrefixes = new ArrayList<String>();
               for (String prefixedName : prefixedNames) {
                  List<String> prefixedNamePieces = normalizer.normalize(prefixedName, true);
                  if (prefixedNamePieces.size() == 1) {
                     prefixedName = prefixedNamePieces.get(0);
                     prefixed2base.put(prefixedName,basename);
                     if (!basenamePrefixes.contains(prefixedName)) {
                        basenamePrefixes.add(prefixedName);
                     }
                  }
               }
               base2prefixed.put(basename, basenamePrefixes);
            }
         }
      }
   }

   private String[] readSimilarNamesFromDb(String namePiece) {
      Connection conn = null;
      PreparedStatement stmt = null;
      String[] similarNames = null;
      try {
         conn = dataSource.getConnection();
         stmt = conn.prepareStatement("SELECT similar_names from "+(isSurname ? "surname" : "givenname")+"_similar_names where name=?");
         stmt.setString(1, namePiece);
         ResultSet rs = stmt.executeQuery();
         if (rs != null && rs.next()) {
            similarNames = rs.getString(1).split(" ");
         }
      } catch (SQLException e) {
         logger.warning("Error reading from db: "+e.getMessage());
      }
      finally {
         try {
            if (stmt != null) {
               stmt.close();
            }
            if (conn != null) {
               conn.close();
            }
         } catch (SQLException e) {
            // ignore
         }
      }
      return similarNames;
   }

   // Returns the base of this surname if it starts with a probable prefix
   private String getProbableBase(String name) {
      if (name.length() >= 4 && surnameProbablePrefixesStart.contains(name.substring(0,2))) {
         for (String probablePrefix : surnameProbablePrefixes) {
            if (name.startsWith(probablePrefix) && name.length() - probablePrefix.length() >= 2) {
               return name.substring(probablePrefix.length());
            }
         }
      }
      return null;
   }

   /**
    * Get additional tokens to index
    * @param namePiece normalized name piece
    * @return tokens to index in addition to the namePiece
    */
   public Collection<String> getAdditionalIndexTokens(String namePiece) {
      Collection<String> indexTokens;

      if (commonNames.contains(namePiece)) {
         indexTokens = Collections.emptySet();
      }
      else {
         // if rare, index also under code
         indexTokens = new HashSet<String>();
         try {
            indexTokens.add(coder.encode(namePiece));
         } catch (EncoderException e) {
            // ignore
         }
         // if prefixed surname, index also under base code
         if (isSurname) {
            String basename = getProbableBase(namePiece);
            if (basename != null) {
               try {
                  indexTokens.add(coder.encode(basename));
               } catch (EncoderException e) {
                  // ignore
               }
            }
         }
      }

      return indexTokens;
   }

   public boolean exists(String namePiece) {
      if (similarNames != null) {
         return similarNames.containsKey(namePiece);
      }
      else {
         return (readSimilarNamesFromDb(namePiece) != null);
      }
   }

   public boolean isCommon(String namePiece) {
      return commonNames.contains(namePiece);
   }

   public String getCode(String namePiece) {
      try {
         return coder.encode(namePiece);
      } catch (EncoderException e) {
         logger.warning("Error encoding "+namePiece+": "+e.getMessage());
      }
      return "";
   }

   private void addSimilarNames(String namePiece, Collection<String> tokens) {
      String[] names = null;
      boolean memcacheLookupFailed = false;

      // if we read similar names from a file, look up there first
      if (similarNames != null) {
         names = similarNames.get(namePiece);
      }
      else {
         // try the cache if we have one
         if (memcachedClient != null) {
            names = (String[]) memcachedClient.get(memcacheKeyPrefix+namePiece);
            if (names == null) {
               memcacheLookupFailed = true;
            }
         }

         // try the database
         if (names == null) {
            names = readSimilarNamesFromDb(namePiece);
         }
      }

      // if all else fails, get similar names from soundex code map
      if (names == null) {
         try {
            names = codeMap.get(coder.encode(namePiece));
         } catch (EncoderException e) {
            logger.warning("Error encoding: "+namePiece);
         }
         if (names == null) {
            names = new String[0];
         }
      }

      if (memcacheLookupFailed) {
         memcachedClient.set(memcacheKeyPrefix+namePiece, memcacheExpiration, names);
      }

      Collections.addAll(tokens, names);
   }

   /**
    * Return the set of similar names for a name piece
    * You don't normally call this function. Call getAdditionalSearch tokens to also include basenames and soundex tokens
    * @param namePiece normalized name piece
    * @return similar names
    */
   public Collection<String> getSimilarNames(String namePiece) {
      Collection<String> tokens = new HashSet<String>();
      addSimilarNames(namePiece, tokens);
      return tokens;
   }

   private void addSearchTokens(String namePiece, Collection<String> tokens, boolean includeName, boolean includeCode) {
      // include exact name and code
      if (includeName) {
         tokens.add(namePiece);
      }
      if (includeCode) {
         try {
            tokens.add(coder.encode(namePiece));
         } catch (EncoderException e) {
            logger.warning("Error encoding: "+namePiece);
         }
      }

      // include similar names (and codes)
      addSimilarNames(namePiece, tokens);
   }

   public String getBasename(String namePiece) {
      String basename = null;
      if (prefixed2base != null) {
         basename = prefixed2base.get(namePiece);
         if (basename == null && !commonNames.contains(namePiece)) {
            basename = getProbableBase(namePiece);
         }
      }
      return basename;
   }

   public Collection<String> getPrefixedNames(String basename) {
      if (base2prefixed != null) {
         return base2prefixed.get(basename);
      }
      return null;
   }

   /**
    * Get additional tokens to search
    * @param namePiece normalized name piece
    * @return tokens to search in addition to the namePiece
    */
   public Collection<String> getAdditionalSearchTokens(String namePiece) {
      Collection<String> tokens = new HashSet<String>();

      // add search tokens for this name
      addSearchTokens(namePiece, tokens, false, true);

      if (isSurname) {
         // if prefixed surname, include basename and similar names
         String basename = getBasename(namePiece);
         if (basename != null) {
            addSearchTokens(basename, tokens, true, true);
         }
         else {
            // if this is a basename, include all prefixed versions (but not similar names or the codes for them)
            Collection<String> prefixedNames = getPrefixedNames(namePiece);
            if (prefixedNames != null) {
               for (String prefixedName : prefixedNames) {
                  tokens.add(prefixedName);
                  // don't add codes for prefixed names; I think it would be non-intuitive to have names with the same soundex as a prefixed form show up
                  // we index the code for the basename of rare prefixed surnames to compensate
//                  addSearchTokens(prefixedName, tokens, true, false);
               }
            }
         }
      }

      tokens.remove(namePiece); // just in case the namePiece was added
      return tokens;
   }
}
