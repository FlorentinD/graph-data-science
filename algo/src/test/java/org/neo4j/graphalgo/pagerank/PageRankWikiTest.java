/*
 * Copyright (c) 2017-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphalgo.pagerank;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.neo4j.graphalgo.AlgoTestBase;
import org.neo4j.graphalgo.TestDatabaseCreator;
import org.neo4j.graphalgo.TestSupport.AllGraphTypesWithoutCypherTest;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.api.GraphFactory;
import org.neo4j.graphalgo.core.GraphLoader;
import org.neo4j.graphalgo.results.CentralityResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.neo4j.graphalgo.QueryRunner.runInTransaction;

final class PageRankWikiTest extends AlgoTestBase {

    static PageRank.Config DEFAULT_CONFIG = new PageRank.Config(40, 0.85, PageRank.DEFAULT_TOLERANCE);

    private static final String DB_CYPHER =
            "CREATE" +
            "  (a:Node {name: 'a'})" +
            ", (b:Node {name: 'b'})" +
            ", (c:Node {name: 'c'})" +
            ", (d:Node {name: 'd'})" +
            ", (e:Node {name: 'e'})" +
            ", (f:Node {name: 'f'})" +
            ", (g:Node {name: 'g'})" +
            ", (h:Node {name: 'h'})" +
            ", (i:Node {name: 'i'})" +
            ", (j:Node {name: 'j'})" +
            ", (k:Node {name: 'k'})" +

            // a (dangling node)
            // b
            ", (b)-[:TYPE]->(c)" +
            // c
            ", (c)-[:TYPE]->(b)" +
            // d
            ", (d)-[:TYPE]->(a)" +
            ", (d)-[:TYPE]->(b)" +
            // e
            ", (e)-[:TYPE]->(b)" +
            ", (e)-[:TYPE]->(d)" +
            ", (e)-[:TYPE]->(f)" +
            // f
            ", (f)-[:TYPE]->(b)" +
            ", (f)-[:TYPE]->(e)" +
            // g
            ", (g)-[:TYPE]->(b)" +
            ", (g)-[:TYPE]->(e)" +
            // h
            ", (h)-[:TYPE]->(b)" +
            ", (h)-[:TYPE]->(e)" +
            // i
            ", (i)-[:TYPE]->(b)" +
            ", (i)-[:TYPE]->(e)" +
            // j
            ", (j)-[:TYPE]->(e)" +
            // k
            ", (k)-[:TYPE]->(e)";

    @BeforeEach
    void setupGraphDb() {
        db = TestDatabaseCreator.createTestDatabase();
        runQuery(DB_CYPHER);
    }

    @AfterEach
    void shutdownGraphDb() {
        if (db != null) db.shutdown();
    }

    @AllGraphTypesWithoutCypherTest
    void test(Class<? extends GraphFactory> graphFactory) {
        final Label label = Label.label("Node");
        final Map<Long, Double> expected = new HashMap<>();

        runInTransaction(db, () -> {
            expected.put(db.findNode(label, "name", "a").getId(), 0.3040965);
            expected.put(db.findNode(label, "name", "b").getId(), 3.5658695);
            expected.put(db.findNode(label, "name", "c").getId(), 3.180981);
            expected.put(db.findNode(label, "name", "d").getId(), 0.3625935);
            expected.put(db.findNode(label, "name", "e").getId(), 0.7503465);
            expected.put(db.findNode(label, "name", "f").getId(), 0.3625935);
            expected.put(db.findNode(label, "name", "g").getId(), 0.15);
            expected.put(db.findNode(label, "name", "h").getId(), 0.15);
            expected.put(db.findNode(label, "name", "i").getId(), 0.15);
            expected.put(db.findNode(label, "name", "j").getId(), 0.15);
            expected.put(db.findNode(label, "name", "k").getId(), 0.15);
        });

        final Graph graph = new GraphLoader(db)
                .withLabel("Node")
                .withRelationshipType("TYPE")
                .withDirection(Direction.OUTGOING)
                .load(graphFactory);

        final CentralityResult rankResult = PageRankAlgorithmType.NON_WEIGHTED
                .create(graph, DEFAULT_CONFIG, LongStream.empty())
                .compute()
                .result();

        IntStream.range(0, expected.size()).forEach(i -> {
            final long nodeId = graph.toOriginalNodeId(i);
            assertEquals(
                    expected.get(nodeId),
                    rankResult.score(i),
                    1e-2,
                    "Node#" + nodeId
            );
        });
    }
}
