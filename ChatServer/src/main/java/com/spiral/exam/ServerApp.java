package com.spiral.exam;

import java.io.*;
import java.net.*;
import javax.swing.*;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.spiral.exam.actors.ChannelHandlerActor;
import com.spiral.exam.actors.ClientHandlerActor;

public class ServerApp extends JFrame {
    private ServerSocket server;
    private Socket socket;

    private ActorSystem actorSystem;
    public static ActorRef clientHandlerActorRef;
    public static ActorRef channelHandlerActorRef;

    private JTextArea messageWindow;

    public ServerApp() {
        super("SpiralChat Server");
        messageWindow = new JTextArea();
        messageWindow.setEditable(false);
        messageWindow.setText("Server is running...");
        add(new JScrollPane(messageWindow));
        setSize(300, 60);
        setVisible(true);
    }

    public void runServer() {
        try {
            server = new ServerSocket(8787, 50);
            actorSystem = ActorSystem.create("server-actor-system");
            clientHandlerActorRef  = actorSystem.actorOf(ClientHandlerActor.props(), "client-handler-actor");
            channelHandlerActorRef = actorSystem.actorOf(ChannelHandlerActor.props(), "channel-handler-actor");

            channelHandlerActorRef.tell(new ChannelHandlerActor.Channel("GENERAL"), ActorRef.noSender());
            channelHandlerActorRef.tell(new ChannelHandlerActor.Channel("FUN"), ActorRef.noSender());
            channelHandlerActorRef.tell(new ChannelHandlerActor.Channel("WORK"), ActorRef.noSender());

            Thread clientAcceptThread = new Thread() {
                @Override
                public void run() {
                    while(true) {
                        try {
                            socket = server.accept();
                            ClientHandlerActor.Client client = new ClientHandlerActor.Client(socket);
                            clientHandlerActorRef.tell(client, ActorRef.noSender());
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            clientAcceptThread.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ServerApp server = new ServerApp();
        server.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        server.runServer();
    }
}