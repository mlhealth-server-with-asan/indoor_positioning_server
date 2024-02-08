package com.example.wifi_positioning_mlhealth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class BeaconDataDTO {


    @JsonProperty("bssid")
    private String bssid;

    @JsonProperty("rssi")
    private int rssi;


    @Override
    public String toString() {
        return "BeaconDataDTO{" +
                ", bssid='" + bssid + '\'' +
                ", signalStrength=" + rssi +
                '}';
    }


}
