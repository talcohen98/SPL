package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;
import bgu.spl.net.srv.StompMessagingProtocolImpl;

public class StompServer {

    /**
     * @param args
     */
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]); //port
        String serverType = args[1]; //reactor or tpc

        if(serverType.equals("tpc")){
            Server<String> server = Server.threadPerClient(
             port,
            () -> new StompMessagingProtocolImpl(), //protocol factory
            () -> new StompMessageEncoderDecoder() //message encoder decoder factory
            );
            server.serve();
        }
        else{ //server = reactor
            Server<String> server = Server.reactor(
            Runtime.getRuntime().availableProcessors(),
             port,
            () -> new StompMessagingProtocolImpl(), //protocol factory
            () -> new StompMessageEncoderDecoder() //message encoder decoder factory
            );
            server.serve();
        }
     }
}
