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
#include "aes.h"
#define CLEPROTOCOL "s85h7hdf"

MicroBit uBit;

void encryptAES() {
    // Clé de 16 octets (128 bits)
    uint8_t key[16] = { 0x2b, 0x7e, 0x15, 0x16, 0x28, 0xae, 0xd2, 0xa6, 
                        0xab, 0xf7, 0x65, 0x77, 0xcd, 0xe4, 0x32, 0x55 };
    
    // Texte clair de 16 octets (128 bits)
    uint8_t plaintext[16] = "Hello, Micro:bit!";
    
    // Buffer pour le texte chiffré
    uint8_t ciphertext[16];
    
    // Initialiser AES
    struct AES_ctx ctx;
    AES_init_ctx(&ctx, key);

    // Copier le texte clair dans le buffer du texte chiffré
    memcpy(ciphertext, plaintext, 16);

    // Chiffrer le texte (AES-128 ECB)
    AES_ECB_encrypt(&ctx, ciphertext);

    // Afficher le résultat chiffré
    for (int i = 0; i < 16; i++) {
        uBit.serial.printf("%02x ", ciphertext[i]);
    }
    uBit.serial.printf("\n");
}

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