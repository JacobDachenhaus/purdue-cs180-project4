import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * A MP3 Server for sending mp3 files over a socket connection.
 */
public class MP3Server {

    private ServerSocket serverSocket;

    public static void main(String[] args) {

        MP3Server server;

        try {
            server = new MP3Server(3000);
        } catch (IOException e) {

            System.out.println("<An unexpected exception occurred>");
            e.printStackTrace();

            System.out.println("<Stopping the server>");
            return;

        }

        server.listen();

    }

    public MP3Server(int port) throws IllegalArgumentException, IOException {

        if (port < 0) {
            throw new IllegalArgumentException("port argument is negative");
        }

        serverSocket = new ServerSocket(port);

    }

    public void listen() {

        Socket client;
        ClientHandler requestHandler;

        System.out.println("<Starting the server>");

        while (!serverSocket.isClosed()) {

            try {
                client = serverSocket.accept();
            } catch (IOException e) {

                System.out.println("<An unexpected exception occurred>");
                System.out.printf("<Exception message> %s", e.getMessage());

                System.out.println("<Stopping the server>");

                try {
                    serverSocket.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }

                return;

            }

            System.out.println("<Connected to a client>");
            requestHandler = new ClientHandler(client);

            new Thread(requestHandler).start();

        }

    }

}


/**
 * Class - ClientHandler
 *
 * This class implements Runnable, and will contain the logic for handling responses and requests to
 * and from a given client. The threads you create in MP3Server will be constructed using instances
 * of this class.
 */
final class ClientHandler implements Runnable {

    private Socket clientSocket;

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public ClientHandler(Socket clientSocket) throws IllegalArgumentException {

        if (clientSocket == null) {
            throw new IllegalArgumentException("client is null");
        }

        this.clientSocket = clientSocket;

    }

    /**
     * This method is the start of execution for the thread. See the handout for more details on what
     * to do here.
     */
    public void run() {

        try {

            inputStream = new ObjectInputStream(clientSocket.getInputStream());
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

        } catch (IOException e) {

            System.out.println("<An unexpected exception occurred>");
            System.out.printf("<Exception message> %s", e.getMessage());

            return;

        }

        // TODO: Receive Input

    }

    /**
     * Searches the record file for the given filename.
     *
     * @param fileName the fileName to search for in the record file
     * @return true if the fileName is present in the record file, false if the fileName is not
     */
    private static boolean fileInRecord(String fileName) {

        BufferedReader reader;

        try {

            reader = new BufferedReader(new FileReader("record.txt"));

            String nextLine;

            while ((nextLine = reader.readLine()) != null) {

                if (nextLine.equals(fileName)) {
                    return true;
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;

    }

    /**
     * Read the bytes of a file with the given name into a byte array.
     *
     * @param fileName the name of the file to read
     * @return the byte array containing all bytes of the file, or null if an error occurred
     */
    private static byte[] readSongData(String fileName) {

        File file = new File(fileName);
        byte[] bytes = new byte[(int) file.length()];

        FileInputStream inputStream;

        try {

            inputStream = new FileInputStream(file);
            inputStream.read(bytes);

            return bytes;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * Split the given byte array into smaller arrays of size 1000, and send the smaller arrays
     * to the clientSocket using SongDataMessages.
     *
     * @param songData the byte array to send to the clientSocket
     */
    private void sendByteArray(byte[] songData) {

        int chunkStart = 0;
        int chunkLength;

        try {

            while (chunkStart < songData.length) {

                if (chunkStart + 1000 > songData.length) {
                    chunkLength = songData.length - chunkStart;
                } else {
                    chunkLength = chunkStart + 1000;
                }

                SongDataMessage message = new SongDataMessage(Arrays.copyOfRange(songData, chunkStart, chunkLength));
                outputStream.writeObject(message);

                chunkStart += 1000;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Read ''record.txt'' line by line again, this time formatting each line in a readable
     * format, and sending it to the clientSocket. Send a ''null'' value to the clientSocket when done, to
     * signal to the clientSocket that you've finished sending the record data.
     */
    private void sendRecordData() {

        BufferedReader reader;

        try {

            reader = new BufferedReader(new FileReader("record.txt"));

            String nextLine;

            while ((nextLine = reader.readLine()) != null) {

                String songName = nextLine.substring(nextLine.indexOf("-") + 1, nextLine.length() - 4);
                String artistName = nextLine.substring(0, nextLine.indexOf("-") - 1);

                nextLine = String.format("\"%s\" by: %s", songName, artistName);
                outputStream.writeObject(nextLine);

            }

            outputStream.writeObject(null);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}