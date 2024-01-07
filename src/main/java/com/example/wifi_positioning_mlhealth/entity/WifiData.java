package com.example.wifi_positioning_mlhealth.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Entity
@Getter
@Setter
public class WifiData {
    @Id
    @GeneratedValue
    private int id;
    private String position;
    @Column(columnDefinition = "json")
    private String wifiData;



}
