package de.hofalab.hofalapp;

import java.net.Socket;

/**
 * Created by piet on 24.07.19.
 */

class SocketHandler {
    private static Socket socket;

    static synchronized Socket getSocket(){
        return socket;
    }

    static synchronized void setSocket(Socket socket){
        SocketHandler.socket = socket;
    }
}