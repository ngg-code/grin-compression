package edu.grinnell.csc207.compression;



import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.io.IOException;


/**
 * The driver for the Grin compression program.
 */

public class Grin {
    private static final int MAGIC_NUMBER = 0xFACEB00C;

    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * 
     * @param infile the file to
     * @param outfile the file to ouptut to
     * @throws IOException
     */
public static void decode(String infile, String outfile) throws IOException {
    
    BitInputStream in = new BitInputStream(infile);
    BitOutputStream out = new BitOutputStream(outfile);

    int magicNumber = in.readBits(32);
    if (magicNumber != MAGIC_NUMBER) {
        in.close();
        out.close();
        throw new IllegalArgumentException("Invalid .grin file: incorrect magic number");
    }

    Map<Short, Integer> frequencies = new HashMap<>();
    while (in.hasBits()) {
        int value = in.readBits(8);
        if (value == -1) {
            break;
        }
        frequencies.put((short) value, frequencies.getOrDefault((short) value, 0) + 1);
    }

    // Deserialize the Huffman tree directly from the input stream
    HuffmanTree tree = new HuffmanTree(frequencies);
    tree.decode(in, out);
    in.close();
    out.close();
}

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * 
     * @param file the file to read
     * @return a freqency map for the given file
     * @throws IOException
     */
    public static Map<Short, Integer> createFrequencyMap(String file) throws IOException {
        Map<Short, Integer> freqMap = new TreeMap<>();
        BitInputStream in = new BitInputStream(file);
        int value;
        while ((value = in.readBits(8)) != -1) {
            short key = (short) value;
            freqMap.put(key, freqMap.getOrDefault(key, 0) + 1);
        }
        in.close();
        return freqMap;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * 
     * @param infile  the file to encode.
     * @param outfile the file to write the output to.
     */

    public static void encode(String infile, String outfile) throws IOException {
        Map<Short, Integer> freqMap = createFrequencyMap(infile);
        HuffmanTree tree = new HuffmanTree(freqMap);
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);
        out.writeBits(MAGIC_NUMBER, 32);
        tree.serialize(out);
        tree.encode(in, out);
        in.close();
        out.close();
    }

    /**
     * The entry point to the program.
     * 
     * @param args the command-line arguments.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
            return;
        }
        String operation = args[0];
        String infile = args[1];
        String outfile = args[2];

        if (operation.equals("encode")) {
            encode(infile, outfile);
            System.out.println("Successfully encoded " + infile + " to " + outfile);
        } else if (operation.equals("decode")) {
            decode(infile, outfile);
            System.out.println("Successfully decoded " + infile + " to " + outfile);
        } else {
            System.out.println("Invalid operation: " + operation);
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
        }
    }
}
