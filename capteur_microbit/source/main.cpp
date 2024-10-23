

#include "MicroBit.h"
#include "ssd1306.h"
#include "tsl256x.h"
#include "veml6070.h"
#include "bme280.h"

MicroBitI2C i2c(I2C_SDA0,I2C_SCL0);
MicroBit uBit;
MicroBitPin P0(MICROBIT_ID_IO_P0, MICROBIT_PIN_P0, PIN_CAPABILITY_DIGITAL_OUT);
ssd1306 screen(&uBit, &i2c, &P0);


tsl256x tsl(&uBit,&i2c);
veml6070 veml(&uBit,&i2c);
bme280 bme(&uBit,&i2c);

uint16_t comb =0;
uint16_t ir = 0;
uint32_t lux = 0;
uint16_t uv = 0;
uint32_t pressure = 0;
int32_t temp = 0;
uint16_t humidite = 0;

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


void ecran(){

    uint32_t* values = getValues();

    char bufferLux[20];
    sprintf(bufferLux, "Lux: %lu", values[1]);
    screen.display_line(0,0,bufferLux);

    char bufferUV[20];
    sprintf(bufferUV, "UV: %lu", values[2]);
    screen.display_line(1,0,bufferUV);

    char bufferIR[20];
    sprintf(bufferIR, "IR: %lu", values[0]);
    screen.display_line(2,0,bufferIR);

    char bufferPressure[20];
    sprintf(bufferPressure, "Pressure: %lu hPa", values[3]);
    screen.display_line(3,0,bufferPressure);

    char bufferTemp[20];
    sprintf(bufferTemp, "Temp: %lu.%lu C", values[4]/100, values[4]%100);
    screen.display_line(4,0,bufferTemp);

    char bufferHumidite[20];
    sprintf(bufferHumidite, "Humidite: %lu.%lu rH", values[5]/100, values[5]%100);
    screen.display_line(5,0,bufferHumidite);

    screen.update_screen();

    //---------------Radio----------------
    char bufferRadio[120];
    sprintf(bufferRadio, "%lu;%lu;%lu;%lu;%lu.%lu;%lu.%lu", values[0], values[1], values[2], values[3], values[4]/100, values[4]%100, values[5]/100, values[5]%100);
    uBit.radio.datagram.send(bufferRadio);
    //------------------------------------
    free(values);
    uBit.sleep(1000);
    
}

int main() {
    uBit.init();
    uBit.radio.enable();
    uBit.radio.setGroup(14);
    while(true)
    {
        ecran();
    }
    release_fiber();
}

