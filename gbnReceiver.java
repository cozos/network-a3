import java.io.IOException;

/**
 * Receiver program that implements the Go-Back-N protocol.
 */
public class gbnReceiver extends Receiver {

  public gbnReceiver(String outFileName) {
    super(outFileName);
  }
  
  @Override
  public void start() {
    while (this.foundEOTPacket == false) {
      try {
        socket.receive(receivedPacket);
      } catch (IOException e) {}

      CS456Packet parsedPacket = new CS456Packet(receivedPacket.getData());
      System.out.println(parsedPacket.toString());
      
      if (parsedPacket.isEOT()) {
        this.end();
      }
    }
  }
  
  /**
   * arg1: filename - the filename where the transferred file is written.
   */
  public static void main(String args[]) {
    if (args.length != 1) {
      System.out.println("Expected 1 argument: outfile");
      System.exit(0);
    }

    gbnReceiver receiver = new gbnReceiver(args[0]);
    receiver.start();
  }
}
