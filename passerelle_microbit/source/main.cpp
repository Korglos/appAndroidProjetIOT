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
// Clé de 16 octets (128 bits)
uint8_t key[16] = { 0x2b, 0x7e, 0x15, 0x16, 0x28, 0xae, 0xd2, 0xa6, 
                    0xab, 0xf7, 0x65, 0x77, 0xcd, 0xe4, 0x32, 0x55 };
size_t bufferSize;

void convertManagedStringToUint8(ManagedString mStr, uint8_t* buffer) {
    // Convertir ManagedString en char*
    const char* charArray = mStr.toCharArray();
    
    size_t length = (size_t)(mStr.length()) < bufferSize ? mStr.length() : bufferSize;
    
    // Copier les données dans le tableau uint8_t
    for (size_t i = 0; i < length; i++) {
        buffer[i] = (uint8_t)charArray[i];
    }
}

ManagedString convertTabuitToManagedString(uint8_t *uint8){
    char charArray[bufferSize + 1];  
    for (size_t i = 0; i < bufferSize; i++) {
        charArray[i] = (char)uint8[i];  // Convertir uint8_t en char
    }
    charArray[bufferSize] = '\0';  // Ajouter le caractère null pour la fin de la chaîne
    return ManagedString(charArray);

}

void decryptAES(ManagedString data, uint8_t* decrypted) {
    struct AES_ctx ctx;
    AES_init_ctx(&ctx, key);

    uint8_t ciphertext[bufferSize];
    convertManagedStringToUint8(data, ciphertext);

    memcpy(decrypted, ciphertext, bufferSize);  // Copier le texte chiffré
    AES_ECB_decrypt(&ctx, decrypted);   // Déchiffrer
}

void encryptAES(ManagedString data, uint8_t *ciphertext) {       
    uint8_t buffer[bufferSize];
    
    convertManagedStringToUint8(data, buffer);
    
    // Initialiser AES
    struct AES_ctx ctx;
    AES_init_ctx(&ctx, key);

    // Copier le texte clair dans le buffer du texte chiffré
    memcpy(ciphertext, buffer, bufferSize);

    // Chiffrer le texte (AES-128 ECB)
    AES_ECB_encrypt(&ctx, ciphertext);
}

void sendRadioMessage(ManagedString data){
    
    bufferSize = data.length();

    uint8_t ciphertext[bufferSize];
    encryptAES(data, ciphertext);

    uBit.radio.datagram.send(CLEPROTOCOL + convertTabuitToManagedString(ciphertext));
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
        bufferSize = data.length()-8;

        uint8_t dechiffre[bufferSize];
        
        decryptAES(realData, dechiffre);

        uBit.serial.send(convertTabuitToManagedString(dechiffre)+"\r\n");
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