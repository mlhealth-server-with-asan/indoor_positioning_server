package com.example.wifi_positioning_mlhealth.dto;


import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabelDataDTO {
    @Id
    @GeneratedValue
    private int id;
    private String position;
    private float x;
    private float y;

}
