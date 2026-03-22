#include <LiquidCrystal_I2C.h>
#include "DHT.h"
#include "BH1750.h"
#include "WiFi.h"
#include "config.h"

// DHT
#define DHT_PIN 16
#define DHT_TYPE DHT22
DHT dht(DHT_PIN, DHT_TYPE);

// LCD
LiquidCrystal_I2C lcd(0x27, 16, 2); 
int currentScreen = 0;
unsigned long lastSwitch = 0; // Timestamp at moment of screen switch
const unsigned long INTERVAL = 2000;
bool draw = true; // Only draw on screen change to avoid flickering/corruption

// BH1750
// Separate I2C bus
#define BH1750_SCL 13 // Clock
#define BH1750_SDA 12 // Data
BH1750 bh1750;

// PIR
#define LED 15
#define PIR_PIN 0
const unsigned long PIR_WAIT = 60000;
unsigned long lastMotion = 0; // Timestamp at moment of real motion detected
const unsigned long MOTION_HOLD = 2000; // How long light remains on after last motion
unsigned long motionStart = 0; // Timestamp when pin goes HIGH
const unsigned long MOTION_CONFIRM = 200; // Must be HIGH for this long to be considered real

// WiFi
WiFiServer server(80); // Listening on port 80 (HTTP)
const char* ssid = WIFI_SSID;
const char* password = WIFI_PASSWORD;
byte connected[8] = {
  0b01110,
  0b10001,
  0b00000,
  0b00100,
  0b01010,
  0b00000,
  0b00100,
  0b00000,
};

void setup() {
  Serial.begin(115200);
  // LCD on Wire
  Wire.begin();
  if (!i2CAddrTest(0x27)) {
    lcd = LiquidCrystal_I2C(0x3F, 16, 2);
  }
  lcd.init();
  lcd.createChar(0, connected);
  lcd.backlight();
  lcd.clear();
  // BH1750 on Wire1 to avoid address conflicts with Wire
  /*
  Wire1.setSDA(BH1750_SDA);
  Wire1.setSCL(BH1750_SCL);
  Wire1.begin();
  if (!bh1750.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, 0x23, &Wire1)) {
    Serial.println("BH1750 not found");
  }
  */
  lcd.setCursor(0,0);
  dht.begin();
  pinMode(PIR_PIN, INPUT);
  pinMode(LED, OUTPUT);
  lastSwitch = millis();
  WiFi.mode(WIFI_STA); // WiFi station mode
  WiFi.begin(ssid, password); // Connects to router
  server.begin(); // Listens to TCP connections
}

void loop() {
  unsigned long now = millis();
  if(now - lastSwitch >= INTERVAL) { // At least the interval period since last screen switch
    lastSwitch = now;
    lcd.clear();
    currentScreen = (currentScreen + 1) % 2; // Cycles between number of screens
    draw = true;
  }
  // DHT22
  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  if(isnan(humidity) || isnan(temperature)) {
    Serial.println("Failed to read from DHT sensor");
    return;
  }
  //float heatIndex = dht.computeHeatIndex(temperature, humidity, false);
  // BH1750
  float lux = -1;
  if(bh1750.measurementReady()) {
    //lux = bh1750.readLightLevel();
  }
  // HC-SR501 PIR
  int motion = digitalRead(PIR_PIN);
  if(now >= PIR_WAIT) {
    if(motion == HIGH) {
      if(motionStart == 0) {
        motionStart = now;
      }
      if(now - motionStart >= MOTION_CONFIRM) { // HIGH for long enough to be real motion
        lastMotion = now;
        Serial.println("Motion detected");
        digitalWrite(LED, HIGH);
      }
    } 
    else {
      motionStart = 0;
      if(now - lastMotion > MOTION_HOLD) { // Turns off LED after hold period
        Serial.println("No motion detected");
        digitalWrite(LED, LOW);
      }
    }
  }
  else {
    Serial.print("PIR warming up, seconds remaining: ");
    Serial.println((PIR_WAIT - now) / 1000);
  }
  // LCD
  if(draw) {
    lcd.clear();
    switch(currentScreen) {
      case 0:
        lcd.setCursor(0, 0);
        lcd.print("Temperature:");
        lcd.setCursor(0, 1);
        lcd.print(temperature, 1);
        lcd.print("\xDF""C");
        break;
      case 1:
        lcd.setCursor(0, 0);
        lcd.print("Humidity:");
        lcd.setCursor(0, 1);
        lcd.print(humidity, 1);
        lcd.print("%");
        break;
    }
    if(WiFi.status() == WL_CONNECTED) {
      lcd.setCursor(15, 0);
      lcd.write(byte(0));
    }
    draw = false;
  }
  if(WiFi.status() == WL_CONNECTED) {
    // Check if any client (would be Android application) connected
    WiFiClient client = server.accept();
    if(!client) {
      return;
    }
    while(client.available() == 0) { // Wait for client to send HTTP request
      if(millis() - now > 3000) { // Timeout
        client.stop();
        return;
      }
    }
    while(client.available()) { // Read and discard HTTP request (only need to send sensor data)
      client.read();
    }
    String json = "{";
    json += "\"temperature\":" + String(temperature, 1) + ",";
    json += "\"humidity\":" + String(humidity, 1) + ",";
    json += "\"lux\":" + String(lux, 1) + ",";
    json += "\"motion\":" + String(motion == HIGH ? "true" : "false") + "}";
    // HTTP response
    client.println("HTTP/1.1 200 OK");
    client.println("Content-Type: application/json");
    client.println("Connection: close");
    client.println(); // Separates headers from body
    client.println(json);
    client.stop();
  }
  delay(100);
}

bool i2CAddrTest(uint8_t addr) {
  Wire.begin();
  Wire.beginTransmission(addr);
  if(Wire.endTransmission() == 0) {
    return true;
  }
  return false;
}