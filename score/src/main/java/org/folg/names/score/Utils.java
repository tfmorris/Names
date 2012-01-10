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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * You'd think that a mature language like java would have these functions built-in
 */
public class Utils {
   public static String join(String glue, Collection c) {
      StringBuilder buf = new StringBuilder();
      for (Object o : c) {
         if (buf.length() > 0) buf.append(glue);
         buf.append(o.toString());
      }
      return buf.toString();
   }

   public static String join(String glue, Object[] c) {
      StringBuilder buf = new StringBuilder();
      for (Object o : c) {
         if (buf.length() > 0) buf.append(glue);
         buf.append(o.toString());
      }
      return buf.toString();
   }

   public static String join(String glue, int[] c) {
      StringBuilder buf = new StringBuilder();
      for (int o : c) {
         if (buf.length() > 0) buf.append(glue);
         buf.append(o);
      }
      return buf.toString();
   }

   @SuppressWarnings("unchecked")
   public static Collection intersect(Collection c1, Collection c2) {
      Set result = new HashSet();
      for (Object o : c1) {
         if (c2.contains(o)) {
            result.add(o);
         }
      }
      return result;
   }
}
