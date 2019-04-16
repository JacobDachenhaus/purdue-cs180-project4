import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * Project04 -- MP3
 * <p>MP3Server</p>
 *
 * @author Jacob Dachenhaus & Zach McClary, L17
 * @version April 12, 2019
 */

public class MP3Server {

    private ServerSocket serverSocket;

    public MP3Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public static void main(String[] args) {

        MP3Server server;

        try {
            server = new MP3Server(3000);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        server.listen();

    }

    public void listen() {

        System.out.println("<Starting the server>");

        while (true) {

            Socket clientSocket;

            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {

                e.printStackTrace();

                try {
                    serverSocket.close();
                } catch (IOException i) {
                    i.printStackTrace();
                }

                break;

            }

            System.out.println("<Connected to a client>");

            ClientHandler requestHandler = new ClientHandler(clientSocket);

            Thread thread = new Thread(requestHandler);
            thread.start();

        }

    }

}

final class ClientHandler implements Runnable {

    private Socket clientSocket;

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public ClientHandler(Socket clientSocket) {

        this.clientSocket = clientSocket;

        try {
            inputStream = new ObjectInputStream(this.clientSocket.getInputStream());
            outputStream = new ObjectOutputStream(this.clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run() {

        while (true) {

            SongRequest request;

            try {
                request = (SongRequest) inputStream.readObject();
            } catch (Exception e) {
                System.out.printf("<Unexpected exception: %s>\n", e.getMessage());
                break;
            }

            if (request.isDownloadRequest()) {

                String songName = request.getSongName();
                String artistName = request.getArtistName();
                String fileName = String.format("%s - %s.mp3", artistName, songName);

                byte[] data = readSongData("songDatabase/" + fileName);

                SongHeaderMessage message;

                if (fileInRecord(fileName) && data != null) {
                    message = new SongHeaderMessage(true, songName, artistName, data.length);
                } else {
                    message = new SongHeaderMessage(true, "", "", -1);
                }

                try {
                    outputStream.writeObject(message);
                } catch (IOException e) {
                    System.out.printf("<Unexpected exception: %s>\n", e.getMessage());
                    break;
                }

                if (message.getFileSize() > -1) {
                    sendByteArray(data);
                }


            } else {

                SongHeaderMessage message = new SongHeaderMessage(false);

                try {
                    outputStream.writeObject(message);
                } catch (IOException e) {
                    System.out.printf("<Unexpected exception: %s>\n", e.getMessage());
                    break;
                }

                sendRecordData();

            }

        }

    }

    public boolean fileInRecord(String fileName) {

        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader("record.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        String nextLine;

        try {
            while ((nextLine = reader.readLine()) != null) {
                if (nextLine.equalsIgnoreCase(fileName)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;

    }

    public void sendRecordData() {

        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader("record.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        String nextLine;

        try {

            while ((nextLine = reader.readLine()) != null) {

                String songName = nextLine.substring(nextLine.indexOf("-") + 2, nextLine.length() - 4);
                String artistName = nextLine.substring(0, nextLine.indexOf("-") - 1);
                String message = String.format("\"%s\" by %s", songName, artistName);

                outputStream.writeObject(message);

            }

            outputStream.writeObject(null);

        } catch (IOException e) {
            System.out.printf("<Unexpected exception: %s>\n", e.getMessage());
        }

    }

    public byte[] readSongData(String fileName) {

        FileInputStream fileInputStream;

        try {
            fileInputStream = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        File file = new File(fileName);
        byte[] data = new byte[(int) file.length()];

        try {
            fileInputStream.read(data);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return data;

    }

    public void sendByteArray(byte[] data) {

        int chunkStart = 0;
        int chunkLength;

        while (chunkStart < data.length) {

            chunkLength = chunkStart + 1000;

            if (chunkLength > data.length) {
                chunkLength = data.length;
            }

            byte[] chunk = Arrays.copyOfRange(data, chunkStart, chunkLength);
            SongDataMessage message = new SongDataMessage(chunk);

            try {
                outputStream.writeObject(message);
            } catch (IOException e) {
                System.out.printf("<Unexpected exception: %s>\n", e.getMessage());
                break;
            }

            chunkStart = chunkLength;

        }

    }

}