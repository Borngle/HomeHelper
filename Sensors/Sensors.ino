#include <LiquidCrystal_I2C.h>
#include "DHT.h"
#include "BH1750.h"

#define DHT_PIN 16
#define DHT_TYPE DHT22

#define LED 15
#define PIR_PIN 0

// Separate I2C bus
#define BH1750_SCL 13 // Clock
#define BH1750_SDA 12 // Data

LiquidCrystal_I2C lcd(0x27, 16, 2); 
DHT dht(DHT_PIN, DHT_TYPE);
BH1750 bh1750;

int currentScreen = 0;
unsigned long lastSwitch = 0; // Timestamp at moment of screen switch
const unsigned long INTERVAL = 2000;
bool draw = true; // Only draw on screen change to avoid flickering/corruption

const unsigned long PIR_WAIT = 60000;
unsigned long lastMotion = 0; // Timestamp at moment of real motion detected
const unsigned long MOTION_HOLD = 2000; // How long light remains on after last motion
unsigned long motionStart = 0; // Timestamp when pin goes HIGH
const unsigned long MOTION_CONFIRM = 200; // Must be HIGH for this long to be considered real

void setup() {
  // LCD on Wire
  Wire.begin();
  if (!i2CAddrTest(0x27)) {
    lcd = LiquidCrystal_I2C(0x3F, 16, 2);
  }
  lcd.init();
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
  Serial.begin(115200);
  lastSwitch = millis();
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
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("Failed to read from DHT sensor");
    return;
  }
  float heatIndex = dht.computeHeatIndex(temperature, humidity, false);
  // BH1750
  float lux = -1;
  if(bh1750.measurementReady()) {
    //lux = bh1750.readLightLevel();
  }
  // HC-SR501 PIR
  if (now >= PIR_WAIT) {
    int motion = digitalRead(PIR_PIN);
    if (motion == HIGH) {
      if (motionStart == 0) {
        motionStart = now;
      }
      if (now - motionStart >= MOTION_CONFIRM) { // HIGH for long enough to be real motion
        lastMotion = now;
        Serial.println("Motion detected");
        digitalWrite(LED, HIGH);
      }
    } 
    else {
      motionStart = 0;
      if (now - lastMotion > MOTION_HOLD) { // Turns off LED after hold period
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
    draw = false;
  }
  delay(100);
}

bool i2CAddrTest(uint8_t addr) {
  Wire.begin();
  Wire.beginTransmission(addr);
  if (Wire.endTransmission() == 0) {
    return true;
  }
  return false;
}