//Slave
#include <SoftwareSerial.h>

const int blueTx = 2;
const int blueRx = 3;
SoftwareSerial BTSerial(blueTx, blueRx);

const int RLED = 4;
const int LLED = 5;

byte data;
unsigned long pre_time = 0;
const int duration = 500;

boolean state = 0;

void setup(){
  Serial.begin(9600);
  
  pinMode(LLED, OUTPUT);
  pinMode(RLED, OUTPUT);
  BTSerial.begin(9600);
  
  digitalWrite(LLED, LOW);
  digitalWrite(RLED, LOW);
  
  pre_time = millis();
}

void loop(){
  unsigned long cur_time = millis();
  data = BTSerial.read();
  Serial.println(data);

  if(cur_time - pre_time >= duration){
    state = !state;
    lighting(state);
    pre_time = cur_time;
  }
}

void lighting(boolean d){
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
