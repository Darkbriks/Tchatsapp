package fr.uga.im2ag.m1info.chatservice.attachments;
 
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
 
/**
 * Handles splitting files into chunks for transfer.
 */
public class FileChunker {
 
    public static final int DEFAULT_CHUNK_SIZE = 64 * 1024; // 64 KB
 
    /**
     * Represents a file chunk.
     */
    public static class Chunk {
        private final int index;
        private final byte[] data;
 
        public Chunk(int index, byte[] data) {
            this.index = index;
            this.data = data;
        }
 
        public int getIndex() {
            return index;
        }
 
        public byte[] getData() {
            return data;
        }
    }
 
    /**
     * Splits a file into chunks.
     * @param file The file to split
     * @param chunkSize Size of each chunk in bytes
     * @return List of chunks
     * @throws IOException if file cannot be read
     */
    public List<Chunk> splitFile(File file, int chunkSize) throws IOException {
        // TODO: Implement file chunking
        throw new UnsupportedOperationException("Not implemented yet");
    }
 
    /**
     * Computes SHA-256 hash of a file for integrity verification.
     * @param file The file to hash
     * @return The hash bytes
     * @throws IOException if file cannot be read
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    public byte[] computeFileHash(File file) throws IOException, NoSuchAlgorithmException {
        // TODO: Implement file hashing
        throw new UnsupportedOperationException("Not implemented yet");
    }
}