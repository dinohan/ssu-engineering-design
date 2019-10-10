#include <SoftwareSerial.h>

const int blueTx = 2;
const int blueRx = 3;
SoftwareSerial btSerial(blueTx, blueRx);

const int RB=4;
const int LB=5;

boolean LlastButton=LOW;
boolean LcurrentButton=LOW;
boolean RlastButton=LOW;
boolean RcurrentButton=LOW;

boolean LOn =0;
boolean ROn =0;

void setup(){
  Serial.begin(9600);
  btSerial.begin(9600);
  pinMode(LB, INPUT);
  pinMode(RB, INPUT);
}

void loop(){
  LcurrentButton=Ldebounce(LlastButton);
  if(LlastButton == LOW && LcurrentButton == HIGH){
    LOn = !LOn;
  }
  LlastButton=LcurrentButton;
  
  RcurrentButton=Rdebounce(RlastButton);
  if(RlastButton == LOW && RcurrentButton == HIGH){
    ROn = !ROn;
  }
  RlastButton=RcurrentButton;
  
  signal();
}

void signal(){
  if(LOn && ROn){
    Serial.println('B');
    btSerial.write('B');
  }
  else if(LOn && !ROn){
    Serial.println('L');
    btSerial.write('L');
  }
  else if(!LOn && ROn){
    Serial.println('R');
    btSerial.write('R');
  }
  else{
    Serial.println('0');
    btSerial.write('0');
  }
}

boolean Ldebounce(boolean last){
  boolean current = digitalRead(LB);
  
  if(last != current){
    delay(10);
    
    current=digitalRead(LB);
  }
  return current;
}
boolean Rdebounce(boolean last){
  boolean current = digitalRead(RB);
  
  if(last != current){
    delay(10);
    
    current=digitalRead(RB);
  }
  return current;
}
