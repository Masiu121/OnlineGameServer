package com.maksuu121.onlinegameserver;

import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    int maxPlayers = 5;
    int port = 2115;
    List<Player> players = new ArrayList<>();

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        log(0, "Starting server...");
        Thread thread = new Thread() {
            @Override
            public void run() {
                try(ServerSocket server = new ServerSocket(port)) {
                    log(0, "Successfully bind the port");
                    while(true) {
                        Socket socket = server.accept();
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        Scanner in = new Scanner(socket.getInputStream());
                        if(in.hasNext()) {
                            String jsonString = in.nextLine();
                            JSONObject receivedObject = new JSONObject(jsonString);
                            if(receivedObject.has("id")) {
                                int id = receivedObject.getInt("id");
                                Player player = findPlayerById(id);
                                if(player != null) {
                                    if(receivedObject.has("x") && receivedObject.has("y")) {
                                        player.x = receivedObject.getInt("x");
                                        player.y = receivedObject.getInt("y");

                                        JSONObject toSendObject = new JSONObject();
                                        toSendObject.put("players", players.size());

                                        for(Player otherPlayer : players) {
                                            if(otherPlayer != player) {
                                                JSONObject otherPlayerObject = new JSONObject();
                                                otherPlayerObject.put("x", otherPlayer.x);
                                                otherPlayerObject.put("y", otherPlayer.y);
                                                toSendObject.put(otherPlayer.id + "", otherPlayerObject);
                                                out.println(toSendObject);
                                            }
                                        }

                                        socket.close();
                                    }
                                    else {
                                        JSONObject disconnectMessage = new JSONObject();
                                        disconnectMessage.put("type", "disconnect");
                                        disconnectMessage.put("info", "Wrong message info");
                                        out.println(disconnectMessage);
                                        socket.close();
                                    }
                                }
                                else {
                                    JSONObject disconnectMessage = new JSONObject();
                                    disconnectMessage.put("type", "disconnect");
                                    disconnectMessage.put("info", "No player with that id on the server");
                                    out.println(disconnectMessage);
                                    socket.close();
                                }
                            }
                            else if(receivedObject.has("nickname")) {
                                if(players.size() < maxPlayers && findPlayerByNickname(receivedObject.getString("nickname"))==null) {
                                    int id = findId();
                                    players.add(new Player(0, 0, id, receivedObject.getString("nickname")));
                                    JSONObject connectMessage = new JSONObject();
                                    connectMessage.put("type", "connect");
                                    connectMessage.put("info", "No additional info");
                                    connectMessage.put("id", id);
                                    out.println(connectMessage);
                                    socket.close();
                                }
                            }
                            else {
                                JSONObject disconnectMessage = new JSONObject();
                                disconnectMessage.put("type", "disconnect");
                                disconnectMessage.put("info", "Wrong join message");
                                out.println(disconnectMessage);
                                socket.close();
                            }
                        }
                        else {
                            JSONObject disconnectMessage = new JSONObject();
                            disconnectMessage.put("type", "disconnect");
                            disconnectMessage.put("info", "Did not receive any data");
                            out.println(disconnectMessage);
                            socket.close();
                        }
                    }
                } catch (IOException e) {
                    log(2, "Failed to bind to the port");
                }
            }
        };

        thread.start();
    }

    public void log(int typeNumber, String message) {
        String typeString = "UNKNOWN";
        switch(typeNumber) {
            case 0:
                typeString = "INFO";
                break;
            case 1:
                typeString = "WARN";
                break;
            case 2:
                typeString = "ERROR";
                break;
        }

        System.out.println("[" + typeString + "][MAIN] " + message);
    }

    Player findPlayerByNickname(String nickname) {
         for(Player player : players) {
             if(player.nickname.equals(nickname));
                return player;
         }
         return null;
    }

    Player findPlayerById(int id) {
        for(Player player : players) {
            if(player.id == id);
            return player;
        }
        return null;
    }

    int findId() {
        for(int i = 0; i < maxPlayers; i++) {
            if(findPlayerById(i)==null) {
                return i;
            }
        }
        return -1;
    }
}
