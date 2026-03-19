#include <LiquidCrystal_I2C.h>
#include "DHT.h"

#define DHTPIN 2
#define DHTTYPE DHT22

LiquidCrystal_I2C lcd(0x27,16,2); 
DHT dht(DHTPIN, DHTTYPE);

void setup() {
  if (!i2CAddrTest(0x27)) {
    lcd = LiquidCrystal_I2C(0x3F, 16, 2);
  }
  lcd.init();
  lcd.backlight();
  lcd.clear();
  lcd.setCursor(0,0);
  dht.begin();
  Serial.begin(115200);
}

void loop() {
  delay(2000);
  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println(F("Failed to read from DHT sensor"));
    return;
  }
  float heatIndex = dht.computeHeatIndex(temperature, humidity, false);
  lcd.setCursor(0,0);
  lcd.print("Temp:");
  lcd.print(temperature, 1);
  lcd.print("\xDF""C");
  lcd.setCursor(0,1);
  lcd.print("Humidity:");
  lcd.print(humidity, 1);
  lcd.print("%");
}

bool i2CAddrTest(uint8_t addr) {
  Wire.begin();
  Wire.beginTransmission(addr);
  if (Wire.endTransmission() == 0) {
    return true;
  }
  return false;
}