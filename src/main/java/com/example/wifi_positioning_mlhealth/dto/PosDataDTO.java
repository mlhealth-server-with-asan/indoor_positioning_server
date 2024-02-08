package com.example.wifi_positioning_mlhealth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("beacon_data")
    private List<BeaconDataDTO> beaconData;

    @Override
    public String toString() {
        return "ResultDataDTO{" +"android_id" + android_id +
                ", position='" + position + '\'' +
                '}';
    }


}
