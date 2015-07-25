import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Receiver program that implements the Go-Back-N protocol.
 */
public class srReceiver extends Receiver {
  private int windowBase = 0;
  private List<CS456Packet> buffer = new ArrayList<CS456Packet>(10);
  
  public srReceiver(String outFileName) {
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
      
      // Send ACK for all sequence numbers within range.
      if (parsedPacket.getSequenceNumber() > windowBase - CS456Packet.WINDOW_SIZE
          && parsedPacket.getSequenceNumber() < windowBase + CS456Packet.WINDOW_SIZE) {
        this.sendAck(parsedPacket.getSequenceNumber(),
                     receivedPacket.getAddress(),
                     receivedPacket.getPort(),
                     parsedPacket.isEOT());
      }
      
      if (parsedPacket.getSequenceNumber() > windowBase
          && parsedPacket.getSequenceNumber() < windowBase + CS456Packet.WINDOW_SIZE) {
        
        this.addToBuffer(parsedPacket);
        
        if (parsedPacket.getSequenceNumber() == windowBase) {
          while(!buffer.isEmpty() && buffer.get(0).getSequenceNumber() == windowBase ) {
            this.writeToFile(buffer.get(0).getPayload());
            buffer.remove(0);
            windowBase++;
          }
        }
      }
      
      if (parsedPacket.isEOT()) {
        this.end();
      }
    }
  }
  
  private void addToBuffer(CS456Packet p1) {
    boolean insertedFlag = false;
    for (int i = 0; i < buffer.size(); i++) {
      CS456Packet p2 = buffer.get(i);
      if (p1.getSequenceNumber() == p2.getSequenceNumber()) {
        return;
      } else if (p1.getSequenceNumber() < p2.getSequenceNumber()) {
        buffer.add(i, p1);
        insertedFlag = true;
      }
    }
    
    if (insertedFlag) {
      buffer.add(p1);
    }
  }
  
  private static int getBufferLocation(int base, int sequenceNumber) {
    if (255 - Math.max(CS456Packet.getSequenceNumber(sequenceNumber), CS456Packet.getSequenceNumber(base))
          + Math.min(CS456Packet.getSequenceNumber(sequenceNumber), CS456Packet.getSequenceNumber(base)) < CS456Packet.WINDOW_SIZE) {
      return 255 - Math.max(CS456Packet.getSequenceNumber(sequenceNumber), CS456Packet.getSequenceNumber(base))
          + Math.min(CS456Packet.getSequenceNumber(sequenceNumber), CS456Packet.getSequenceNumber(base)); 
    } else if (Math.abs(base - CS456Packet.getSequenceNumber(sequenceNumber)) < CS456Packet.WINDOW_SIZE) {
      return Math.abs(base - CS456Packet.getSequenceNumber(sequenceNumber));
    } else {
      return -1;
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

      // Ghetto Asserts 
      if (srReceiver.getBufferLocation(10, 19) != 9) {
        System.out.println("9 != " + srReceiver.getBufferLocation(10, 19));
      }
      
      if (srReceiver.getBufferLocation(10, 200) != -1) {
        System.out.println("-1");
      }
      
      if (srReceiver.getBufferLocation(9, 0) != 9) {
        System.out.println("10");
      }
      
      if (srReceiver.getBufferLocation(5, 254) != 6) {
        System.out.println("6 != " + srReceiver.getBufferLocation(5, 254));
      }
      
      if (srReceiver.getBufferLocation(252, 4) != 7) {
        System.out.println("7 != " + srReceiver.getBufferLocation(252, 4));
      }
      
      srReceiver receiver = new srReceiver(args[0]);
      receiver.start();
  }
}
