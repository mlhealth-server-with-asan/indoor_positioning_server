package com.example.wifi_positioning_mlhealth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class WifiDataDTO {


    @JsonProperty("bssid")
    private String bssid;

    @JsonProperty("rssi")
    private int rssi;


}
