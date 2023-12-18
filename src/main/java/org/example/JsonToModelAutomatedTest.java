package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.rdf.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import virtuoso.jena.driver.VirtModel;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class JsonToModelAutomatedTest implements JsonToModel {

    private final Model model;
    private final Property prompt;
    private final Property gptExplanation;
    private final Property testData;
    private final Property exampleData;
    private final Property annotationType;
    private final Property usedComponent;
    private final Property usedComponentAsNum;
    private final Property dataset;
    private final Property graphId;
    private final Property explanation;
    private final Property questionId;
    private final Property question;
    @Value("${virtuoso.triplestore.endpoint}")
    private String VIRTUOSO_TRIPLESTORE_ENDPOINT;
    @Value("${virtuoso.triplestore.username}")
    private String VIRTUOSO_TRIPLESTORE_USERNAME;
    @Value("${virtuoso.triplestore.password}")
    private String VIRTUOSO_TRIPLESTORE_PASSWORD;
    private Logger logger = LoggerFactory.getLogger(JsonToModelAutomatedTest.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonToModelAutomatedTest() {
        model = ModelFactory.createDefaultModel();

        //Init Properties
        annotationType = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#annotationType");
        usedComponent = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#usedComponent");
        usedComponentAsNum = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#usedComponentAsNum");
        dataset = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#dataset");
        graphId = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#graphId");
        explanation = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#explanation");
        questionId = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#questionId");
        question = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#question");
        model.setNsPrefix("aex", "http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#");
        model.setNsPrefix("rdfs", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prompt = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#prompt");
        gptExplanation = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#gptExplanation");
        testData = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#testData");
        exampleData = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#exampleData");

    }

    public JSONObject parseFileToJSONObject() {

        try {
            org.springframework.core.io.Resource resource = new ClassPathResource("example.json");
            String jsonString = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
            return new JSONObject(jsonString);
        } catch (Exception e) {
            logger.error(e.toString());
        }
        return null;
    }

    public String parseJsonToModel(String file) {
        JSONObject jsonObject = parseFileToJSONObject();
        // Get the array of experiments
        JSONArray experiments = jsonObject.getJSONArray("explanations");
        VirtModel virtModel;

        for(int i = 0; i < experiments.length(); i++) {
            Model tempModel = model;
            JSONObject experiment = experiments.getJSONObject(i);
            String uuid = UUID.randomUUID().toString();
            Resource experimentId = model.createResource(uuid); // create unique ID
            List<Statement> statements = createStatements(experiment, experimentId);
            tempModel.add(statements);
            logger.info("UUID: {}", uuid);
            virtModel = VirtModel.openDatabaseModel("urn:aex:" + uuid, VIRTUOSO_TRIPLESTORE_ENDPOINT, VIRTUOSO_TRIPLESTORE_USERNAME, VIRTUOSO_TRIPLESTORE_PASSWORD);
            virtModel.add(tempModel);
            virtModel.close();
        }

        return jsonObject.toString();
    }

    public List<Statement> createStatements(JSONObject singleTest, Resource experimentId) {
        List<Statement> statements = new ArrayList<>();

        JSONObject testObject = singleTest.getJSONObject("testData");
        JSONArray examples = singleTest.getJSONArray("exampleData");

        statements.add(ResourceFactory.createStatement(experimentId, prompt, ResourceFactory.createPlainLiteral(singleTest.getString("prompt"))));
        statements.add(ResourceFactory.createStatement(experimentId, gptExplanation, ResourceFactory.createPlainLiteral(singleTest.getString("gptExplanation"))));
        statements.add(ResourceFactory.createStatement(experimentId, testData, setUpTestObject(testObject)));
        statements.add(ResourceFactory.createStatement(experimentId, exampleData, setUpExampleData(examples)));

        return statements;
    }

    public Resource setUpTestObject(JSONObject testData) {
        Resource resource = model.createResource();

        // Add items to object
        resource.addProperty(annotationType, ResourceFactory.createPlainLiteral(testData.getString("annotationType")));
        resource.addProperty(usedComponent, ResourceFactory.createPlainLiteral(testData.getString("usedComponent")));
        resource.addProperty(usedComponentAsNum, ResourceFactory.createPlainLiteral(String.valueOf(testData.get("componentNumber"))));
        resource.addProperty(dataset, ResourceFactory.createPlainLiteral(testData.getString("dataSet")));
        resource.addProperty(graphId, ResourceFactory.createPlainLiteral(testData.getString("graphID")));
        resource.addProperty(explanation, ResourceFactory.createPlainLiteral(testData.getString("explanation")));
        resource.addProperty(questionId, ResourceFactory.createPlainLiteral(testData.getString("questionID")));
        resource.addProperty(question, ResourceFactory.createPlainLiteral(testData.getString("question")));

        return resource;
    }

    public Seq setUpExampleData(JSONArray examples) {
        Seq seq = model.createSeq();
        int i = 0;

        while(!examples.isNull(i)) {
            seq.add(i+1, setUpTestObject(examples.getJSONObject(i)));
            i++;
        }
        return seq;
    }

}
