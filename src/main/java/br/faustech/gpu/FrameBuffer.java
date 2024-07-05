package br.faustech.gpu;

import br.faustech.comum.Component;
import br.faustech.memory.MemoryException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import lombok.Getter;

/**
 * A class representing a framebuffer that manages two buffers for double buffering.
 */
public class FrameBuffer extends Component {

  @Getter private static int bufferSize; // Size of each buffer

  private byte[] frontBuffer; // Buffer currently displayed

  private byte[] backBuffer; // Buffer to write new data to

  private byte[] pixelBuffer; // Buffer to store pixel data

  /**
   * Constructs a FrameBuffer with specified memory addresses and buffer size.
   *
   * @param addresses  The memory addresses.
   * @param bufferSize The size of each buffer.
   */
  public FrameBuffer(final int[] addresses, final int bufferSize) {

    super(addresses);
    this.frontBuffer = new byte[bufferSize]; // Initialize front buffer
    this.backBuffer = new byte[bufferSize]; // Initialize back buffer
    FrameBuffer.bufferSize = bufferSize;
  }

  /**
   * Swaps the front and back buffers, promoting the back to front for display.
   */
  public void swap() {

    byte[] temp = frontBuffer;
    frontBuffer = backBuffer;
    backBuffer = temp;
  }

  /**
   * Writes data to the back buffer starting from a specified position.
   *
   * @param beginDataPosition The starting position in the back buffer.
   * @param data              The byte data to be written.
   * @throws MemoryException If the write operation exceeds buffer limits.
   */
  public void writeToBackBufferFromBytes(final int beginDataPosition, final byte[] data)
      throws MemoryException {

    if (beginDataPosition < 0 || beginDataPosition + data.length > backBuffer.length) {
      throw new MemoryException("Invalid data positions or data length.");
    }

    System.arraycopy(data, 0, backBuffer, beginDataPosition, data.length);
  }

  public void writePixel(final int beginDataPosition, final int data) throws MemoryException {

    this.writeToBackBufferFromInts(beginDataPosition, new int[]{data});

    int x = beginDataPosition - super.getAddresses()[0] % GPU.getWidth();
    int y = beginDataPosition - super.getAddresses()[0] % GPU.getWidth();

    float normX = (x / (float) GPU.getWidth()) * 2 - 1;
    float normY = ((GPU.getHeight() - y) / (float) GPU.getHeight()) * 2 - 1;

    this.writeToBackBufferFromFloats(8 * (y * GPU.getWidth() + x),
        new float[]{normX, normY, ((data >> 16) & 0xFF) / 255.0f,  // r
            ((data >> 8) & 0xFF) / 255.0f,                         // g
            (data & 0xFF) / 255.0f,                                // b
            ((data >> 24) & 0xFF) / 255.0f,                        // a
            x / (float) GPU.getWidth(),                            // u
            y / (float) GPU.getHeight()                            // v
        });
  }

  /**
   * Writes integer data to the back buffer, converting them to bytes before storing.
   *
   * @param beginDataPosition The starting index where data is to be written.
   * @param data              The integer data to be converted and written.
   * @throws MemoryException If the write operation exceeds buffer limits.
   */
  public void writeToBackBufferFromInts(final int beginDataPosition, final int[] data)
      throws MemoryException {

    if (beginDataPosition < 0 || beginDataPosition + data.length > backBuffer.length / 4) {
      throw new MemoryException("Invalid data positions or data length.");
    }

    ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4).order(ByteOrder.nativeOrder());
    IntBuffer intBuffer = byteBuffer.asIntBuffer();
    intBuffer.put(data);

    byteBuffer.rewind();
    byteBuffer.get(backBuffer, beginDataPosition * 4, byteBuffer.remaining());
  }

  /**
   * Writes float data to the back buffer, converting them to bytes before storing.
   *
   * @param beginDataPosition The starting index where data is to be written.
   * @param data              The float data to be converted and written.
   * @throws MemoryException If the write operation exceeds buffer limits.
   */
  public void writeToBackBufferFromFloats(final int beginDataPosition, final float[] data)
      throws MemoryException {

    if (beginDataPosition < 0 || beginDataPosition + data.length > backBuffer.length / 4) {
      throw new MemoryException("Invalid data positions or data length.");
    }

    ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4).order(ByteOrder.nativeOrder());
    FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
    floatBuffer.put(data);

    byteBuffer.rewind();
    byteBuffer.get(backBuffer, beginDataPosition * 4, byteBuffer.remaining());
  }

  /**
   * Reads a segment of the front buffer as byte data.
   *
   * @param beginDataPosition The starting index in the buffer.
   * @param endDataPosition   The ending index in the buffer.
   * @return An array of bytes read from the buffer.
   */
  public byte[] readFromFrontBufferAsBytes(final int beginDataPosition, final int endDataPosition) {

    if (beginDataPosition < 0 || endDataPosition > frontBuffer.length
        || beginDataPosition >= endDataPosition) {
      throw new IllegalArgumentException("Invalid data positions.");
    }

    int length = endDataPosition - beginDataPosition;
    byte[] result = new byte[length];

    System.arraycopy(frontBuffer, beginDataPosition, result, 0, length);

    return result;
  }

  /**
   * Reads a segment of the front buffer as float data.
   *
   * @param beginDataPosition The starting index in the buffer.
   * @param endDataPosition   The ending index in the buffer.
   * @return An array of floats read from the buffer.
   * @throws MemoryException If invalid data positions are used.
   */
  public float[] readFromFrontBufferAsFloats(final int beginDataPosition, final int endDataPosition)
      throws MemoryException {

    int length = endDataPosition - beginDataPosition;

    final ByteBuffer byteBuffer = getByteBufferFromBuffer(frontBuffer, beginDataPosition,
        endDataPosition);

    FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
    float[] floatArray = new float[length];
    floatBuffer.get(floatArray, 0, length);

    return floatArray;
  }

  private ByteBuffer getByteBufferFromBuffer(final byte[] buffer, final int beginDataPosition,
      final int endDataPosition) throws MemoryException {

    if (beginDataPosition < 0 || endDataPosition > buffer.length / 4
        || beginDataPosition >= endDataPosition) {
      throw new MemoryException("Invalid data positions.");
    }

    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
    byteBuffer.order(ByteOrder.nativeOrder());
    byteBuffer.position(beginDataPosition * 4);
    return byteBuffer;
  }

  /**
   * Reads a segment of the front buffer as integer data.
   *
   * @param beginDataPosition The starting index in the buffer.
   * @param endDataPosition   The ending index in the buffer.
   * @return An array of integers read from the buffer.
   * @throws MemoryException If invalid data positions are used.
   */
  public int[] readFromPixelBufferAsInts(final int beginDataPosition, final int endDataPosition)
      throws MemoryException {

    int length = endDataPosition - beginDataPosition;

    final ByteBuffer byteBuffer = getByteBufferFromBuffer(frontBuffer, beginDataPosition,
        endDataPosition);

    IntBuffer intBuffer = byteBuffer.asIntBuffer();
    int[] intArray = new int[length];
    intBuffer.get(intArray, 0, length);

    return intArray;
  }

}
