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

import org.folg.names.score.Scorer;
import org.folg.names.search.Normalizer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 *  Return the score of two name pieces
 */
@Path("/score")
public class ScoreService {
	@GET
   @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
   @Path("{type}/{name1}/{name2}")
	public Score get(@PathParam("type") String type, @PathParam("name1") String name1, @PathParam("name2") String name2) {
      double bestScore = Double.NEGATIVE_INFINITY;
      if (type.equals("surname") || type.equals("givenname")) {
         boolean isSurname = type.equals("surname");
         Normalizer normalizer = Normalizer.getInstance();
         Scorer scorer = (isSurname ? Scorer.getSurnameInstance() : Scorer.getGivennameInstance());
         List<String> namePieces1 = normalizer.normalize(name1, isSurname);
         List<String> namePieces2 = normalizer.normalize(name2, isSurname);
         for (String namePiece1 : namePieces1) {
            for (String namePiece2 : namePieces2) {
               double score = scorer.scoreNamePair(namePiece1, namePiece2);
               if (score > bestScore) {
                  bestScore = score;
               }
            }
         }
      }
      return new Score(bestScore);
	}
}
