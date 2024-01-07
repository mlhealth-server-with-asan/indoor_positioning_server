package com.example.wifi_positioning_mlhealth.service;


import com.example.wifi_positioning_mlhealth.entity.PosDataDTO;
import com.example.wifi_positioning_mlhealth.entity.ResultDataDTO;
import com.example.wifi_positioning_mlhealth.entity.WifiData;
import com.example.wifi_positioning_mlhealth.entity.WifiDataDTO;
import com.example.wifi_positioning_mlhealth.exception.InvalidPasswordException;
import com.example.wifi_positioning_mlhealth.repository.WifiDataRepository;
import com.example.wifi_positioning_mlhealth.util.WifiDataUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.example.wifi_positioning_mlhealth.util.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PositionService {


    private final WifiDataRepository wifiDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final WifiDataUtil wifiDataUtil;
    private JSONArray bestResultsArray = new JSONArray(); // bestResult를 저장할 JSON 배열

    public void addBestResultToJsonArray(ResultDataDTO bestResult,int count) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonString = mapper.writeValueAsString(bestResult);
            JSONObject jsonObject = new JSONObject(jsonString);
            bestResultsArray.put(jsonObject);

            System.out.println("bestResultsArray.length() = " + bestResultsArray.length());
            //워치 개수와 bestResult 수가 같아질 경우, JsonArray로 된 DTO를 만들어서 웹클라이언트로 전달 예정.
            if( bestResultsArray.length() == 2 ) {
                for (int i = 0; i < bestResultsArray.length(); i++) {
                    System.out.println(bestResultsArray.get(i));
                }
                bestResultsArray = new JSONArray();
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    // 받은 PosData에서 json으로 묶여 있는 다수의 wifiData를 분할하여 wifiDataEntity(DB)에 저장.
    public List<WifiData> addPosData(PosDataDTO posData) throws InvalidPasswordException {
        String storedHash = getStoredPasswordHash();
        if (passwordEncoder.matches(posData.getPassword(), storedHash)) {
            List<WifiData> wifiDataEntities = new ArrayList<>();
            for (WifiDataDTO wifiDataDTO : posData.getWifiData()) {
                WifiData wifiDataEntity = new WifiData();
                wifiDataEntity.setPosition(posData.getPosition());
                String wifiDataJson = convertWifiDataDtoToJson(wifiDataDTO);
                wifiDataEntity.setWifiData(wifiDataJson);
                wifiDataEntities.add(wifiDataEntity);
            }
            return wifiDataRepository.saveAll(wifiDataEntities);
        } else {
            throw new InvalidPasswordException("Invalid password");
        }
    }

    private String convertWifiDataDtoToJson(WifiDataDTO wifiDataDTO) {
        // ObjectMapper를 사용하여 WifiDataDTO를 JSON 문자열로 변환
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(wifiDataDTO);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }


    private String getStoredPasswordHash() {
        try {
            String jsonContent = Files.readString(Paths.get("C:\\Users\\qkrwo\\OneDrive\\바탕 화면\\wifi_positioning_mlhealth\\src\\main\\resources\\static\\password.json"));
            JSONObject jsonObject = new JSONObject(jsonContent);
            return jsonObject.getString("key");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public ResultDataDTO findPosition(PosDataDTO data){
        String storedHash = getStoredPasswordHash();
        if (passwordEncoder.matches(data.getPassword(), storedHash)) {

        List<WifiData> dbDataList = wifiDataRepository.findAll();
        List<Future<List<ResultDataDTO>>> futureResults = new ArrayList<>();

        //클라이언트가 제공한 와이파이 데이터와 데이터베이스에 저장된 와이파이 데이터를 빠르게 비교하기 위해 다중 스레딩 사용.
        int threadNum = Runtime.getRuntime().availableProcessors();
        int sliceLen = (int) Math.ceil((double) dbDataList.size()) / threadNum ;

        for(int i=0; i < threadNum; i++){
            int start = sliceLen * i;
            int end = Math.min(start + sliceLen, dbDataList.size());
            List<WifiData> slicedDataList = dbDataList.subList(start,end);
            Future<List<ResultDataDTO>> future = taskExecutor.submit(() -> calPos(slicedDataList,data,0.6));
            futureResults.add(future);
        }

            List<ResultDataDTO> results = futureResults.stream()
                    .flatMap(future ->{
                try {
                    return future.get().stream();
                } catch (InterruptedException | ExecutionException e){
                    throw new IllegalStateException("Thread interrupted", e);
                }
            }).collect(Collectors.toList());

        return calcKnn(results, 4);
        } else {
            throw new InvalidPasswordException("Invalid password");
        }
    }


    // 클라이언트의 와이파이 데이터와 데이터베이스의 와이파이 데이터를 비교하여, 가장 가능성이 높은 위치 정보를 담은 리스트를 반환
    private List<ResultDataDTO> calPos(List<WifiData> dbDataList, PosDataDTO inputData, double margin) {
        List<ResultDataDTO> resultList = new ArrayList<>();
        int largestCount = 0;

        for (WifiData dbData : dbDataList) {
            List<WifiDataDTO> dbWifiDataList = wifiDataUtil.parseWifiData(dbData.getWifiData());
            int count = 0;
            int sum = 0;

            for (WifiDataDTO dbWifiData : dbWifiDataList) {
                for (WifiDataDTO inputWifiData : inputData.getWifiData()) {
                    if (dbWifiData.getBssid().equals(inputWifiData.getBssid())) {
                        count++;
                        sum += Math.abs(dbWifiData.getRssi() - inputWifiData.getRssi());
                        //System.out.println("calPos" + "Matching BSSID found: " + dbWifiData.getBssid());
                        break;
                    }
                }
            }

            double avg = count > 0 ? (double) sum / count : Double.MAX_VALUE;
            double ratio = count > 0 ? avg / count : Double.MAX_VALUE;

            resultList.add(new ResultDataDTO(dbData.getId(), dbData.getPosition(), count, avg, ratio));
            largestCount = Math.max(largestCount, count);}


        int finalLargestCount = largestCount;
        return resultList.stream()
                    .filter(data -> data.getCount() >= finalLargestCount * margin)
                    .sorted(Comparator.comparingDouble(ResultDataDTO::getRatio))
                    .collect(Collectors.toList());
        }

    // calpos 결과값을 기반으로 k개의 이웃값과 비교하여 최적값 반환.
    public ResultDataDTO calcKnn(List<ResultDataDTO> results, int k) {
        List<ResultDataDTO> limitedResults = results.stream()
                .limit(k)
                .collect(Collectors.toList());
        Map<String, List<ResultDataDTO>> groupedResults = limitedResults.stream()
                .collect(Collectors.groupingBy(ResultDataDTO::getPosition));

        String bestPosition = null;
        int maxCount = -1;

        for (Map.Entry<String, List<ResultDataDTO>> entry : groupedResults.entrySet()) {
            int count = entry.getValue().size();
            if (count > maxCount) {
                maxCount = count;
                bestPosition = entry.getKey();
            }
        }

        if (bestPosition != null) {
            List<ResultDataDTO> bestResults = groupedResults.get(bestPosition);
            ResultDataDTO bestResult = bestResults.get(0);
            addBestResultToJsonArray(bestResult,2); // bestResult를 JSON 배열에 추가

            return bestResult;
        }

        // position을 찾지 못한 경우에는 적절한 기본값 반환.
        return new ResultDataDTO(0, "not found", 0, Double.MAX_VALUE, Double.MAX_VALUE);
    }
}


