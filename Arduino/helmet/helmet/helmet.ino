#include "I2Cdev.h"
#include "MPU6050.h"
#include <SoftwareSerial.h>
#define LLED A0
#define RLED A1
#define BUZZER 4
#define OUTPUT_READABLE_ACCELGYRO

SoftwareSerial HC06(8, 9);
SoftwareSerial HM10(11, 10);

MPU6050 accelgyro;
int ax, ay, az;
int gx, gy, gz;
int pax, pay, paz;
int pgx, pgy, pgz;
int dax, day, daz;
unsigned long acc;
char aa[16];

char ledData = '0';
int level = 0;
unsigned long cur_time;
unsigned long ledPreTime = 0;
unsigned long crashPreTime = 0;
const int duration = 500;

boolean ledState = 0;

void setup() {
  Serial.begin(9600);
  HC06.begin(9600);
  HM10.begin(9600);

  #if I2CDEV_IMPLEMENTATION == I2CDEV_ARDUINO_WIRE
      Wire.begin();
  #elif I2CDEV_IMPLEMENTATION == I2CDEV_BUILTIN_FASTWIRE
      Fastwire::setup(400, true);
  #endif
  
  accelgyro.initialize();
  
  pinMode(LLED, OUTPUT);
  pinMode(RLED, OUTPUT);
  pinMode(BUZZER, OUTPUT);
  
  digitalWrite(LLED, LOW);
  digitalWrite(RLED, LOW);
  digitalWrite(BUZZER, LOW);
  
  ledPreTime = millis();
  crashPreTime = millis();
}

void loop() {
  cur_time = millis();

  HC06.listen();
  if (HC06.available())
  {
    ledData = HC06.read();
    Serial.write(ledData);
  }
  if(cur_time - ledPreTime >= duration){
    ledState = !ledState;
    light(ledState);
    ledPreTime = cur_time;
  }
  
  pax = ax;
  pay = ay;
  paz = az;
  accelgyro.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
  dax = ax - pax;
  day = ay - pay;
  daz = az - paz;

  acc = (int)sqrt(pow((double)dax,2)+pow((double)day,2)+pow((double)daz,2));
  sprintf(aa, "%d", acc);
  IsCrash();
}

void light(boolean d){
  if(d == 1){
    if(ledData == '0'){
      digitalWrite(LLED, LOW);
      digitalWrite(RLED, LOW);
    }
    if(ledData == 'L'){
      digitalWrite(LLED, HIGH);
      digitalWrite(RLED, LOW);
    }
    if(ledData == 'R'){
      digitalWrite(LLED, LOW);
      digitalWrite(RLED, HIGH);
    }
    if(ledData == 'B'){
      digitalWrite(LLED, HIGH);
      digitalWrite(RLED, HIGH);
    }
  }
  else{
    digitalWrite(LLED, LOW);
    digitalWrite(RLED, LOW);
  }
}

void IsCrash(){
  if(level == 0 && acc > 20000){
    level = 1;
    crashPreTime = cur_time;
    HC06.write("crash\n");
    digitalWrite(BUZZER, HIGH);
    delay(50);
    digitalWrite(BUZZER, LOW);
  }
  if(level == 1 && cur_time - crashPreTime >= 5000){
    if(acc > 500){
      level = 0;
      HC06.write("Oh, it's OK\n");
      crashPreTime = cur_time;
      digitalWrite(BUZZER, HIGH);
      delay(50);
      digitalWrite(BUZZER, LOW);
      delay(50);
      digitalWrite(BUZZER, HIGH);
      delay(50);
      digitalWrite(BUZZER, LOW);
    }
  }
  if(level == 1 && cur_time - crashPreTime >= 10000){
    HC06.write("emergency\n");
    HM10.write("emregency\n");
    digitalWrite(BUZZER, HIGH);
    delay(50);
    digitalWrite(BUZZER, LOW);
    delay(50);
    digitalWrite(BUZZER, HIGH);
    delay(50);
    digitalWrite(BUZZER, LOW);
    delay(50);
    digitalWrite(BUZZER, HIGH);
    delay(50);
    digitalWrite(BUZZER, LOW);
    level = 0;
  }
}
