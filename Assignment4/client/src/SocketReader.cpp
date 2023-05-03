#include <../include/SocketReader.h>
#include "../src/event.cpp"
#include <string>
#include <queue>
#include <list>

using std::string;
using std::queue;
using std::mutex;
using std::unique_lock;
using std::cin;
using std::cout;
using std::endl;

SocketReader:: SocketReader(ConnectionHandler& connectionHandler, queue<string>& messageQueue,  unique_lock<mutex>& mutex, std::condition_variable& _condVar) :
    connectionHandler(connectionHandler), messageQueue(messageQueue), monitor(mutex), condVar(_condVar), shouldTerminate(false), uniqueId(0), userSubscriptions(), uniqueReceipt(1), connectedUser(""){
    }

void SocketReader::readMessages(){
    while(!getShouldTerminate()){
        while(messageQueue.empty()){ //If the queue is empty, we wait until its not.
            condVar.wait(monitor);
        }

        //we get here if we got the first input
        cout.flush();
        string line = messageQueue.front(); // reading the first message in queue
        messageQueue.pop(); 
        string preparedMessage = prepareMessage(line);
        if (preparedMessage == ""){ // if we dont recognize the command, we ignore it 
            condVar.notify_all();
            continue;
        }
        int messageSize = preparedMessage.size();
        // sending message to server
        if(!connectionHandler.sendBytes(preparedMessage.c_str(), messageSize + 1)){ // If the message isnt received fully, we terminate
            cout << "Disconnected. Exiting..." << '\n';
            shouldTerminate = true;
            condVar.notify_all();
            break;
        }
        cout.flush();
        //handling received answer from server
        string answer;
        if(!connectionHandler.getFrameAscii(answer, '\0')){
            cout << "Disconnected. Exiting..." << '\n';
            break;
        }
        if(answer == ""){
            if(!connectionHandler.getFrameAscii(answer, '\0')){
                cout << "Disconnected. Exiting..." << '\n';
                break;
            }
        }
        cout.flush();
        // cout << answer << endl;
        // ** for exit response
        string messageArr [4];
            int index = 0;
            int prev_index = 0;
            int i=0;
        while(index != -1){
            prev_index = index;
            index = line.find(' ', index);
            if(index != -1){
                messageArr[i] = line.substr(prev_index, index-prev_index);
                index++;
            }else{
                messageArr[i] = line.substr(prev_index, (int)line.size()-prev_index);
            }
            i++;
        }
        // ** END OF for exit response
        if(answer.substr(0,9) == "CONNECTED" && connectedUser == ""){// print output to user
            connectedUser =  messageArr[2]; 
            cout << "Login Successful" << endl;
        } 
            
        if (answer.find("User already logged in") != std::string::npos)   
            cout << "The client is already logged in, log out before trying again." << endl;  
        if (answer.find("Wrong password") != std::string::npos)   
            cout << "Wrong password" << endl;   
        if (answer.find("UNSUBSCRIBE") != std::string::npos)   
            cout << "Exited channel " + messageArr[1]  << endl;  
        if (answer.find("Client is not subscribed to this topic") != std::string::npos)   
            cout << "Client is not subscribed to this topic"  << endl; 
            
        cout.flush();
        condVar.notify_all();
    }
}
string SocketReader::prepareMessage(string &s) {
    //deconstructing message to array of strings
    string messageArr [4]; //arr[0] = command
    int index = 0;
    int prev_index = 0;
    int i=0;
    while(index != -1){
        prev_index = index;
        index = s.find(' ', index);
        if(index != -1){
            messageArr[i] = s.substr(prev_index, index-prev_index);
            index++;
        }else{
            messageArr[i] = s.substr(prev_index, (int)s.size()-prev_index);
        }
        i++;
    }
    string frame="";
    //acting according to the command received:
    if(messageArr[0] == "login"){ // building CONNECT frame
        //In case the login attempt is successful
            if(connectedUser == ""){
                std::map<int, std::string> subIdsToChannel;
                userSubscriptions.insert({messageArr[2] /* =username*/, subIdsToChannel});
                frame = frame + "CONNECT\n"+
                "accept-version:1.2\n"+
                "host:stomp.cs.bgu.ac.il\n"+
                "login:" + messageArr[2] + "\n"+
                "passcode:" + messageArr[3] + "\n\0";     
        }else{
            cout<< "The client is already logged in, log out before trying again." << endl;
        }
    }

    // building SUBSCRIBE frame
    bool subAlreadyExists = false;
    for(auto sub:userSubscriptions[messageArr[2] /* =username*/]){ // if we are already subscribed, we ignore the request
        if(sub.second == messageArr[1]/* =topic*/){
            subAlreadyExists = true;
            break;
        }
    }
    if(messageArr[0] == "join" && !subAlreadyExists){ 
        uniqueId++;
        userSubscriptions[messageArr[2] /* =username*/].insert({uniqueId, messageArr[1] /* = channel name*/});
        frame = frame + "SUBSCRIBE\n"+
        "destination:/" + messageArr[1] +"\n"+
        "id:" + std::to_string(uniqueId) + "\n" +
        "receipt:" + std::to_string(uniqueReceipt) + "\n\0";
        std::string str1 = std::to_string(uniqueReceipt) + "SUBSCRIBE";
        std::string str2 = messageArr[1]+ ":" + std::to_string(uniqueId);
        uniqueReceipt++;
    }
    if(messageArr[0] == "exit"){ // building UNSUBSCRIBE frame
        std::map<int,string> userSubById = userSubscriptions[messageArr[2] /* =username*/];
        int idFound;
        for(auto it = userSubById.begin(); it != userSubById.end(); it++){
            if(it->second == messageArr[1] /* = channel name*/){
                idFound = it ->first;
                userSubscriptions[messageArr[2] /* =username*/].erase(idFound);
                break;
            }
        }
        frame = frame + "UNSUBSCRIBE\n" +

        "id:" + std::to_string(idFound) + "\n"+
        "receipt:" + std::to_string(uniqueReceipt) + "\n\0";
        uniqueReceipt++;
    }
    if(messageArr[0] == "report"){
        string path = "/workspaces/SPL231-Assignment3-student-template-2/client/data/" + messageArr[1];
        names_and_events rep(parseEventsFile(path)); 
        for(Event ev : rep.events){
            string gameUpdatesStr = mapToStr(ev.get_game_updates());
            string gameUpdatesStr_teamA = mapToStr(ev.get_team_a_updates());
            string gameUpdatesStr_teamB = mapToStr(ev.get_team_b_updates());
            string newFrame = "";
            newFrame = newFrame + "SEND\n" + 
            "destination:/" + ev.get_team_a_name() + "_" + ev.get_team_b_name() + "\n\n"+
            "user:" + connectedUser + "\n"+
            "team a:" + ev.get_team_a_name() + "\n" +
            "team b:" + ev.get_team_b_name() + "\n" +
            "event name:" + ev.get_name() + "\n" +
            "time:" + std::to_string(ev.get_time()) + "\n" +

            "general game updates:\n" + gameUpdatesStr + 
            "team a updates:\n" + gameUpdatesStr_teamA + 
            "team b updates:\n" + gameUpdatesStr_teamB + 
            "description:\n" + ev.get_discription() +"\n\0";

            frame = frame + newFrame;
        }
        cout << frame << endl;
    }
    if(messageArr[0] == "summary"){
        cout << "DIDNT IMPLEMENT" << endl;
    }
    if(messageArr[0] == "logout"){ // building DISCONNECT frame
        frame = frame + "DISCONNECT\n" +
        "receipt:" + std::to_string(uniqueReceipt) + "\n\0";
        uniqueReceipt++;
        connectedUser = "";
    } 
    return frame;
}

bool SocketReader::getShouldTerminate() const {
    return shouldTerminate;
}

string SocketReader::mapToStr(std::map<std::string, std::string> map){
    string str = "";
    if(!map.empty()){
        for(auto const& x : map){
            str = str + x.first + ":" + x.second + "\n";
        }
    }
    return str;
}