package com.example.wifi_positioning_mlhealth.util;

import com.example.wifi_positioning_mlhealth.entity.WifiData;
import com.example.wifi_positioning_mlhealth.entity.WifiDataDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class WifiDataUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static List<WifiDataDTO> parseWifiData(String wifiDataJson) {
        try {
            // JSON 문자열이 객체 리스트를 나타내는지 확인
            if (wifiDataJson.trim().startsWith("[")) {
                return objectMapper.readValue(wifiDataJson, new TypeReference<List<WifiDataDTO>>() {});
            } else {
                // JSON 문자열이 단일 객체를 나타내면, 이를 리스트에 추가
                WifiDataDTO singleData = objectMapper.readValue(wifiDataJson, WifiDataDTO.class);
                return Collections.singletonList(singleData);
            }
        } catch (Exception e) {
            // JSON 파싱 중 오류 발생 시 처리
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
