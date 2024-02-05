package com.example.wifi_positioning_mlhealth.service;


import com.example.wifi_positioning_mlhealth.dto.PosDataDTO;
import com.example.wifi_positioning_mlhealth.dto.ResultDataDTO;
import com.example.wifi_positioning_mlhealth.entity.BeaconData;
import com.example.wifi_positioning_mlhealth.dto.BeaconDataDTO;
import com.example.wifi_positioning_mlhealth.exception.InvalidPasswordException;
import com.example.wifi_positioning_mlhealth.repository.WifiDataRepository;
import com.example.wifi_positioning_mlhealth.util.WifiDataUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.example.wifi_positioning_mlhealth.util.PasswordEncoder;


import java.io.IOException;
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


    private final MqttPahoMessageHandler mqttMessageHandler;

    private final RabbitTemplate rabbitTemplate;



    public void addBestResultToJsonArray(ResultDataDTO bestResult, int count, String android_id) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonString = mapper.writeValueAsString(bestResult);
            JSONObject jsonObject = new JSONObject(jsonString);
            jsonObject.put("android_id", android_id);
            bestResultsArray.put(jsonObject);


            //워치 개수와 bestResult 수가 같아질 경우, JsonArray로 된 DTO를 만들어서 웹클라이언트로 전달 예정.
            if (bestResultsArray.length() == count) {
                for (int i = 0; i < bestResultsArray.length(); i++) {
                    System.out.println(bestResultsArray.get(i));
                }
                bestResultsArray = new JSONArray();
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    // 받은 PosData에서 json({macaddress, rssi})을 wifiDataEntity(DB)에 저장.
    public BeaconData addPosData(PosDataDTO posData) throws InvalidPasswordException {
        String storedHash = getStoredPasswordHash();
        if (passwordEncoder.matches(posData.getPassword(), storedHash)) {
            // Create a new WifiData entity
            BeaconData beaconDataEntity = new BeaconData();
            beaconDataEntity.setPosition(posData.getPosition());
            String wifiDataJson = convertWifiDataDtoToJson(posData.getWifiData());
//            System.out.println("wifiDataJson = " + posData.getWifiData());
            beaconDataEntity.setWifiData(wifiDataJson);
            return wifiDataRepository.save(beaconDataEntity);
        } else {
            throw new InvalidPasswordException("Invalid password");
        }
    }

    private String convertWifiDataDtoToJson(List wifiDataDTO) {
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


    public ResultDataDTO findPosition(PosDataDTO data) {

        String storedHash = getStoredPasswordHash();
        String android_id = data.getAndroid_id();

        System.out.println("data = " + data.getWifiData());


        
        if (passwordEncoder.matches(data.getPassword(), storedHash)) {


            List<BeaconDataDTO> wifiDataList = data.getWifiData();

            for (BeaconDataDTO beaconDataDTO : wifiDataList) {
                System.out.println("WifiDataDTO: " + beaconDataDTO.toString());
            }


            List<BeaconData> dbDataList = wifiDataRepository.findAll();
            List<Future<List<ResultDataDTO>>> futureResults = new ArrayList<>();

            //클라이언트가 제공한 와이파이 데이터와 데이터베이스에 저장된 와이파이 데이터를 빠르게 비교하기 위해 다중 스레딩 사용.
            int threadNum = Runtime.getRuntime().availableProcessors();
            int sliceLen = (int) Math.ceil((double) dbDataList.size()) / threadNum;

            for (int i = 0; i < threadNum-1; i++) {
                int start = sliceLen * i;
                int end = Math.min(start + sliceLen, dbDataList.size());
                List<BeaconData> slicedDataList = dbDataList.subList(start, end);
                Future<List<ResultDataDTO>> future = taskExecutor.submit(() -> calPos(slicedDataList, data, 0.6));
                futureResults.add(future);
            }

            List<ResultDataDTO> results = futureResults.stream()
                    .flatMap(future -> {
                        try {
                            return future.get().stream();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new IllegalStateException("Thread interrupted", e);
                        }
                    }).collect(Collectors.toList());

            return calcKnn(results, 1, android_id);
        } else {
            throw new InvalidPasswordException("Invalid password");
        }
    }


    // 클라이언트의 와이파이 데이터와 데이터베이스의 와이파이 데이터를 비교하여, 가장 가능성이 높은 위치 정보를 담은 리스트를 반환
    private List<ResultDataDTO> calPos(List<BeaconData> dbDataList, PosDataDTO inputData, double margin) {
        List<ResultDataDTO> resultList = new ArrayList<>();
//        System.out.println("inputData.getWifiData() = " + inputData.getWifiData());

        int largestCount = 0;

        for (BeaconData dbData : dbDataList) {
            List<BeaconDataDTO> dbWifiDataList = wifiDataUtil.parseWifiData(dbData.getWifiData());
            int count = 0;
            int sum = 0;

            for (BeaconDataDTO dbWifiData : dbWifiDataList) {
                for (BeaconDataDTO inputWifiData : inputData.getWifiData()) {
                    if (dbWifiData.getBssid().equals(inputWifiData.getBssid())) {

                        count++;
                        sum += Math.abs(dbWifiData.getRssi() - inputWifiData.getRssi());
                        break;
                    }
                }
            }

            double avg = count > 0 ? (double) sum / count : Double.MAX_VALUE;
            double ratio = count > 0 ? avg / count : Double.MAX_VALUE;

//            DecimalFormat decimalFormat = new DecimalFormat("#.####"); // 네 번째 자리까지 표시
//            avg = Double.parseDouble(decimalFormat.format(avg));
//            ratio = Double.parseDouble(decimalFormat.format(ratio));


            resultList.add(new ResultDataDTO(dbData.getId(), dbData.getPosition(), count, avg, ratio));
            largestCount = Math.max(largestCount, count);}


        int finalLargestCount = largestCount;

        List<ResultDataDTO> filteredAndSortedResults = resultList.stream()
                .filter(data -> data.getCount() >= finalLargestCount * margin)
                .sorted(Comparator.comparingDouble(ResultDataDTO::getRatio))
                .collect(Collectors.toList());

        for (ResultDataDTO data : filteredAndSortedResults) {
            System.out.println(data.getId() + " " + data.getPosition() + " " + data.getCount() + " " + data.getRatio());
        }


        return resultList.stream()
                .filter(data -> data.getCount() >= finalLargestCount * margin)
                .sorted(Comparator.comparingDouble(ResultDataDTO::getRatio))
                .collect(Collectors.toList());
    }

    // calpos 결과값을 기반으로 k개의 이웃값과 비교하여 최적값 반환.
    public ResultDataDTO calcKnn(List<ResultDataDTO> results, int k, String android_id) {
        // 결과를 ratio 오름차순으로 정렬
        List<ResultDataDTO> sortedResults = results.stream()
                .sorted(Comparator.comparingInt(ResultDataDTO::getCount).reversed()
                        .thenComparingDouble(ResultDataDTO::getRatio))
                .toList();



        // 가장 가까운 k개의 이웃을 선택
        List<ResultDataDTO> nearestNeighbors = sortedResults.stream()
                .limit(k)
                .toList();

        // 이웃들 중에서 위치별 투표 수 계산
        Map<String, Integer> positionVotes = new HashMap<>();
        for (ResultDataDTO neighbor : nearestNeighbors) {
            String position = neighbor.getPosition();
            positionVotes.put(position, positionVotes.getOrDefault(position, 0) + 1);
        }

        // 가장 많이 투표된 위치를 찾음
        String bestPosition = null;
        int maxVotes = -1;

        for (Map.Entry<String, Integer> entry : positionVotes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                bestPosition = entry.getKey();
            }
        }

        if (bestPosition != null) {
            for (ResultDataDTO result : nearestNeighbors) {
                if (result.getPosition().equals(bestPosition)) {
                    sendResultToClient(result);
                    System.out.println("result = " + result);
                    return result; // 해당 위치의 ResultDataDTO 반환
                }
            }
        }

        // 위치를 찾지 못한 경우에는 적절한 기본값 반환.
        return new ResultDataDTO(0, "not found", 0, Double.MAX_VALUE, Double.MAX_VALUE);
    }

    private void sendResultToClient(ResultDataDTO result) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonResult = mapper.writeValueAsString(result);
            byte[] payload = jsonResult.getBytes();
            Message<byte[]> message = MessageBuilder.withPayload(payload).build();
            mqttMessageHandler.handleMessage(message);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}




