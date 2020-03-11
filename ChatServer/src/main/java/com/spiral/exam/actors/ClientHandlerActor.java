package com.spiral.exam.actors;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.spiral.exam.ServerApp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandlerActor extends AbstractLoggingActor {
    private List<Client> clientList = new ArrayList<>();

    public static class Client {
        public Socket socket;
        public String nickname;
        public String channelName;

        private ObjectOutputStream output;
        private ObjectInputStream input;

        private Client _clientInstance;

        boolean isConnected;
        enum CLIENT_PHASE {
            NONE,
            CONNECT,
            ASK_NICKNAME,
            CHOOSE_CHANNEL,
            START_CHAT,
            QUIT
        }
        private CLIENT_PHASE clientPhase = CLIENT_PHASE.NONE;

        public static class Message {
            String message;
            String clientName;
            String channelName;

            public Message(String message, String clientName, String channelName) {
                this.message = message;
                this.clientName = clientName;
                this.channelName = channelName;
            }
        }

        public Client(Socket socket) throws IOException {
            this.socket = socket;
            isConnected = true;

            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            clientPhase = CLIENT_PHASE.CONNECT;
            _clientInstance = this;
        }

        public void sendMessageToClient(String message) {
            try {
                output.writeObject(message);
                output.flush();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }

        public void listenToClient() {
            System.out.println("Start listening");
            Thread listenThread = new Thread() {
                @Override
                public void run() {
                    String message = "";

                    try {
                        do {
                            message = (String) input.readObject();
                            System.out.println(message);

                            switch (clientPhase) {
                                case CHOOSE_CHANNEL:
                                {
                                    System.out.println("Choose channel.");

                                    switch (message.toUpperCase()) {
                                        case "GENERAL":
                                        case "FUN":
                                        case "WORK":
                                        {
                                            channelName = message.toUpperCase();
                                            sendMessageToClient("Connected! Please enter a nickname: ");
                                            clientPhase = CLIENT_PHASE.ASK_NICKNAME;
                                            break;
                                        }
                                        case "QUIT":
                                        {
                                            clientPhase = CLIENT_PHASE.QUIT;
                                            break;
                                        }
                                        default:
                                        {
                                            sendMessageToClient("Please enter a valid channel name.");
                                        }
                                    }
                                    break;
                                }
                                case ASK_NICKNAME:
                                {
                                    nickname = message;
                                    sendMessageToClient("-- Changed nickname to: "+nickname+" --");
                                    clientPhase = CLIENT_PHASE.START_CHAT;

                                    ServerApp.channelHandlerActorRef.tell(_clientInstance,  ActorRef.noSender());
                                    break;
                                }
                                case START_CHAT:
                                {
                                    ServerApp.channelHandlerActorRef.tell(new Message(message, nickname, channelName), ActorRef.noSender());
                                    break;
                                }
                                case QUIT: {

                                }
                            }
                        } while(!message.equalsIgnoreCase("quit"));

                        input.close();
                        output.close();
                        socket.close();
                    }
                    catch(ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            listenThread.start();
        }
    }

    public void addClient(Client client) {
        clientList.add(client);
        client.nickname = "client-"+clientList.size(); //temp nickname
        System.out.println("Client added!");

        client.clientPhase = Client.CLIENT_PHASE.CHOOSE_CHANNEL;
        client.sendMessageToClient("Please choose a channel: \nGENERAL\nFUN\nWORK");

        client.listenToClient();
    }

    public Receive createReceive() {
        final Receive receive = receiveBuilder()
                .match(Client.class, this::addClient)
                .build();

        return receive;
    }
    public static Props props() {
        return Props.create(ClientHandlerActor.class);
    }
}
