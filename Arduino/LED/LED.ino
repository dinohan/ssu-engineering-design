#include <SoftwareSerial.h>
#define LLED A0
#define RLED A1

SoftwareSerial HC06(8, 9);

char data = '0';
unsigned long pre_time = 0;
const int duration = 500;

boolean ledState = 0;

void setup() {
  Serial.begin(9600);
  HC06.begin(9600);
  
  pinMode(LLED, OUTPUT);
  pinMode(RLED, OUTPUT);
  
  digitalWrite(LLED, LOW);
  digitalWrite(RLED, LOW);
  
  pre_time = millis();
}

void loop() {
  unsigned long cur_time = millis();
  
  if (HC06.available())
  {
    data = HC06.read();
    Serial.write(data);
  }
  

  if(cur_time - pre_time >= duration){
    ledState = !ledState;
    light(ledState);
    pre_time = cur_time;
  }
}

void light(boolean d){
  if(d == 1){
    if(data == '0'){
      digitalWrite(LLED, LOW);
      digitalWrite(RLED, LOW);
    }
    if(data == 'L'){
      digitalWrite(LLED, HIGH);
      digitalWrite(RLED, LOW);
    }
    if(data == 'R'){
      digitalWrite(LLED, LOW);
      digitalWrite(RLED, HIGH);
    }
    if(data == 'B'){
      digitalWrite(LLED, HIGH);
      digitalWrite(RLED, HIGH);
    }
  }
  else{
    digitalWrite(LLED, LOW);
    digitalWrite(RLED, LOW);
  }
}
