#include <LiquidCrystal_I2C.h>
#include "DHT.h"
#include "BH1750.h"

#define DHTPIN 16
#define DHTTYPE DHT22

#define LED 15
#define PIRPIN 0

// Separate I2C bus
#define BH1750_SCL 13 // Clock
#define BH1750_SDA 12 // Data

LiquidCrystal_I2C lcd(0x27, 16, 2); 
DHT dht(DHTPIN, DHTTYPE);
BH1750 bh1750;

int currentScreen = 0;
unsigned long lastSwitch = 0; // Timestamp at moment of screen switch
const unsigned long interval = 2000;
bool draw = true; // Only draw on screen change to avoid flickering/corruption

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
  Wire1.setSDA(BH1750_SDA);
  Wire1.setSCL(BH1750_SCL);
  Wire1.begin();
  if (!bh1750.begin(BH1750::CONTINUOUS_HIGH_RES_MODE, 0x23, &Wire1)) {
    Serial.println("BH1750 not found");
  }
  lcd.setCursor(0,0);
  dht.begin();
  pinMode(PIRPIN, INPUT);
  pinMode(LED, OUTPUT);
  Serial.begin(115200);
  lastSwitch = millis();
}

void loop() {
  unsigned long now = millis();
  if(now - lastSwitch >= interval) { // At least the interval period since last screen switch
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
  int motion = digitalRead(PIRPIN); 
  if(motion == HIGH) {
    Serial.println("High");
    digitalWrite(LED, HIGH);
  }
  else {
    Serial.println("Low");
    digitalWrite(LED, LOW);
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
}

bool i2CAddrTest(uint8_t addr) {
  Wire.begin();
  Wire.beginTransmission(addr);
  if (Wire.endTransmission() == 0) {
    return true;
  }
  return false;
}