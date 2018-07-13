//  Instinct Server - ThreadedServer
//  Copyright (c) 2016  Robert H. Wortham <r.h.wortham@gmail.com>
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

package instinct;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

// provides support for a multi-threaded tcp server
public class ThreadedEnquiryServer implements Runnable{

    private final List<PrintWriter> clients;
    protected int			serverPort;
    protected ServerSocket	serverSocket;

    public ThreadedEnquiryServer(int port, List<PrintWriter> clients){
        this.serverPort = port;
        this.clients = clients;
    }

    public void run(){

        openServerSocket();

        while(true){
            Socket clientSocket = null;
            try {
                System.out.println("Waiting for mobile connection(s).");
                clientSocket = this.serverSocket.accept();
                System.out.println("Accepted a mobile connection");

                clients.add(new PrintWriter(clientSocket.getOutputStream(), true));
            } catch (IOException e) {
                throw new RuntimeException("Error accepting client connection", e);
            }

        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
    }
}

