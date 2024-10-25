# Program to control passerelle between Android application
# and micro-controller through USB tty
import time
import argparse
import signal
import sys
import socket
import socketserver
import serial
import threading

HOST           = "0.0.0.0"
UDP_PORT       = 10000
MICRO_COMMANDS = ["TL" , "LT"]
JSON_KEYS       = ["id", "lux", "uv", "ir", "pressure", "temp", "humidite"]
KCRYPT          = "2b7e151628aed2a6abf7158809cf4f3c"
FILENAME        = "values.json"
LAST_VALUE      = ""
ORDER           = ""

class ThreadedUDPRequestHandler(socketserver.BaseRequestHandler):

    def handle(self):
        data = self.request[0].strip().decode()
        socket = self.request[1]
        current_thread = threading.current_thread()
        print("{}: client: {}, wrote: {}".format(current_thread.name, self.client_address, data))
        if data != "":
                        if data in MICRO_COMMANDS: # Send message through UART
                                sendUARTMessage(data)
                        elif ";" in data: 
                                data = data + "\r\n"
                                ORDER = data.encode()
                                sendUARTMessage(ORDER)
                        elif data == "getValues()": # Sent last value received from micro-controller
                                print("J'envoie les valeurs : ", LAST_VALUE)
                                socket.sendto(LAST_VALUE.encode(), self.client_address) 
                                # TODO: Create last_values_received as global variable      
                        else:
                                print("Unknown message: ",data)

class ThreadedUDPServer(socketserver.ThreadingMixIn, socketserver.UDPServer):
    pass


# send serial message 
SERIALPORT = "COM6"
BAUDRATE = 115200
ser = serial.Serial()

def initUART():        
        # ser = serial.Serial(SERIALPORT, BAUDRATE)
        ser.port=SERIALPORT
        ser.baudrate=BAUDRATE
        ser.bytesize = serial.EIGHTBITS #number of bits per bytes
        ser.parity = serial.PARITY_NONE #set parity check: no parity
        ser.stopbits = serial.STOPBITS_ONE #number of stop bits
        ser.timeout = None          #block read

        # ser.timeout = 0             #non-block read
        # ser.timeout = 2              #timeout block read
        ser.xonxoff = False     #disable software flow control
        ser.rtscts = False     #disable hardware (RTS/CTS) flow control
        ser.dsrdtr = False       #disable hardware (DSR/DTR) flow control
        #ser.writeTimeout = 0     #timeout for write
        print('Starting Up Serial Monitor')
        try:
                ser.open()
        except serial.SerialException:
                print("Serial {} port not available".format(SERIALPORT))
                exit()



def sendUARTMessage(msg):
    ser.write(msg)
    print("Message <" + msg.decode() + "> sent to micro-controller." )


# Main program logic follows:
if __name__ == '__main__':
        initUART()
        f= open(FILENAME,"a")
        print ('Press Ctrl-C to quit.')

        server = ThreadedUDPServer((HOST, UDP_PORT), ThreadedUDPRequestHandler)

        server_thread = threading.Thread(target=server.serve_forever)
        server_thread.daemon = True
        

        try:
                msg = ""
                server_thread.start()
                print("Server started at {} port {}".format(HOST, UDP_PORT))
                while ser.isOpen(): 
                        # time.sleep(100)
                        if ser.inWaiting() > 0:  # S'il y a des octets entrants
                                data_bytes = ser.read(ser.inWaiting())
                                print(data_bytes)
                                
                                
                                msg = msg + data_bytes.decode()
                                # Vérifier si le dernier caractère est un saut de ligne
                                if b"\n" in data_bytes:  # Utiliser b"\n" car data_bytes est en bytes
                                        # Décoder en string
                                        msg = msg[:-1]
                                        msg_arr = msg.split(";")
                                        msg_arr = [float(value) for value in msg_arr]
                                        json_struct = {}

                                        for value in JSON_KEYS : 
                                                json_struct[value] = msg_arr[JSON_KEYS.index(value)]
                                        print("JSON STRUCTURE", json_struct)
                                        json_stringify = str(json_struct)
                                        json_stringify.replace("\'", "\"")
                                        print(json_stringify)
                                        f.write(json_stringify + ",")
                                        LAST_VALUE = msg
                                        msg = ""
                                
        except (KeyboardInterrupt, SystemExit):
                server.shutdown()
                server.server_close()
                f.close()
                ser.close()
                exit()
