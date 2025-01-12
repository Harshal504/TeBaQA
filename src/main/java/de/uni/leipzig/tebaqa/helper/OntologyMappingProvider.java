package de.uni.leipzig.tebaqa.helper;

import de.uni.leipzig.tebaqa.controller.PipelineController;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class OntologyMappingProvider {

    private static Map<String, Set<String>> ontologyMapping;
    private static Logger log = Logger.getLogger(PipelineController.class);


    //do not instantiate
    private OntologyMappingProvider() {
    }

    public static Map<String, Set<String>> getOntologyMapping() {
        //The Ontology mapping is unnecessary with the putty phrases
        //if (ontologyMapping.isEmpty()) {
        //    loadOntologyMapping();
        //}
        return new HashMap<>();
    }

    public static void setOntologyMapping(Map<String, Set<String>> ontologyMapping) {
        //Map<String, Set<String>> mapping = loadOntologyMapping();
        Map<String, Set<String>> mapping = new HashMap<>();
        OntologyMappingProvider.ontologyMapping = ontologyMapping;
        OntologyMappingProvider.ontologyMapping.putAll(mapping);
    }

    private static Map<String, Set<String>> loadOntologyMapping() {
        Map<String, Set<String>> mapping = new HashMap<>();
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(new ClassPathResource("ontology-mappings.properties").getFile()));
        } catch (IOException e) {
            log.error("Unable to load ontology-mappings.properties file.", e);
        }

        for (String key : properties.stringPropertyNames()) {
            mapping.put(key, new HashSet<>(Arrays.asList(properties.get(key).toString().split(","))));
        }
        return mapping;
    }
}
