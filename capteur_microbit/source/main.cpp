

#include "MicroBit.h"
#include "ssd1306.h"
#include "tsl256x.h"
#include "veml6070.h"
#include "bme280.h"

char* PROTOCOL="s85h7hdf";

//Microbit
MicroBitI2C i2c(I2C_SDA0,I2C_SCL0);
MicroBit uBit;
MicroBitPin P0(MICROBIT_ID_IO_P0, MICROBIT_PIN_P0, PIN_CAPABILITY_DIGITAL_OUT);
ssd1306 screen(&uBit, &i2c, &P0);

//Capteurs
tsl256x tsl(&uBit,&i2c);
veml6070 veml(&uBit,&i2c);
bme280 bme(&uBit,&i2c);

//Valeurs
uint16_t comb =0;
uint16_t ir = 0;
uint32_t lux = 0;
uint16_t uv = 0;
uint32_t pressure = 0;
int32_t temp = 0;
uint16_t humidite = 0;

//Ordre
int order[6] = {0,1,2,3,4,5};

char* id;

char* generateRandomString(int length){ 
    int randomNumber = uBit.random(900)+100;
    static char idBuffer[4];
    sprintf(idBuffer, "%d", randomNumber);
    return idBuffer;
}

/*
Prend les valeurs retourné par les 3 capteurs
Fait un traitement sur les données retourné par le capteur bme
malloc et memcopy pour garder le pointeur de la liste (sinon dès qu'on va quitter la fonction, la place mémoire de la liste sera suppr)
*/
uint32_t* getValues()
{
    tsl.sensor_read(&comb, &ir, &lux);
    veml.sensor_read(&uv);
    bme.sensor_read(&pressure, &temp, &humidite);
    uint32_t vals_values[6]  = {ir,lux,uv,bme.compensate_pressure(pressure)/100,static_cast<uint32_t>(bme.compensate_temperature(temp)),bme.compensate_humidity(humidite)};
    uint32_t* vals = (uint32_t*)malloc(6*sizeof(uint32_t));
    memcpy(vals, vals_values, 6*sizeof(u_int32_t));
    return vals;
}

/*
Ecoute sur la liason radio
Regarde si il y a notre protocole: check "Protocole"
Eneleve "protocole" puis met dans order la valeurs (si ce n'est pas un ";")
Efface l'ecran
*/
void onData(MicroBitEvent)
{
    ManagedString buffer = uBit.radio.datagram.recv();
    if(buffer.length()>0){
        uBit.serial.send(buffer+"\n");
        if(buffer.substring(0,8)==PROTOCOL && buffer.substring(8,3)==id)
        {
            int count = 0;
            for(int i = 0; i < buffer.length()-11; i++){
                if(buffer.substring(11,buffer.length()-1).charAt(i) != ';')
                {
                    order[count] = buffer.substring(11,buffer.length()-1).charAt(i)-'0';
                    uBit.serial.send(buffer.substring(11,buffer.length()-1).charAt(i));
                    count++;
                }
            }
            screen.clear();
        }
        uBit.serial.send("\n");
    }
}

/*
Prend les valeurs
Affiche les valeurs dans l'odre de la liste "order"
Envoie par radio les valeurs
free le tableau
*/
void ecran(){

    uint32_t* values = getValues();

    char bufferLux[20];
    sprintf(bufferLux, "Lux: %lu", values[1]);
    screen.display_line(order[0],0,bufferLux);

    char bufferUV[20];
    sprintf(bufferUV, "UV: %lu", values[2]);
    screen.display_line(order[1],0,bufferUV);

    char bufferIR[20];
    sprintf(bufferIR, "IR: %lu", values[0]);
    screen.display_line(order[2],0,bufferIR);

    char bufferPressure[20];
    sprintf(bufferPressure, "Press: %lu hPa", values[3]);
    screen.display_line(order[3],0,bufferPressure);

    char bufferTemp[20];
    sprintf(bufferTemp, "Temp: %lu.%lu C", values[4]/100, values[4]%100);
    screen.display_line(order[4],0,bufferTemp);

    char bufferHumidite[20];
    sprintf(bufferHumidite, "Hum: %lu.%lu rH", values[5]/100, values[5]%100);
    screen.display_line(order[5],0,bufferHumidite);

    char bufferId[20];
    sprintf(bufferId, "Id: %s", id);
    screen.display_line(6,0,bufferId);

    screen.update_screen();

    //---------------Radio----------------
    char bufferRadio[300];
    sprintf(bufferRadio, "%s%s;%lu;%lu;%lu;%lu;%lu.%lu;%lu.%lu", PROTOCOL, id, values[1], values[2], values[0], values[3], values[4]/100, values[4]%100, values[5]/100, values[5]%100);
    uBit.radio.datagram.send(bufferRadio);
    //------------------------------------

    free(values);
    uBit.sleep(1000);
    
}

int main() {
    uBit.init();
    uBit.radio.enable();
    uBit.radio.setGroup(14);
    uBit.messageBus.listen(MICROBIT_ID_RADIO, MICROBIT_RADIO_EVT_DATAGRAM, onData);

    id = generateRandomString(4);
    while(true)
    {
        ecran();
    }
    release_fiber();
}

