import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * An MP3 Client to request .mp3 files from a server and receive them over the socket connection.
 */
public class MP3Client {

    private Socket clientSocket;

    private ObjectOutputStream outputStream;

    public static void main(String[] args)
    {

        MP3Client client;

        try {
            client = new MP3Client("localhost", 3000);
        } catch (IOException e) {

            System.out.println("<An unexpected exception occurred>");
            e.printStackTrace();

            System.out.println("<Disconnecting the client>");
            return;

        }

        client.connect();

    }

    public MP3Client(String host, int port) throws IllegalArgumentException, IOException {

        if (host.length() == 0) {
            throw new IllegalArgumentException("host argument is invalid");
        }

        if (port < 0) {
            throw new IllegalArgumentException("port argument is negative");
        }

        clientSocket = new Socket(host, port);

    }

    public void connect() {

        System.out.println("<Connected to the server>");

        try {
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {

            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message: %s\n", e.getMessage());

            System.out.println("<Disconnecting the client>");

            try {
                clientSocket.close();
            } catch (IOException i) {
                i.printStackTrace();
            }

            return;

        }

        ResponseListener responseListener;
        Scanner inputStream = new Scanner(System.in);

        System.out.println("What would you like to do?");

        while (clientSocket.isBound()) {

            System.out.println("To see a list of available songs to download enter <list>");
            System.out.println("To download a song enter <download>");
            System.out.println("To exit enter <exit>");

            System.out.print("Enter your choice: ");
            String choice = inputStream.nextLine();

            if (choice.equalsIgnoreCase("list")) {
                try {
                    outputStream.writeObject(new SongRequest(false));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (choice.equalsIgnoreCase("download")) {

                System.out.print("Enter the name of the song: ");
                String songName = inputStream.nextLine();

                System.out.print("Enter the artist of the song: ");
                String artistName = inputStream.nextLine();

                try {
                    outputStream.writeObject(new SongRequest(true, songName, artistName));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (choice.equalsIgnoreCase("exit")) {

                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;

            } else {

                System.out.println("That is an incorrect choice. Please enter one of the following choices:");
                continue;

            }

            responseListener = new ResponseListener(clientSocket);

            Thread t = new Thread(responseListener);
            t.start();

            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }

}


/**
 * This class implements Runnable, and will contain the logic for listening for
 * server responses. The threads you create in MP3Server will be constructed using
 * instances of this class.
 */
final class ResponseListener implements Runnable {

    private Socket clientSocket;

    private ObjectInputStream inputStream;

    public ResponseListener(Socket clientSocket) throws IllegalArgumentException {

        if (clientSocket == null) {
            throw new IllegalArgumentException("client is null");
        }

        this.clientSocket = clientSocket;

    }

    /**
     * Listens for a response from the server.
     * <p>
     * Continuously tries to read a SongHeaderMessage. Gets the artist name, song name, and file size from that header,
     * and if the file size is not -1, that means the file exists. If the file does exist, the method then subsequently
     * waits for a series of SongDataMessages, takes the byte data from those data messages and writes it into a
     * properly named file.
     */
    public void run() {

        try {
            inputStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {

            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message> %s\n", e.getMessage());
            return;

        }

        Object object;

        try {
            object = inputStream.readObject();
        } catch (Exception e) {

            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message> %s\n", e.getMessage());
            return;

        }

        if (object.getClass() != SongHeaderMessage.class) return;
        SongHeaderMessage response = (SongHeaderMessage) object;

        if (response.isSongHeader()) {

            int fileSize = response.getFileSize();

            if (fileSize == -1) {

                System.out.println("Invalid SongRequest");
                return;

            }

            String fileName = String.format("savedSongs/%s - %s.mp3", response.getSongName(), response.getArtistName());

            SongDataMessage data = null;
            int chunkTotal = 0;

            while (true) {

                try {
                    data = (SongDataMessage) inputStream.readObject();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (data == null) break;

                byte[] bytes = data.getData();
                chunkTotal += bytes.length;

                System.out.print(String.format("\r<Downloading File (%d/%d)>", chunkTotal, fileSize));
                writeByteArrayToFile(bytes, fileName);
                if (chunkTotal == fileSize) break;

            }

            System.out.println();
            return;

        }

        String message = null;

        while (true) {

            try {
                message = (String) inputStream.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (message == null) break;
            System.out.println(message);

        }

    }

    /**
     * Writes the given array of bytes to a file whose name is given by the fileName argument.
     *
     * @param songBytes the byte array to be written
     * @param fileName  the name of the file to which the bytes will be written
     */
    private void writeByteArrayToFile(byte[] songBytes, String fileName)
    {
        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            outputStream.write(songBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}