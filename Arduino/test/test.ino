#include <SoftwareSerial.h>

SoftwareSerial BT(2, 3);

#define BPIN1 4
#define BPIN2 5

int state1 = HIGH;
int state2 = HIGH;

boolean LB = 0;
boolean RB = 0;
boolean preRB = 0;
boolean preLB = 0;

unsigned long previousMillis=0;
const long delayTime = 100;

void setup() {
  Serial.begin(9600);
  BT.begin(9600);
  
  pinMode(BPIN1, INPUT);
  digitalWrite(BPIN1, HIGH);
  pinMode(BPIN2, INPUT);
  digitalWrite(BPIN2, HIGH);
}

void loop() {
  unsigned long currentMillis = millis();
  if (BT.available())
  {
    Serial.write(BT.read());
  }
  if(currentMillis - previousMillis >= delayTime){
    previousMillis = currentMillis;
    Button();
  }
  if(preRB != RB || preLB != LB){
    preRB = RB;
    preLB = LB;
    signal();
  }
}

void signal(){
  if(RB && LB){
    Serial.println("B");
    BT.write('B');
  }
  else if(RB && !LB){
    Serial.println("R");
    BT.write('R');
  }
  else if(!RB && LB){
    Serial.println("L");
    BT.write('L');
  }
  else{
    Serial.println("0");
    BT.write('0');
  }
}

void Button(){
  int val1 = digitalRead(BPIN1);
  int val2 = digitalRead(BPIN2);

  if(state1!= val1){
    //Serial.print("R");
    if(!val1){
      RB = !RB;
    }
    state1 = val1;
    //Serial.println(val1, DEC);
  }
  if(state2 != val2){
    state2 = val2;
    if(!val2){
      LB = !LB;
    }
    //Serial.print("L");
    //Serial.println(val2, DEC);
  }
}
