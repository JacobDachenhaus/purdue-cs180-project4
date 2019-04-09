import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * An MP3 Client to request .mp3 files from a server and receive them over the socket connection.
 */
public class MP3Client {

    public static void main(String[] args)
    {
        Socket serverConnection = null;
        Scanner inUser = new Scanner(System.in);
        ObjectOutputStream outServer;

        String choice = "";
        String song;
        String artist;
        while (choice != "exit")
        {
            try
            {
                serverConnection = new Socket("localhost", 50000);
                outServer = new ObjectOutputStream(serverConnection.getOutputStream());
            } catch (IOException e)
            {
                System.out.println("<An unexpected exception occurred>");
                System.out.printf("<Exception message: %s\n", e.getMessage());

                if (serverConnection != null)
                {
                    try
                    {
                        serverConnection.close();
                    } catch (IOException i)
                    {
                        i.printStackTrace();
                    }
                }

                return;
            }

            System.out.println("<Connected to the server>");
            System.out.print("What would you like to do?");
            System.out.println("See list of available songs to download. Enter <list>");
            System.out.println("Download a Song. Enter <download>");
            System.out.println("Exit. Enter <exit>");
            System.out.println("Enter your choice: ");
            choice = inUser.nextLine();
            if (choice.compareToIgnoreCase("exit") == 0)
            {
                try
                {
                    serverConnection.close();
                } catch (IOException i)
                {
                    i.printStackTrace();
                }
                break;
            } else if (choice.compareToIgnoreCase("list") == 0)
            {
                try
                {
                    outServer.writeObject(new SongRequest(false));
                } catch (IOException i)
                {
                    i.printStackTrace();
                }
            } else if (choice.compareToIgnoreCase("download") == 0)
            {
                try
                {
                    System.out.println("Enter the name of the desired song download: ");
                    song = inUser.nextLine();
                    System.out.println("Enter the name of the artist of the desired song");
                    artist = inUser.nextLine();
                    outServer.writeObject(new SongRequest(true, song, artist));
                } catch (IOException i)
                {
                    i.printStackTrace();
                }
            } else
            {
                System.out.println("That is an incorrect choice. Please enter one of the following choices.");
            }

            try
            {
                outServer.flush();
                new ResponseListener(serverConnection);
            } catch (IOException i)
            {
                i.printStackTrace();
            }

            try
            {
                serverConnection.close();
            } catch (IOException i)
            {
                i.printStackTrace();
            }
        }

        return;
    }
}


/**
 * This class implements Runnable, and will contain the logic for listening for
 * server responses. The threads you create in MP3Server will be constructed using
 * instances of this class.
 */
final class ResponseListener implements Runnable
{
    private ObjectInputStream ois;

    public ResponseListener(Socket clientSocket)
    {
        try
        {
            ois = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException i)
        {
            i.printStackTrace();
        }
    }

    /**
     * Listens for a response from the server.
     * <p>
     * Continuously tries to read a SongHeaderMessage. Gets the artist name, song name, and file size from that header,
     * and if the file size is not -1, that means the file exists. If the file does exist, the method then subsequently
     * waits for a series of SongDataMessages, takes the byte data from those data messages and writes it into a
     * properly named file.
     */
    public void run()
    {
        Object object;
        SongHeaderMessage header;
        int length;
        byte[] bytes;
        SongDataMessage data;
        File file;
        String song;
        int i;
        while (true)
        {
            try
            {
                object = ois.readObject();
                if (object != null && object.getClass() == (new SongHeaderMessage(true)).getClass())
                {
                    header = (SongHeaderMessage) object;
                    if (header.isSongHeader())
                    {
                        length = header.getFileSize();
                        file = new File("/savedSongs/" + header.getArtistName() + " - " + header.getSongName() + ".mp3");
                        file.createNewFile();
                        while (length > 0)
                        {
                            data = (SongDataMessage) ois.readObject();
                            bytes = data.getData();
                            writeByteArrayToFile(bytes, file.getPath());
                            length -= 1000;
                        }
                    } else
                    {
                        song = "";
                        while (song != null)
                        {
                            song = (String) ois.readObject();
                            System.out.println(song);
                        }
                    }
                }
            } catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            } catch (IOException f)
            {
                f.printStackTrace();
            }
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
        try
        {
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(songBytes);
        } catch (FileNotFoundException f)
        {
            f.printStackTrace();
        } catch(IOException i)
        {
            i.printStackTrace();
        }
    }
}