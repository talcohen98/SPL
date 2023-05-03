#include <iostream>
#include <stdlib.h>
#include "../include/ConnectionHandler.h"
#include <string>
#include <queue>
#include <thread>
#include <SocketReader.h>

using namespace std;

int main(int argc, char *argv[]) {
	// TODO: implement the STOMP client
	if (argc < 3) { // reading first 3 args - fileName, hostIp, port
    	std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
	std::string host = argv[1];
    short port = atoi(argv[2]);
    
    ConnectionHandler connectionHandler(host, port); // creating connection Handler for client
    if (!connectionHandler.connect()) { // if connection failed client die
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    std::queue<string> messageQueue;
    std::mutex monitor;
    std::condition_variable condVar;
    std::unique_lock<mutex> lock(monitor);
    SocketReader socketReader(connectionHandler,messageQueue,lock,condVar);
    std::thread socketReaderThread(&SocketReader::readMessages, &socketReader); // run thread for handling socket

    while(!socketReader.getShouldTerminate()){
        const short bufSize = 1024;
        char buffer[bufSize]; // creating new buffer to read to
        cin.getline(buffer, bufSize); // getting line from user
        string line(buffer);
        messageQueue.push(line); // adding the line to the queue of lines
        condVar.notify_all();
    
        condVar.wait(lock);
        cout.flush();
    }

    socketReaderThread.join();
    return 0;
}