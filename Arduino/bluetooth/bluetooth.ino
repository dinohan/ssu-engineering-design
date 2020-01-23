#include <SoftwareSerial.h>

SoftwareSerial BTSerial(6, 7);

void setup()
{
  Serial.begin(9600);
  BTSerial.begin(9600);
}

void loop()
{
  if (Serial.available())
  {
    BTSerial.write(Serial.read());
  }

  if (BTSerial.available())
  {
    Serial.write(BTSerial.read());
  }
}
