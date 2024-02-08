package com.example.wifi_positioning_mlhealth.controller;

//import com.example.wifi_positioning_mlhealth.exception.InvalidPasswordException;
import com.example.wifi_positioning_mlhealth.dto.StateDTO;
import com.example.wifi_positioning_mlhealth.exception.InvalidPasswordException;

import com.example.wifi_positioning_mlhealth.service.PositionService;
import com.example.wifi_positioning_mlhealth.dto.PosDataDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @PostMapping("/api/receiveData")
    public ResponseEntity<?> receiveData(@RequestBody PosDataDTO posData) {
        try {
            positionService.receiveData(posData);
            return ResponseEntity.of(Optional.of(Map.of("status", "success")));
        } catch (InvalidPasswordException e) {
            // Generate한 password와 다를 경우 exception 처리.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "password invalid"));
        }
    }

    @PostMapping("/api/insertState")
    public ResponseEntity<?> insertState(@RequestBody StateDTO stateDTO) {
        try {

            positionService.insertState(stateDTO);
            return ResponseEntity.of(Optional.of(Map.of("status", "success")));
        } catch (InvalidPasswordException e) {
            // Generate한 password와 다를 경우 exception 처리.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "password invalid"));
        }
    }

    @PostMapping("/api/deleteState")
    public ResponseEntity<?> deleteState(@RequestBody StateDTO stateDTO) {
        try {

            positionService.deleteState(stateDTO);
            return ResponseEntity.of(Optional.of(Map.of("status", "success")));
        } catch (InvalidPasswordException e) {
            // Generate한 password와 다를 경우 exception 처리.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "password invalid"));
        }
    }


//    @PostMapping("/api/addData")
//    public ResponseEntity<?> addData(@RequestBody PosDataDTO posData) {
//        try {
//            positionService.addPosData(posData);
//            return ResponseEntity.of(Optional.of(Map.of("status", "success")));
//        } catch (InvalidPasswordException e) {
//            // Generate한 password와 다를 경우 exception 처리.
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "password invalid"));
//        }
//    }

//    @PostMapping("/findPosition")
//    public ResponseEntity<?> findPosition(@RequestBody PosDataDTO posData) {
//        ;
//        try {
//            return ResponseEntity.ok(positionService.findPosition(posData));
//        } catch (InvalidPasswordException e) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "password invalid"));
//        }
//
//    }



}



