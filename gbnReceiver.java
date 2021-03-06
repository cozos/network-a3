import java.io.IOException;

/**
 * Receiver program that implements the Go-Back-N protocol.
 */
public class gbnReceiver extends Receiver {
  private int expectedNumber = -1;
  
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
      parsedPacket.printLog(false);
      
      // Initialize expected number if we haven't received a packet before. 
      if (this.expectedNumber == -1) {
        this.expectedNumber = 0;
      }
      
      if (this.expectedNumber == parsedPacket.getSequenceNumber()) {
        if (parsedPacket.isData()) {
          this.writeToFile(parsedPacket.getPayload());
        }
        // Packet accepted. Send ACK to acknowledge.
        this.sendAck(this.expectedNumber,
                     receivedPacket.getAddress(),
                     receivedPacket.getPort(),
                     parsedPacket.isEOT());
        this.expectedNumber++;
      } else {
        // Packet rejected. Send ACK specifying the latest ACK.
        this.sendAck(Math.max(this.expectedNumber - 1, 0),
            receivedPacket.getAddress(),
            receivedPacket.getPort(),
            parsedPacket.isEOT());
      }
      
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
