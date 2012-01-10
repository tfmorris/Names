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

package org.folg.names.service;

import org.folg.names.search.Normalizer;
import org.folg.names.search.Searcher;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *  Return tokens to search
 */
@Path("/search")
public class SearchService {
	@GET
   @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
   @Path("{type}/{name}")
   public Tokens get(@PathParam("type") String type, @PathParam("name") String name) {
      Tokens tokens = new Tokens();
      if (type.equals("surname") || type.equals("givenname")) {
         boolean isSurname = type.equals("surname");
         Searcher searcher = (isSurname ? Searcher.getSurnameInstance() : Searcher.getGivennameInstance());
         Normalizer normalizer = Normalizer.getInstance();
         for (String namePiece : normalizer.normalize(name, isSurname)) {
            tokens.add(namePiece);
            for (String token : searcher.getAdditionalSearchTokens(namePiece)) {
               tokens.add(token);
            }
         }
      }
      return tokens;
	}
}
