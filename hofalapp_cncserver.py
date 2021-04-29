#!/usr/bin/env python3

# HoFaLab CNC mill server
# For remote control using HoFaLapp
# Author: Piet Jarmatz
# Date: July 2019

import socket
import sys
import pyqrcode
import ipaddress
import secrets
import base64
import deskcnc_serial

# Network server for remote control of HoFaLab CNC device with HoFaLapp
class CNCServer():
    def __init__(self, ip, port, serialport):
        self._ip = ipaddress.IPv4Address(ip)
        self._port = int(port)
        if serialport == 'none':
            self._debugmode = True
            print('Debug mode without device enabled!')
        else:
            self._debugmode = False
            mill = deskcnc_serial.DeskCNC(serialport)
            self._parser = deskcnc_serial.ModalGCodeParser(mill, sys.stdin)

    def generateQR(self):
        self._token = secrets.token_bytes(nbytes=8)
        content = self._ip.packed + self._port.to_bytes(2, byteorder="little", signed=False) + self._token
        content_b64 = base64.b64encode(content)
        qrcode = pyqrcode.create(content_b64, error='H', version=None, encoding='ascii')
        print("Ready to connect to client, please scan this QR code with HoFaLapp: ")
        print(qrcode.terminal())

    def setupConnection(self, sock):
        while True:
            try:
                conn, addr = sock.accept()
                conn.settimeout(0.1) 
                token_received = conn.recv(8)
                if(self._token == token_received):
                    conn.send("ACK".encode('utf-8'))
                    self._username = conn.recv(64).decode("utf-8")
                    print("Connected with " + self._username + ". " + str(conn.getpeername()) )
                    return conn
            except OSError:
                pass
            # close connection if it exists
            if 'conn' in locals():
                conn.close()

    def acceptGCode(self, conn):
        try:
            conn.settimeout(None)
            while True:
                data = conn.recv(512)
                if self._debugmode:
                    print(data.decode("utf-8"))
                else:
                    self._parser.parseLine(data.decode("utf-8"))
                conn.send(b'\x01')
        except OSError:
            pass
        except:
            print(str(sys.exc_info()[0]))
        finally:
            print("Connection closed")
            conn.close()

    def run(self):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.bind(('', self._port))   
            s.listen()
            while True:
                self.generateQR()
                conn = self.setupConnection(s)
                self.acceptGCode(conn)
                
def main():
    if not len(sys.argv) == 4:
        print("Usage: " + sys.argv[0] + " my_ip port serial_port")
        print("(set serial_port to 'none' for debug mode without device)")
        sys.exit(1)

    print("Starting HoFaLapp CNC Server...")
    server = CNCServer(sys.argv[1], sys.argv[2], sys.argv[3])
    server.run()

if __name__ == '__main__':
    main()
