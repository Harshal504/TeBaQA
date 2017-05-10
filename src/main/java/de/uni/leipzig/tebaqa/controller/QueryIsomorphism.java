package de.uni.leipzig.tebaqa.controller;

import org.aksw.hawk.datastructures.HAWKQuestion;
import org.aksw.hawk.datastructures.HAWKQuestionFactory;
import org.aksw.qa.commons.datastructure.Question;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.PathBlock;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Source: https://github.com/AKSW/NLIWOD/blob/master/qa.hawk/src/main/java/org/aksw/hawk/experiment/QueryIsomorphism.java
 */
public class QueryIsomorphism {
    private static Logger log = Logger.getLogger(QueryIsomorphism.class);

    public static void main(String[] args) {
        HashMap<Graph, Integer> graphs = new HashMap<Graph, Integer>();
        Dataset[] datasets = Dataset.values();
        for (Dataset d : datasets) {
            List<HAWKQuestion> questions = new ArrayList<HAWKQuestion>(0);
            try {
                questions = HAWKQuestionFactory.createInstances(LoaderController.load(d));
            } catch (Exception e) {
                System.out.println(d.name());
            }
            for (Question q : questions) {
                //build the graph associated to the query
                final Graph g = GraphFactory.createDefaultGraph();
                String sparql = "";
                Query query = new Query();
                try {
                    sparql = q.getSparqlQuery();
                } catch (NullPointerException e) {
                    log.warn(e.toString(), e);
                }
                try {
                    ParameterizedSparqlString pss = new ParameterizedSparqlString();
                    pss.append(sparql);
                    query = pss.asQuery();
                } catch (QueryParseException e) {
                    log.warn(e.toString(), e);
                }
                try {
                    ElementWalker.walk(query.getQueryPattern(), new ElementVisitorBase() {
                        public void visit(ElementPathBlock el) {
                            PathBlock path = el.getPattern();
                            HashMap<Node, Node> dict = new HashMap<Node, Node>();
                            int i = 1;
                            for (TriplePath t : path.getList()) {
                                Node s = t.getSubject();
                                Node o = t.getObject();
                                Node p = NodeFactory.createLiteral("p");
                                if (dict.containsKey(s) == false) {
                                    dict.put(s, NodeFactory.createLiteral(String.valueOf(i)));
                                    i++;
                                }
                                if (dict.containsKey(o) == false) {
                                    dict.put(o, NodeFactory.createLiteral(String.valueOf(i)));
                                    i++;
                                }
                                Triple tmp = Triple.create(dict.get(s), p, dict.get(o));
                                g.add(tmp);
                            }
                        }
                    });
                    //if the Graph is not isomorphic to previously seen graphs add it to the List

                    Boolean present = false;
                    for (Graph g_present : graphs.keySet()) {
                        if (g.isIsomorphicWith(g_present)) {
                            present = true;
                            int i = graphs.get(g_present) + 1;
                            graphs.put(g_present, i);
                        }
                    }
                    if (present == false) {
                        graphs.put(g, 1);
                    }
                } catch (NullPointerException e) {
                    log.warn(e.toString(), e);
                }
            }
        }
        //look at some properties
        int i = 0;
        int j = 0;
        for (Graph g : graphs.keySet()) {
            i += graphs.get(g);
            if (graphs.get(g) > 10) {
                j += graphs.get(g);
                System.out.println(g.toString());
                System.out.println(graphs.get(g));
            }
        }
        System.out.print(i + "\n");
        System.out.print(j);
    }
}
