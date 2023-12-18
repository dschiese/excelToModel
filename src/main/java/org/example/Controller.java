package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@org.springframework.stereotype.Controller
public class Controller {

    @Autowired
    private JsonToModelAutomatedTest jsonToModelAutomatedTest;

    @GetMapping("/abc")
    public ResponseEntity<?> send() {
        return new ResponseEntity<>(jsonToModelAutomatedTest.jsonToModel(), HttpStatus.OK);
    }

}
