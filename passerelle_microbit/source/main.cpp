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
#define CLEPROTOCOL "s85h7hdf"

MicroBit uBit;


void sendRadioMessage(ManagedString data){
    uBit.radio.datagram.send(CLEPROTOCOL + data);
}

void onData(MicroBitEvent)
{
    ManagedString data = uBit.radio.datagram.recv();
    if(data.length() < 12){
        return;
    }
    ManagedString protocol = data.substring(0, 8);
    ManagedString realData = data.substring(8, data.length()-8);
    if(protocol == CLEPROTOCOL){
        uBit.serial.send(realData+"\r\n");
    }
}

void receiveRadioMessage(){
    uBit.radio.enable();
    uBit.radio.setGroup(14);
    uBit.messageBus.listen(MICROBIT_ID_RADIO, MICROBIT_RADIO_EVT_DATAGRAM, onData);

}

void readSerialData(MicroBitEvent){
    //Lis sur le port serial jusqu'à trouver \n
    ManagedString receivedData = uBit.serial.readUntil('\n');
    //Envoie
    sendRadioMessage(receivedData.substring(0, receivedData.length()-1));//Il faut supprimer les \r\n pour ne pas envoyer trop de data par radio.
}


void receiveSerialMessage(){
    //configure le buffer serial pour à 32o
    uBit.serial.setRxBufferSize(32);
    //Définit le char de fin à \n
    uBit.serial.eventOn('\n');
    //déclare l'écoute sur le bus serial et la redirection vers readSerialData
    uBit.messageBus.listen(MICROBIT_ID_SERIAL, MICROBIT_SERIAL_EVT_DELIM_MATCH, readSerialData);
}

int main() {
    // Initialisation
    uBit.init();
    //Initialise l'écoute sur le bus radio
    receiveRadioMessage();
    //Initialise l'écoute sur le bus serial
    receiveSerialMessage();

    release_fiber();
}