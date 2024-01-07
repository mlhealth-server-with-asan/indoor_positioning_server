package com.example.wifi_positioning_mlhealth.repository;

import com.example.wifi_positioning_mlhealth.entity.WifiData;
import com.example.wifi_positioning_mlhealth.entity.WifiDataDTO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WifiDataRepository extends JpaRepository<WifiData,Long> {


}
