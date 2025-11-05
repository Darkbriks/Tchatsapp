/*
 * Copyright (c) 2025.  Jerome David. Univ. Grenoble Alpes.
 * This file is part of TchatsApp.
 *
 * TchatsApp is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * TchatsApp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with TchatsApp. If not, see <https://www.gnu.org/licenses/>.
 */

package fr.uga.im2ag.m1info.chatservice.client;

import fr.uga.im2ag.m1info.chatservice.common.Packet;

import java.io.IOException;
import java.util.Scanner;

public class Client2 {

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Votre id ? (0 pour en créer un nouveau)");
        int clientId =  sc.nextInt();


        Client c = new Client(clientId);
        if (c.connect("localhost",1666)) {

            clientId = c.getClientId();
            System.out.println("Vous êtes connecté avec l'id " + clientId);
            c.setPacketProcessor(msg -> {
                byte[] b = new byte[msg.getPayload().capacity()];
                msg.getPayload().get(b);
                System.out.println("Message from " + msg.from() + " to " + msg.to() + " : " + new String(b));
            });

            Packet m = Packet.createTextMessage(48, 2, "coucou 2 comment vas tu ?");

            c.sendPacket(m);

            while (true) {
                System.out.println("A qui envoyer ? (0 pour quitter)");
                int to = sc.nextInt();sc.nextLine();
                if (to==0) break;
                System.out.println("Votre message :");
                String msg = sc.nextLine();
                c.sendPacket(Packet.createTextMessage(c.getClientId(), to, msg));
            }
            c.disconnect();
            System.exit(0);
        }



    }
}