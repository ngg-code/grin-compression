package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values which would normally
 * take 8 bits. However, we also need to encode a special EOF character to
 * denote the end of a .grin file. We use the next larger primitive integral
 * type, short, to store
 * our byte values.
 */
public class HuffmanTree {

    /**
     * The EOF character is used to denote the end of a .grin file. It is
     * represented by the value 255 (0xFF) in the file.
     */
    public static final short EOF = 255;
    /*
     * The frequency map is a mapping from 9-bit values to their
     * frequencies in the file. The key is a short (which can hold 9 bits)
     * and the value is an integer (which can hold the frequency of that
     * value in the file).
     */
    public Map<Short, Integer> freqs;
    /*
     * The root of the Huffman tree. The tree is built from the frequency
     * map using a priority queue. The tree is a binary tree.
     */
    public Node root;
    /*
     * The frequency of the most common value in the file. This is used to
     * determine the size of the Huffman tree.
     */
    public int frequency;
    /*
     * The codes map is a mapping from 9-bit values to their Huffman codes.
     * The key is a short (which can hold 9 bits) and the value is a string
     * (which holds the Huffman code for that value). The codes map is used
     * to encode and decode the file.
     */
    public Map<Short, String> codes;
    /*
     * The input and output streams are used to read and write the file.
     * The input stream is a BitInputStream which reads the file bit-by-bit
     */
    public BitInputStream in;
    /*
     * The output stream is a BitOutputStream which writes the file
     * bit-by-bit.
     */
    public BitOutputStream out;

    /*
     * The Node class represents a node in the Huffman tree. Each node
     * contains a value which is a short, a frequency which is an
     * integer, and two child nodes which are also HuffmanNodes.
     */
    public static class Node {
        /*
         * The short which can hold 9 bits.
         */
        private Short value;
        /*
         * The frequency the integer which can hold the
         * frequency of that value in the file.
         */
        private int frequency;
        /*
         * The left child of the node.
         */
        public Node left;
        /*
         * The right child of the node.
         */
        public Node right;

        /**
         * Constructs a new Huffman Node with the given value and frequency.
         * 
         * @param value     the value of the node (a short)
         * @param frequency the frequency of the node (an integer)
         */
        public Node(Short value, int frequency) {
            this.value = value;
            this.frequency = frequency;
            this.left = null;
            this.right = null;
        }

        /**
         * Constructs a new Huffman Node with the given left and right child
         * nodes. The frequency of the node is the sum of the frequencies of
         * the left and right child nodes.
         * 
         * @param left  the left child of the node
         * @param right the right child of the node
         */
        public Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            this.frequency = left.frequency + right.frequency;
        }

        /**
         * Returns the value of the node.
         * 
         * @return the value of the node (a short)
         */
        public Short getValue() {
            return value;
        }

        /**
         * Returns the frequency of the node.
         * 
         * @return the frequency of the node (an integer)
         */
        public int getFrequency() {
            return frequency;
        }

        /**
         * Returns true if the node is a leaf node (i.e. it has no children).
         * 
         * @return true if the node is a leaf node, false otherwise
         */
        public boolean isLeaf() {
            return left == null && right == null;
        }

        /**
         * Compares this node to another node based on their frequencies.
         * 
         * @param other the other node to compare to
         * @return a negative integer, zero, or a positive integer as this
         *         node is less than, equal to, or greater than the specified
         *         node
         */
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
        this.freqs = freqs;
        freqs.put(EOF, 1);
        PriorityQueue<Node> priorityQue = new PriorityQueue<>();
        for (Map.Entry<Short, Integer> entry : freqs.entrySet()) {
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

    /**
     * Constructs a mapping from 9-bit values to their Huffman codes.
     * 
     * @param node the root of the Huffman tree
     * @param code the current Huffman code
     */
    private void buildCodesMap(Node node, String code) {
        if (node.isLeaf()) {
            codes.put(node.value, code);
        } else {
            buildCodesMap(node.left, code + "0");
            buildCodesMap(node.right, code + "1");
        }
    }

    public HuffmanTree(Node rootNode) {
        this.root = rootNode;
        this.codes = new HashMap<>();
        buildCodesMap(root, "");
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

    /**
     * Serializes the given Huffman tree node to the given output stream.
     * 
     * @param node the node to serialize
     * @param out  the output file as a BitOutputStream
     */
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

    // Deserialize a tree from a BitInputStream
public static HuffmanTree deserialize(BitInputStream in) throws IOException {
    Node root = deserializeNode(in);
    return new HuffmanTree(root);
}

// Helper method for deserialization
private static Node deserializeNode(BitInputStream in) throws IOException {
    int bit = in.readBit();
    if (bit == 1) {
        // Leaf node
        int value = in.readBits(9);
        return new Node((short)value, 0); // Frequency doesn't matter for decoding
    } else {
        // Internal node
        Node left = deserializeNode(in);
        Node right = deserializeNode(in);
        return new Node(left, right);
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
     * output stream.
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
