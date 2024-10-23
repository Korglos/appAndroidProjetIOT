/*
The MIT License (MIT)

Copyright (c) 2016 British Broadcasting Corporation.
This software is provided by Lancaster University by arrangement with the BBC.

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
*/

#include "MicroBit.h"

MicroBit uBit;
ManagedString empty = "";

void sendRadioMessage(){
    uBit.radio.enable();

    if (uBit.buttonA.isPressed())
        uBit.radio.datagram.send("1");

    else if (uBit.buttonB.isPressed())
        uBit.radio.datagram.send("2");

    uBit.sleep(100);
}

void onData(MicroBitEvent e)
{
    ManagedString data = uBit.radio.datagram.recv();
    if(data.substring(0, 8) == "Protocol"){
        uBit.serial.send("T" + data+"\r\n");
    }
}

void receiveRadioMessage(){
    uBit.radio.enable();
    uBit.radio.setGroup(14);
    uBit.messageBus.listen(MICROBIT_ID_RADIO, MICROBIT_RADIO_EVT_DATAGRAM, onData);

}

void liaisonSerie() {

    // Affiche un message pour indiquer que le programme a démarré

    // Lire les valeurs de l'accéléromètre
    int x = uBit.accelerometer.getX();
    int y = uBit.accelerometer.getY();
    int z = uBit.accelerometer.getZ();

    // Lire la température
    int temperature = uBit.thermometer.getTemperature();

    // Lire les valeurs de la boussole
    int heading = uBit.compass.heading();

    // Formater les données des capteurs pour l'envoi
    ManagedString data = "Accelerometre X: " + ManagedString(x) + " Y: " + ManagedString(y) + " Z: " + ManagedString(z) + "\r\n";
    data = data + "Temperature: " + ManagedString(temperature) + " C\r\n";
    data = data + "Boussole: " + ManagedString(heading) + " degres\r\n";

    // Envoyer les données via USB (interface série)
    uBit.serial.send(data);

    // Attendre une seconde avant de lire les données à nouveau
    uBit.sleep(1000);
}

void receiveSerialMessage(){
    if(uBit.serial.isReadable()){
        ManagedString data = uBit.serial.readUntil(ManagedString ("\r\n"));
        uBit.display.scroll(data);
    }
}

int main() {
    // Initialisation
    uBit.init();
    receiveRadioMessage();
    // receiveSerialMessage();
    // liaisonSerie();
    // sendRadioMessage();
    release_fiber();
}
