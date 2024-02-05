package com.example.wifi_positioning_mlhealth.repository;

import com.example.wifi_positioning_mlhealth.entity.BeaconData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WifiDataRepository extends JpaRepository<BeaconData,Long> {


}
