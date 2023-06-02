/*
 * Created on Jun 23, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.kc7bfi.jflac.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;


/**
 * This is a FileInputStream that uses a Random Access File.
 * @author kc7bfi
 */
public class RandomFileInputStream extends InputStream {
    
    protected RandomAccessFile randomFile;

    /**
     * Constructor.
     * @param file  The File to read
     * @throws FileNotFoundException    If file is not found
     */
    public RandomFileInputStream(File file) throws FileNotFoundException {
        super();
        randomFile = new RandomAccessFile(file, "r");
    }

    /**
     * Constructor.
     * @param fileName The name of the file to read
     * @throws FileNotFoundException If the file is not found.
     */
    public RandomFileInputStream(String fileName) throws FileNotFoundException {
        super();
        randomFile = new RandomAccessFile(fileName, "r");
    }
    
    /**
     * Constructor.
     * @param randomFile The file to read
     */
    public RandomFileInputStream(RandomAccessFile randomFile) {
        this.randomFile = randomFile;
    }

    /**
     * Read a byte value.
     * @return the byte value
     * @throws IOException on IO error
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        return randomFile.read();
    }
    
    /**
     * @see java.io.InputStream#reset()
     */
    @Override
    public synchronized void reset() {
        throw new UnsupportedOperationException("reset");
    }
    
    /**
     * Close the file.
     * @throws IOException on IO error
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        randomFile.close();
    }
    
    /** returns length of underline file
     * 
     * @return length of file
     * @throws IOException
     */
    public long getLength() throws IOException {
    	return randomFile.length();
    }
    
    /**
     * Is file marking supported.
     * @return true if file marking is supported
     * @see java.io.InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        return false;
    }
    
    /**
     * @see java.io.InputStream#mark(int)
     */
    @Override
    public synchronized void mark(int pos) {
    	 throw new UnsupportedOperationException("mark");
    }
    
    /**
     * Skip bytes in the input file.
     * @param bytes The number of bytes to skip
     * @return the number of bytes skipped
     * @throws IOException on IO error
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip(long bytes) throws IOException {
        long pos = randomFile.getFilePointer();
        randomFile.seek(pos + bytes);
        return randomFile.getFilePointer() - pos;
    }
    
    /**
     * Read bytes into an array.
     * @param buffer    The buffer to read bytes into
     * @return bytes read
     * @throws IOException on IO error
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        return randomFile.read(buffer);
    }
    
    /**
     * Read bytes into an array.
     * @param buffer    The buffer to read bytes into
     * @param pos   The start position in the buffer
     * @param bytes The number of bytes to read
     * @return bytes read
     * @throws IOException on IO error
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] buffer, int pos, int bytes) throws IOException {
        return randomFile.read(buffer, pos, bytes);
    }
    
    /**
     * Seek to a position in the file.
     * @param pos   The seek position
     * @throws IOException  On error seeking
     */
    public void seek(long pos) throws IOException {
        randomFile.seek(pos);
    }
    
    /** returns current read position in file
     * 
     * @return
     * @throws IOException
     */
    public long getPosition() throws IOException {
    	return randomFile.getFilePointer();
    }
}
