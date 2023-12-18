package org.example;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.json.JSONObject;

import java.util.List;

public interface JsonToModel {

    void parseJsonToModel(JSONObject jsonObject);

    List<Statement> createStatements(JSONObject singleTest, Resource experimentId);



}
