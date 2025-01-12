package de.uni.leipzig.tebaqa.analyzerGerman;

import de.uni.leipzig.tebaqa.helper.StanfordPipelineProvider;
import de.uni.leipzig.tebaqa.helper.StanfordPipelineProviderGerman;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.mlqa.analyzer.IAnalyzer;
import org.apache.xerces.parsers.DOMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import weka.core.Attribute;

import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;

public class TripleCandidates implements IAnalyzer {
    private static Logger log = LoggerFactory.getLogger(TripleCandidates.class);
    private Attribute attribute = null;
    private StanfordCoreNLP pipeline;
    private String serializedClassifier = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";
    private AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier.getClassifier(serializedClassifier);

    TripleCandidates() throws IOException, ClassNotFoundException {
        pipeline = StanfordPipelineProviderGerman.getSingletonPipelineInstance();

        attribute = new Attribute("TripleCandidatesCount");
    }

    /**
     * Counts all verbs, adjective and group of consecutive nouns of a question and infer the number of possible
     * SPARQL triple to generateQueries it.
     *
     * @param q A question like: What is Batman's real name?
     * @return The number of possible SPARQL triples to generateQueries the question.
     */
    @Override
    public Object analyze(String q) {
        List<List<String>> entityGroups = new ArrayList<>();
        String exceptions = "haben|ist|viele|viel|gib|rufe|liste";
        Annotation annotation = new Annotation(q);
        pipeline.annotate(annotation);
        List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            List<CoreLabel> labels = sentence.get(TokensAnnotation.class);
            boolean foundNounGroup = false;
            List<String> wordGroup = new ArrayList<>();
            for (CoreLabel token : labels) {
                String word = token.get(TextAnnotation.class);
                String pos = token.get(PartOfSpeechAnnotation.class);
                log.debug("word: "+word);
                log.debug("pos: "+pos);
                String lemma = token.get(LemmaAnnotation.class);
                if (pos.startsWith("N") /*|| "of".equals(lemma)*/) {
                    //beginning of a group of related words
                    if (foundNounGroup) {
                        wordGroup.add(word);
                    } else {
                        foundNounGroup = true;
                        wordGroup.add(word);
                    }
                } else {
                    foundNounGroup = false;
                    if (!wordGroup.isEmpty()) {
                        //flush the last group of related words
                        entityGroups.add(wordGroup);
                        wordGroup = new ArrayList<>();
                    }
                    if (pos.startsWith("AD") && !word.matches(exceptions)) {
                        List<String> adjective = new ArrayList<>();
                        adjective.add(word);
                        entityGroups.add(adjective);
                    } else if (pos.startsWith("V") && !word.matches(exceptions)) {
                            wordGroup.add(word);
                            entityGroups.add(wordGroup);
                            wordGroup = new ArrayList<>();
                    }
                }
            }
            if (!wordGroup.isEmpty()) {
                //flush the last group of related words when they are at the end of a sentence
                entityGroups.add(wordGroup);
            }
        }

        int tokenCount = entityGroups.size();
        if (tokenCount > 4) {
            return (double) 4;
        } else if (tokenCount == 4) {
            return (double) 3;
        } else if (tokenCount == 3) {
            return (double) 2;
        }
        return (double) 1;

    }



    public Attribute getAttribute() {
        return attribute;
    }
}
