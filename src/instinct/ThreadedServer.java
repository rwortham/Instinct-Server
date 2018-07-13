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
import java.util.concurrent.ConcurrentLinkedDeque;

// provides support for a multi-threaded tcp server
public class ThreadedServer implements Runnable{

	private final List<PrintWriter> clients;
	protected int			serverPort   = 1024;
    protected ServerSocket	serverSocket = null;
    protected boolean		isStopped    = false;
    protected Thread		runningThread= null;
    protected TcpHandler	lastHandler = null;
    protected RobotStreamData robotIncomingString = null;

    public ThreadedServer(int port, List<PrintWriter> clients){
        this.robotIncomingString = robotIncomingString;
        this.clients = clients;
        this.serverPort = port;
    }

	public void run(){
	    synchronized(this){
	        this.runningThread = Thread.currentThread();
	    }
	    openServerSocket();
	    int nLogFileCount = 1;
	    while(! isStopped()){
	        Socket clientSocket = null;
	        try {
                System.out.println("Waiting for connection(s).");
	            clientSocket = this.serverSocket.accept();
                System.out.println("Accepted a connection");
	        } catch (IOException e) {
	            if(isStopped()) {
	                System.out.println("Server Stopped.") ;
	                return;
	            }
	            throw new RuntimeException("Error accepting client connection", e);
	        }

            Logger logger = new Logger("logs/logfile"+nLogFileCount+".txt");
            // each handler gets its own log file
            lastHandler = new TcpHandler(clientSocket, logger.getFullFileName(), "cmd/cmdfile.txt", clients);
            new Thread(lastHandler).start();
            nLogFileCount++;
	    }
	    System.out.println("Server Stopped.") ;
	}
	
	
	private synchronized boolean isStopped()
	{
	    return this.isStopped;
	}
	
	public synchronized void stop()
	{
	    this.isStopped = true;
	    try {
	        this.serverSocket.close();
	        if (this.lastHandler != null)
	        	this.lastHandler.stop();
	    } catch (IOException e) {
	        throw new RuntimeException("Error closing server", e);
	    }
	}
	
	public synchronized void sendCmd(String cmd)
	{
		if (!this.isStopped)
		{
			lastHandler.sendCmd(cmd);
		}
	}

	public synchronized void toggleLogDisplay()
	{
		if (!this.isStopped && (lastHandler != null))
		{
			lastHandler.toggleLogDisplay();
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

