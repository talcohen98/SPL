package bgu.spl.net.srv;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.api.StompMessagingProtocol;


public class StompMessagingProtocolImpl implements StompMessagingProtocol<String> {

    private int connectionId;
    private Connections<String> connections;
    private boolean sendError;
    private String errorMsg;
    private boolean shouldTerminate = false;
    private boolean foundReceipt = false;
    private String receiptId;
    private String username;
    private String password;
    private LinkedList<User> users;
    private String topic; //channel
    private String destination; //line in message of destination
    private int messageId;
    private String messageToAllClientsSubscribesToChannel;
    private String subscriptionId;

    public StompMessagingProtocolImpl() {
        sendError = false;
        errorMsg = "";
        receiptId = "";
        username = "";
        password = "";
        topic = "";
        destination = "";
        messageId = 0;
        messageToAllClientsSubscribesToChannel = "";
        subscriptionId = "";
    }

    public void start(int connectionId, Connections<String> connections){
        this.connectionId = connectionId;
        this.connections = connections;
    }

    public void process(String message){    
        //System.out.println("the message: " + message); // ADDED
        String[] splitedMessage = message.split("\n");

        //find which command we got from client
        if(splitedMessage[0].equals("CONNECT"))
          FrameConnect(splitedMessage, message);
        else if(splitedMessage[0].equals("SEND"))
           FrameSend(splitedMessage, message);
        else if(splitedMessage[0].equals("SUBSCRIBE"))
            FrameSubscribe(splitedMessage, message);
        else if(splitedMessage[0].equals("UNSUBSCRIBE"))
            FrameUnsubscribe(splitedMessage, message);
        else if(splitedMessage[0].equals("DISCONNECT"))
            FrameDisconncet(splitedMessage, message);

        else { //if the command isn't legal
            sendError = true;
            errorMsg = "Illegal command in frame";
            connections.send(connectionId, errorFrame(errorMsg, message));
            connections.disconnect(connectionId);
            // shouldTerminate = true;
        }
    }

    //--------------------start first frame--------------------------------------

    private void FrameConnect(String[] splitedMessage, String message){
        //check that the frame is legal
        boolean found = false;
        try{
            for(int i = 1; i <= 4 & !found; i++) { //check if first header exists
                if(splitedMessage[i].contains("accept-version:1.2")) {
                    found = true;
                }
            }
        } catch(ArrayIndexOutOfBoundsException e){
            if(!found) { //header doesnt exist
                sendError = true;
                errorMsg = "Did not contain a 'accept-version' header";
            }
        }

        if(!found) { //header doesnt exist
            sendError = true;
            errorMsg = "Did not contain a 'accept-version' header";
        }

        if(!sendError) { //only if error wasnt already found
            found = false;
            try{
                for(int i = 1; i <= 4 & !found; i++) { //check if second header exists
                    if(splitedMessage[i].contains("host:stomp.cs.bgu.ac.il")) {
                        found = true;
                    }
                }
            } catch(ArrayIndexOutOfBoundsException e){
                if(!found) { //header doesnt exist
                    sendError = true;
                    errorMsg = "Did not contain a 'host' header";
                }
            }

            if(!found) { //header doesnt exist
                sendError = true;
                errorMsg = "Did not contain a 'host' header";
            }
        }

        if(!sendError) { //only if error wasnt already found
            found = false;
            try{
                for(int i = 1; i <= 4 & !found; i++) { //check if third header exists
                    if(splitedMessage[i].contains("login:")) {
                        String[] findUserName = splitedMessage[i].split(":");
                        if(findUserName.length > 1){
                            found = true;
                            //header login exists
                            username = findUserName[1];                           
                        }
                        else {
                            sendError = true;
                            errorMsg = "Did not contain a 'login' header";
                        }
                    }
                }
            }
            catch(ArrayIndexOutOfBoundsException e){
                if(!found) { //header doesnt exist
                    sendError = true;
                    errorMsg = "Did not contain a 'login' header";
                }
            }

            if(!found) { //header doesnt exist
                sendError = true;
                errorMsg = "Did not contain a 'login' header";
            }
        }

        if(!sendError) { //only if error wasnt already found
            found = false;
            try {
                for(int i = 1; i <= 4 & !found; i++) { //check if fourth header exists
                    if(splitedMessage[i].contains("passcode:")) {
                        String[] findPassword = splitedMessage[i].split(":");
                        if(findPassword.length > 1){
                            found = true;
                            //header passcode exists
                            password = findPassword[1];
                        }
                    }
                }
            } catch(ArrayIndexOutOfBoundsException e) {
                if(!found) { //header doesnt exist
                    sendError = true;
                    errorMsg = "Did not contain a 'passcode' header";
                }
            }

            if(!found) { //header doesnt exist
                sendError = true;
                errorMsg = "Did not contain a 'passcode' header";
            }
        }

        if(!sendError) { //only if error wasnt already found
            //all headers are legal
            boolean foundUser = false; 
            users = connections.getUsers();
            if(users != null) {
                for(User user : users) {
                    if(!foundUser) {
                        if(user.getUsername().equals(username)) { //username exists
                            foundUser = true;
                            if(user.getPassword().equals(password)) { //password matches
                                if(!user.getConnection()) { //user not connected
                                    user.setConnection(true);
                                }
                                else { //user already connected
                                    sendError = true;
                                    errorMsg = "User already logged in";
                                    break; // ADDED
                                }
                            }
                            else { //password doesnt match
                                sendError = true;
                                errorMsg = "Wrong password";
                                break; // ADDED
                            }
                        }
                    }                    
                }
            } 
            if(!foundUser) { //user doesn't exist- create new user
                User newUser = new User(username, password);
                connections.addUser(newUser);
                newUser.setConnection(true);
                connections.addClientToMap(connectionId, newUser);
            }
        }
        if(sendError) { //if error was found
            // if(connections.send(connectionId, errorFrame(errorMsg, message))){ // ADDED instead of line below FOR TESTING ONLY
            //     System.out.println("SENT ERROR");
            // }
            
            connections.send(connectionId, errorFrame(errorMsg, message));
            connections.disconnect(connectionId);
            // shouldTerminate = true;
        }


        //the frame connect is legal- send connection frame to client
        else {
            connections.send(connectionId, connectedFrame());
        }
    }

    //response to client after connection
    private String connectedFrame(){
        String connectedFrame = "CONNECTED";
        connectedFrame += "\n" + "version:1.2";
        connectedFrame += "\n\n" + "";
        connectedFrame += "\u0000"; 
        return connectedFrame;
    }

    //--------------------end first frame--------------------------------------

    //--------------------start second frame-----------------------------------

    private void FrameSend(String[] splitedMessage, String message){
        //check that the frame is legal
        boolean found = false;
        int numOfHeaders = 2;

        if(splitedMessage.length - 1 >= numOfHeaders) {
            if(!splitedMessage[1].contains("destination:")) { //the header 'destination' exists
                sendError = true;
                errorMsg = "The frame doesnt contain a 'destination' header";
            }
            else {
                String[] findDestination = splitedMessage[1].split(":");
                if(findDestination.length > 1){
                    found = true;
                    String[] findTopic = splitedMessage[1].split("/");
                    topic = findTopic[1];
                    destination = splitedMessage[1];
                }
            }
        }
        else {
            sendError = true;
            errorMsg = "The frame doesnt contain all headers";
        }

        if(found) {
            if(!splitedMessage[2].equals("")){
                sendError = true;
                errorMsg = "The frame doesnt contain an empty line";
            }
        }
        else {
            sendError = true;
            errorMsg = "The frame doesnt contain a 'destination' header";
        }

        if(!sendError) { //only if error wasn't already found
        //all headers are legal
        //check if client is subscribed to the topic
           ConcurrentHashMap<Integer, Integer > clientsSubscribedToChannel = connections.getClientsThatSubscribedToTheChannel(topic);
           if(clientsSubscribedToChannel == null || !clientsSubscribedToChannel.containsKey(connectionId)) {
               sendError = true;
               errorMsg = "Client is not subscribed to this topic";
           }
        }

        if(sendError) { //if an error  was found
            connections.send(connectionId, errorFrame(errorMsg, message));
            //connections.disconnect(connectionId);
            // shouldTerminate = true;
        }

        //the frame send is legal- send message frame to all clients that are subscribed to channel
        else {
            messageToAllClientsSubscribesToChannel = String.join("\n", Arrays.copyOfRange(splitedMessage, 4, splitedMessage.length ));
            subscriptionId = (connections.getClientsSubscriptionId(topic, connectionId)).toString();
            connections.send(connectionId, MessageFrame());

            //send message to all clients subscribed to the topic
            ConcurrentHashMap<Integer, Integer > clientsSubscribedToChannel = connections.getClientsThatSubscribedToTheChannel(topic);
            Set<Integer> keys = clientsSubscribedToChannel.keySet();
            for(Integer client : keys) {
                connections.send(client, MessageFrame());
            }       
        }
    }

    //response to client after send
    private String MessageFrame(){
        String MessageFrame = "MESSAGE";
        MessageFrame += "\n" + "subscription:" + subscriptionId;
        MessageFrame += "\n" + "message-id:" + messageId;
        MessageFrame += "\n" + destination;
        MessageFrame += "\n" + "";
        MessageFrame += "\n" + messageToAllClientsSubscribesToChannel + "\n\n";
        MessageFrame += "\u0000";

        messageId++;
        return MessageFrame;
    }

    //--------------------end second frame-----------------------------------

    //--------------------start third frame-----------------------------------

    private void FrameSubscribe(String[] splitedMessage, String message){
        //check that the frame is legal
        boolean found = false;
        try{
            for(int i = 1; i <= 3 & !found; i++) { //check if first header exists
                if(splitedMessage[i].contains("destination:")) {
                    found = true;
                    //the header 'destination' exists
                    String[] findDestination = splitedMessage[i].split(":");
                    if(findDestination.length > 1) {
                        String[] findTopic = splitedMessage[i].split("/");
                        topic = findTopic[1];
                        destination = splitedMessage[i];
                        
                    }
                    else
                        found = false;
                  }
            }

        } catch(ArrayIndexOutOfBoundsException e) {
            if(!found) { //header doesnt exist
                sendError = true;
                errorMsg = "Did not contain a 'destination' header";
            }
        }

        if(!found) { //header doesnt exist
            sendError = true;
            errorMsg = "Did not contain a 'destination' header";
        }
            
        if(!sendError) { //only if error wasnt already found
            found = false;
            try{
                for(int i = 1; i <= 3 & !found; i++) { //check if second header exists
                    if(splitedMessage[i].contains("id:")) { //this header is actually the subcription id
                        found = true;           
                        String[] findId = splitedMessage[i].split(":");
                        if(findId.length > 1) {        
                            //the header 'id' exists
                            String[] splittedSub = splitedMessage[i].split(":");
                            subscriptionId = splittedSub[1];
                        }
                        else
                            found = false;
                    }
                }
            } catch(ArrayIndexOutOfBoundsException e) {
                if(!found) { //header doesnt exist
                    sendError = true;
                    errorMsg = "Did not contain a 'id' header";
                }
            }
      
            if(!found) { //header doesnt exist
                sendError = true;
                errorMsg = "Did not contain a 'id' header";
            }
        }

        if(!sendError) { //only if error wasnt already found
            found = false;
            try{
                for(int i = 1; i <= 3 & !found; i++) { //check if third header exists
                    if(splitedMessage[i].contains("receipt:")) {
                        found = true;
                        foundReceipt = true;
                        receiptId = getReceiptId(splitedMessage, 4);
                        if(receiptId.equals("-1")) {
                            found = false;
                            foundReceipt = false;
                        }
                            
                    }
                }
            } catch(ArrayIndexOutOfBoundsException e) {
                if(!found) { //header doesnt exist
                    sendError = true;
                    errorMsg = "Did not contain a 'receipt' header";
                }
            }

            if(!found) { //header doesnt exist
                sendError = true;
                errorMsg = "Did not contain a 'receipt' header";
            }
        }

        if(sendError) { //if an error  was found
            connections.send(connectionId, errorFrame(errorMsg, message));
            //connections.disconnect(connectionId);
            // shouldTerminate = true;
        }

        //the frame subscribe is legal- send receipt frame to client
        else {
            
            int subscribtionIdNumber = Integer.parseInt(subscriptionId);

            //add the channel to the clients hash map of channels it subscribed to
            connections.addChannelToClientsSubscription(connectionId, topic, subscribtionIdNumber);

            //add the client to the channels hash map of clients that subscribed to this channel
            connections.addClientToChannelsSubscribedClients(topic, connectionId, subscribtionIdNumber);

            //response to client
            connections.send(connectionId, ReceiptFrame());
        }
    }

    //--------------------end third frame-----------------------------------

    //--------------------start fourth frame-----------------------------------

    private void FrameUnsubscribe(String[] splitedMessage, String message){

        //check that the frame is legal

        boolean found = false;
        try{
            for(int i = 1; i <= 2 & !found; i++) { //check if the first header exists
                if(splitedMessage[i].contains("id:")) { 
                 //find subscription id from header
                    found = true;
                    String[] findId = splitedMessage[i].split(":");
                    if(findId.length > 1) {

                        subscriptionId = findId[1];
                    }
                    else
                        found = false;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            if(!found) {
                sendError = true;
                errorMsg = "The frame doesnt contain a 'id' header";
            }
        }

        if(!found) {
            sendError = true;
            errorMsg = "The frame doesnt contain a 'id' header";
        }

        if(!sendError) { //only if error wasnt already found
            found = false;
            try{
                for(int i = 1; i <= 2 & !found; i++) { //check if the second header exists
                    if(splitedMessage[i].contains("receipt:")) {
                        found = true;
                        foundReceipt = true;
                        receiptId = getReceiptId(splitedMessage, 2);
                        if(receiptId.equals("-1")) {
                            found = false;
                            foundReceipt = false;
                        }      
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e){
                if(!found) { //header doesnt exist
                    sendError = true;
                    errorMsg = "Did not contain a 'receipt' header";
                }
            }
 
            if(!found) { //header doesnt exist
                sendError = true;
                errorMsg = "Did not contain a 'receipt' header";
            }
        }

        if(!sendError) { //only if error wasn't already found
        //all headers are legal

          //check if the client is subscribed
          ConcurrentHashMap<String, Integer> clientsSubscriptions = connections.getClientsSubscribtions(connectionId);
          if(clientsSubscriptions != null && !clientsSubscriptions.containsValue(Integer.valueOf(subscriptionId))) {
              sendError = true;
              errorMsg = "Client is not subscribed";
          }
        }

        if(sendError) { //if an error  was found
            connections.send(connectionId, errorFrame(errorMsg, message));
            connections.disconnect(connectionId);
            //shouldTerminate = true;
        }

        //the frame unsubscribe is legal- send receipt frame and unsubscribe client
        else {
            Integer subscribtionIdNumber = Integer.valueOf(subscriptionId);

            //remove the channel from the clients hash map of channels it subscribed to
            connections.removeChannelFromClientsSubscription(connectionId, topic, subscribtionIdNumber);

            //remove the client from the channels hash map of clients that subscribed to this channel
            connections.removeClientFromChannelsSubscribedClients(topic, connectionId, subscribtionIdNumber);

            //response to client
            connections.send(connectionId, ReceiptFrame());
            connections.disconnect(connectionId);
        }
    }

    //--------------------end fourth frame-----------------------------------

    //--------------------start fifth frame-----------------------------------

    private void FrameDisconncet(String[] splitedMessage, String message){
        int numOfHeaders = 1;
        if(splitedMessage.length - 1 != numOfHeaders || !splitedMessage[1].contains("receipt:")){
            sendError = true;
            errorMsg = "The frame doesn't contain a 'receipt' header";
        }
        else {
            foundReceipt = true;
            receiptId = getReceiptId(splitedMessage, 1);
            if(receiptId.equals("-1")) {
                foundReceipt = false;
                sendError = true;
                errorMsg = "The frame doesn't contain a 'receipt' header";
            }      
        }

        if(!sendError) {
            //check if the client is active
            User user = connections.getUserByConnectionId(connectionId);
            if(!user.getConnection()) {
                sendError = true;
                errorMsg = "The client isn't connected";
            }  
       }

        if(sendError) { //if an error  was found
            connections.send(connectionId, errorFrame(errorMsg, message));
            connections.disconnect(connectionId);
            // shouldTerminate = true;
        }

        //the frame disconnct is legal- send receipt frame to client and unsubscribe from all channels
        else {

            //change user to not active
            User user = connections.getUserByConnectionId(connectionId);
            user.setConnection(false);

            //unsubscribe client from all channels that the client was subscribed to
            //connections.UnsubscribeClientFromAllChannels(connectionId);

            //remove all of the subscriptions that the client had subscribed to
            connections.removeAllOfClientsSubscriptions(connectionId);

            //response to client
            connections.send(connectionId, ReceiptFrame());
        } 
    }

    //--------------------end fifth frame-----------------------------------

    //------functions relevant for more than one frame------

    //find receiptId from message received
    private String getReceiptId(String[] splitedMessage, int numberOfHeaders){
        for(int i = 1; i <= numberOfHeaders; i++) {
            if(splitedMessage[i].contains("receipt")) {
                String[] findReceipt = splitedMessage[i].split(":");
                return findReceipt[1];
            }
        }
        return "";
    }

    //response to client after subscribe/unsubscribe/disconnect
     private String ReceiptFrame(){
        String ReceiptFrame = "RECEIPT";
        ReceiptFrame += "\n" + "receipt-id:" + receiptId;
        ReceiptFrame += "\n\n" + "";
        ReceiptFrame += "\0";
        return ReceiptFrame;
    }

    //response error to client
    private String errorFrame(String errMsg, String message){
        String errorFrame = "ERROR";
        if(foundReceipt) 
           errorFrame += "\n" + "receipt-id: " + receiptId;
        errorFrame += "\n" + "message: malformed frame received ";
        errorFrame += "\n" + "";
        errorFrame += "\n" + "The message:";
        errorFrame += "\n" + "-----";
        errorFrame += "\n" + message;
        errorFrame += "\n" + "-----";
        errorFrame += "\n" + "message: " + errMsg + "\n\n";
        errorFrame += "\u0000";
        return errorFrame;
    }
    //------end of frames------

    public boolean shouldTerminate(){
        return shouldTerminate;
    }
}