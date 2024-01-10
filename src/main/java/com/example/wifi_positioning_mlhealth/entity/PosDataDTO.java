package com.example.wifi_positioning_mlhealth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class PosDataDTO {

    @JsonProperty("android_id")
    private String android_id;

    @JsonProperty("position")
    private String position;

    @JsonProperty("password")
    private String password;

    @JsonProperty("wifi_data")
    private List<WifiDataDTO> wifiData;



}
