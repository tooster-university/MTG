package me.tooster;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {

    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    // run with arguments <IP> <port> <nick>
    // main thread reads command line
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Pass <IP> <port> <nick> as arguments.");
            System.exit(1);
        }


        try (Socket socket = new Socket(args[0], Integer.parseInt(args[1]))) {
            new Thread(new Receiver(socket)).start(); // create reader from server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(args[2]); // send nick to server as config data
            out.flush();
            Scanner scanner = new Scanner(System.in);
            String s;
            while (!socket.isClosed()) // skip blank lines
                if (!(s = scanner.nextLine()).isEmpty())
                    out.println(s);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Displays output to the screen on separate thread
     */
    static class Receiver implements Runnable {

        Socket socket;
        BufferedReader in;

        Receiver(Socket sockin) throws IOException {

            socket = sockin;
            in = new BufferedReader(new InputStreamReader(sockin.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String s;
                while ((s = in.readLine()) != null)
                    System.out.println(s); // display server reply

                socket.close();

                LOGGER.info("Client disconnected.");
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}


