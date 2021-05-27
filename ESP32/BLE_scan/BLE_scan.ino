/*
   Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleScan.cpp
   Ported to Arduino ESP32 by Evandro Copercini
*/

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>

int scanTime = 1; //In seconds
int count = 0;
int total = 0;
BLEScan* pBLEScan;

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
    void onResult(BLEAdvertisedDevice advertisedDevice) {
      if(advertisedDevice.getName() == "IoT_Beacon"){
          int rssi = advertisedDevice.getRSSI();
          count++;
          total += rssi;
      }
    }
}; 
//d8:a0:1d:5e:18:12
//-49
BLECharacteristic *pCharacteristic;
void setup() {
  Serial.begin(115200);
  Serial.println("Scanning...");

  BLEDevice::init("IoT_Beacon");
  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       );

  pCharacteristic->setValue("No collisions");
  pService->start();
  // BLEAdvertising *pAdvertising = pServer->getAdvertising();  // this still is working for backward compatibility
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);  // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
  Serial.println("Characteristic defined! Now you can read it in your phone!");

  pBLEScan = BLEDevice::getScan(); //create new scan
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setActiveScan(true); //active scan uses more power, but get results faster
  pBLEScan->setInterval(100);
  pBLEScan->setWindow(99);  // less or equal setInterval value
  delay(2000);
}

void loop() {
  count = 0;
  total = 0;
  // put your main code here, to run repeatedly:
  BLEScanResults foundDevices = pBLEScan->start(scanTime, false);
  Serial.print("Devices found: ");
  Serial.println(foundDevices.getCount());
  Serial.println("Scan done!");

  bool collision = false;
  
  if(count != 0){
      Serial.print("Average rssi: ");
      Serial.println(total / count);
    
      Serial.print("Distance: ");
      double dist = pow(10, (-46.0 - (total / count)) / 20);
      if(dist < 1.5f){
          pCharacteristic->setValue("Collision");
          pCharacteristic->notify();
          collision = true;
      } else if(collision == true){
         pCharacteristic->setValue("No collisions");
         pCharacteristic->notify();
    
         collision = false;
      }
      
      Serial.println(dist);
  } else if(collision == true){
      pCharacteristic->setValue("No collisions");
      pCharacteristic->notify();

      collision = false;
  }
  
  pBLEScan->clearResults();   // delete results fromBLEScan buffer to release memory
  delay(1000);
}
