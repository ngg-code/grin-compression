package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits. However, we also need to encode a special EOF character to
 * denote the end of a .grin file. Thus, we need 9 bits to store each
 * byte value. This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {

    public static final short EOF = 255;
    public Map<Short, Integer> freqs;
    public Node root;
    public int frequency;
    public Map<Short, String> codes;
    public BitInputStream in;
    public BitOutputStream out;

    public static class Node {
        private Short value;
        private int frequency;
        public Node left;
        public Node right;

        public Node(Short value, int frequency) {
            this.value = value;
            this.frequency = frequency;
            this.left = null;
            this.right = null;
        }

        public Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            this.frequency = left.frequency + right.frequency;
        }

        public Short getValue() {
            return value;
        }

        public int getFrequency() {
            return frequency;
        }

        public boolean isLeaf() {
            return left == null && right == null;
        }

        public int compareTo(Node other) {
            return this.frequency - other.frequency;
        }
    }

    /**
     * Constructs a new HuffmanTree from a frequency map.
     * 
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree(Map<Short, Integer> freqs) {
        Map<Short, Integer> frequencies = new HashMap<>(freqs);
        frequencies.put(EOF, 1);
        PriorityQueue<Node> priorityQue = new PriorityQueue<>();
        for (Map.Entry<Short, Integer> entry : frequencies.entrySet()) {
            priorityQue.add(new Node(entry.getKey(), entry.getValue()));
        }
        while (priorityQue.size() > 1) {
            Node left = priorityQue.poll();
            Node right = priorityQue.poll();
            priorityQue.add(new Node(left, right));
        }
        root = priorityQue.poll();
        codes = new HashMap<>();
        buildCodesMap(root, "");
    }

    private void buildCodesMap(Node node, String code) {
        if (node.isLeaf()) {
            codes.put(node.value, code);
        } else {
            buildCodesMap(node.left, code + "0");
            buildCodesMap(node.right, code + "1");
        }
    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * 
     * @param in the input file (as a BitInputStream)
     * @throws IOException 
     */
    public HuffmanTree(BitInputStream in) throws IOException {
        Map<Short, Integer> frequencies = new HashMap<>();
        while (in.hasBits()) {
            int value = in.readBits(8);
            if (value == -1) {
                break;
            }
            frequencies.put((short) value, frequencies.getOrDefault((short) value, 0) + 1);
        }
        frequencies.put(EOF, 1);
        PriorityQueue<Node> priorityQue = new PriorityQueue<>();
        for (Map.Entry<Short, Integer> entry : frequencies.entrySet()) {
            priorityQue.add(new Node(entry.getKey(), entry.getValue()));
        }
        while (priorityQue.size() > 1) {
            Node left = priorityQue.poll();
            Node right = priorityQue.poll();
            priorityQue.add(new Node(left, right));
        }
        root = priorityQue.poll();
        codes = new HashMap<>();
        buildCodesMap(root, "");
        this.in = in;
        this.out = new BitOutputStream(in.getFile(), false);

    }

    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * 
     * @param out the output file as a BitOutputStream
     */
    public void serialize(BitOutputStream out) {
        serializeNode(root, out);
    }

    private void serializeNode(Node node, BitOutputStream out) {
        if (node.isLeaf()) {
            out.writeBits(1, 1);
            out.writeBits(node.value, 9);
        } else {
            out.writeBits(0, 1);
            serializeNode(node.left, out);
            serializeNode(node.right, out);
        }
    }


    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * 
     * @param in  the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode(BitInputStream in, BitOutputStream out) {
        int value;
        while ((value = in.readBits(8)) != -1) {
            String code = codes.get((short) value);
            for (int i = 0; i < code.length(); i++) {
                out.writeBit(code.charAt(i) - '0');
            }
        }
        String eofCode = codes.get(EOF);
        for (int i = 0; i < eofCode.length(); i++) {
            out.writeBit(eofCode.charAt(i) - '0');
        }
    }

    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * 
     * @param in  the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode(BitInputStream in, BitOutputStream out) {
        Node currentNode = root;
        while (in.hasBits()) {
            int bit = in.readBit();
            if (bit == -1) {
                break;
            }
            if (bit == 0) {
                currentNode = currentNode.left;
            } else {
                currentNode = currentNode.right;
            }
            if (currentNode.isLeaf()) {
                if (currentNode.value == EOF) {
                    break;
                }
                out.writeBits(currentNode.value, 8);
                currentNode = root;
            }
        }
        in.close();
        out.close();
    }
}
