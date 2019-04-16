import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Arrays;

/**
 * Project04 -- MP3
 * <p>MP3Client</p>
 *
 * @author Jacob Dachenhaus & Zach McClary, L17
 * @version April 12, 2019
 */

public class MP3Client {

    private Socket clientSocket;
    private ObjectOutputStream outputStream;

    public static void main(String[] args) {

        MP3Client client;

        try {
            client = new MP3Client("localhost", 3000);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        client.connect();

    }

    public MP3Client(String host, int port) throws IOException {
        clientSocket = new Socket(host, port);
        outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
    }

    public void connect() {

        System.out.println("<Connected to server>");

        ResponseListener responseListener = new ResponseListener(clientSocket);

        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.println("What would you like to do?");
            System.out.println("To see all available songs enter <list>");
            System.out.println("To request a song enter <download>");
            System.out.println("To quit enter <exit>");

            System.out.print("Choice: ");
            String choice = scanner.nextLine();

            if (choice.equalsIgnoreCase("list")) {

                SongRequest request = new SongRequest(false);

                try {
                    outputStream.writeObject(request);
                } catch (IOException e) {
                    System.out.printf("<Unexpected exception: %s>\n", e.getMessage());
                    break;
                }

            } else if (choice.equalsIgnoreCase("download")) {

                System.out.print("Song name: ");
                String songName = scanner.nextLine();

                System.out.print("Artist name: ");
                String artistName = scanner.nextLine();

                SongRequest request = new SongRequest(true, songName, artistName);

                try {
                    outputStream.writeObject(request);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

            } else if (choice.equalsIgnoreCase("exit")) {

                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;

            } else {
                System.out.println("<Invalid song request>");
                continue;
            }

            Thread thread = new Thread(responseListener);
            thread.start();

            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        System.out.println("<Disconnected from the server>");

    }

}

final class ResponseListener implements Runnable {

    private Socket clientSocket;
    private ObjectInputStream inputStream;

    public ResponseListener(Socket clientSocket) {

        this.clientSocket = clientSocket;

        try {
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {

        Object object;

        try {
            object = inputStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (object.getClass() != SongHeaderMessage.class) return;
        SongHeaderMessage response = (SongHeaderMessage) object;

        if (response.isSongHeader()) {

            int fileSize = response.getFileSize();

            if (fileSize == -1) {
                System.out.println("Invalid song request");
                return;
            }

            String fileName = String.format("%s - %s.mp3", response.getArtistName(), response.getSongName());
            File file = new File("savedSongs/" + fileName);

            try {
                if (!file.createNewFile()) {
                    System.out.println("File is already saved locally!");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            SongDataMessage message;
            int byteTotal = 0;

            while (byteTotal < fileSize) {

                try {
                    message = (SongDataMessage) inputStream.readObject();
                } catch (Exception e) {
                    System.out.printf("<Unexpected exception: %s>\n", e.getMessage());
                    continue;
                }

                byte[] data = message.getData();
                writeByteArrayToFile(data, file.getPath());

                byteTotal += data.length;

            }

            System.out.println("<Download complete>");

        } else {

            String message;

            while (true) {

                try {
                    message = (String) inputStream.readObject();
                } catch (Exception e) {
                    System.out.printf("<Unexpected exception: %s>\n", e.getMessage());
                    continue;
                }

                if (message == null) break;

                System.out.println("- " + message);

            }

        }

    }

    public void writeByteArrayToFile(byte[] data, String fileName) {

        FileOutputStream fileOutputStream;

        try {
            fileOutputStream = new FileOutputStream(fileName, true);
            fileOutputStream.write(data);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}