/*
 * Created on Jun 7, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

package org.kc7bfi.jflac.apps;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.kc7bfi.jflac.Constants;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.io.BitOutputStream;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.SeekPoint;
import org.kc7bfi.jflac.metadata.SeekTable;
import org.kc7bfi.jflac.metadata.StreamInfo;


/**
 * Assemble several FLAC files into one album.
 * @author kc7bfi
 */
public class FlacPacker extends JFrame {
    
    private JTextArea textArea = new JTextArea(16, 50);
    private JButton addButton = new JButton("Add Files");
    private JButton makeButton = new JButton("Pack FLAC");
    
    private ArrayList<File> flacFiles = new ArrayList<>();
    private ArrayList<PackerFile> albumFiles = new ArrayList<>();
    private StreamInfo masterStreamInfo = null;
    private byte[] buffer = new byte[64 * 1024];
    
    /**
     * Constructor.
     * @param title     Frame title
     */
    public FlacPacker(String title) {
        super(title);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        this.getContentPane().setLayout(new BorderLayout());
        
        // text area
        textArea.setText("");
        textArea.setAutoscrolls(true);
        this.getContentPane().add(textArea, BorderLayout.CENTER);
        
        // button panel
        Panel buttonPanel = new Panel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(addButton);
        buttonPanel.add(makeButton);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        this.pack();
        
        addButton.addActionListener(event -> addFilesToList());
        
        makeButton.addActionListener(event -> {
            try {
                packFlac();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }
    
    private void appendMsg(String msg) {
        textArea.setText(textArea.getText() + msg + "\n");
        textArea.repaint();
    }
    
    private void addFilesToList() {
        JFileChooser chooser = new JFileChooser();
        ExtensionFileFilter filter = new ExtensionFileFilter();
        filter.addExtension("flac");
        filter.setDescription("FLAC files");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(new File("."));
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) return;
        File[] files = chooser.getSelectedFiles();
        Collections.addAll(flacFiles, files);
    }
    
    private File getOutputFile() {
        JFileChooser chooser = new JFileChooser();
        ExtensionFileFilter filter = new ExtensionFileFilter();
        filter.addExtension("flac");
        filter.setDescription("FLAC files");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(new File("."));
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) return null;
        File file = chooser.getSelectedFile();
        return file;
    }
    
    private SeekTable makeSeekTable() {
        long lastSampleNumber = 0;
        long lastStreamOffset = 0;
        
        // process selected files
        for (File flacFile : flacFiles) {
            File file = flacFile;
            try {
                FileInputStream is = new FileInputStream(file);
                FLACDecoder decoder = new FLACDecoder(is);
                decoder.readMetadata();
                StreamInfo info = decoder.getStreamInfo();
                if (masterStreamInfo == null) {
                    masterStreamInfo = info;
                    masterStreamInfo.setTotalSamples(0);
                }
                if (!info.compatiable(masterStreamInfo)) {
                    appendMsg("Bad StreamInfo " + file + ": " + info);
                    continue;
                }
                masterStreamInfo.addTotalSamples(info.getTotalSamples());

                SeekPoint seekPoint = new SeekPoint(lastSampleNumber, lastStreamOffset, 0);
                //decoder.processMetadata();
                long frameStartOffs = decoder.getTotalBytesRead();
                PackerFile aFile = new PackerFile(file, seekPoint, frameStartOffs);
                albumFiles.add(aFile);
                //System.out.println("Do decode " +i);
                try {
                    decoder.decodeFrames();
                } catch (EOFException e) {
                    //appendMsg("File " + file + ": " + e);
                }
                //System.out.println("Done decode");
                long frameEndOffs = decoder.getTotalBytesRead();
                //appendMsg(frameStartOffs + " " + frameEndOffs + " " + decoder.getSamplesDecoded());
                lastSampleNumber += decoder.getSamplesDecoded();
                lastStreamOffset += frameEndOffs - frameStartOffs;
            } catch (IOException e) {
                appendMsg("File " + file + ": " + e);
            }
        }
        
        // make Seek Table
        SeekPoint[] points = new SeekPoint[albumFiles.size()];
        SeekTable seekTable = new SeekTable(points, true);
        int metadataHeader = (Metadata.STREAM_METADATA_IS_LAST_LEN + Metadata.STREAM_METADATA_TYPE_LEN + Metadata.STREAM_METADATA_LENGTH_LEN) / 8;
        int metadataOffset = Constants.STREAM_SYNC_STRING.length + masterStreamInfo.calcLength() + seekTable.calcLength() + metadataHeader * 2;
        for (int i = 0; i < albumFiles.size(); i++) {
            PackerFile aFile = albumFiles.get(i);
            appendMsg("SeekTable build " + i + " Offset=" + aFile.seekPoint.getStreamOffset() + " header=" + metadataOffset);
            aFile.seekPoint.setStreamOffset(aFile.seekPoint.getStreamOffset() + metadataOffset);
            points[i] = aFile.seekPoint;
        }
        
        return seekTable;
    }
    
    private void packFlac() throws IOException {
        // get output file
        File outFile = getOutputFile();
        if (outFile == null) return;
        BitOutputStream os;
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(outFile);
            os = new BitOutputStream(fos);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return;
        }
        
        // get seek table
        SeekTable seekTable = makeSeekTable();
        if (masterStreamInfo == null) return;
        
        // write FLAC marker
        os.writeByteBlock(Constants.STREAM_SYNC_STRING, Constants.STREAM_SYNC_STRING.length);
        
        // output StreamInfo
        masterStreamInfo.write(os, false);
        
        // output SeekTable
        seekTable.write(os, true);
        
        // generate output file
        for (int i = 0; i < albumFiles.size(); i++) {
            PackerFile aFile = albumFiles.get(i);
            appendMsg("Process file " + i + ": " + aFile.file);
            try (RandomAccessFile raf = new RandomAccessFile(aFile.file, "r")) {
                raf.seek(aFile.firstFrameOffset);
                for (int bytes = raf.read(buffer); bytes > 0; bytes = raf.read(buffer)) {
                    fos.write(buffer, 0, bytes);
                }
                fos.flush();
            } catch (EOFException e) {
                appendMsg("File " + aFile.file + ": Done!");
            } catch (IOException e) {
                appendMsg("File " + aFile.file + ": " + e);
            }
        }
    }
    
    /**
     * Main routine.
     * @param args  Command line arguments
     */
    public static void main(String[] args) {
        FlacPacker app = new FlacPacker("FLAC Album Maker");
        app.setVisible(true);
    }
    
    /**
     * This class holds the files and their seek points.
     * @author kc7bfi
     */
    private static class PackerFile {
        protected File file;
        protected SeekPoint seekPoint;
        protected long firstFrameOffset;
        
        /**
         * The constructor.
         * @param file      The file
         * @param seekPoint The SeekPoint
         */
        public PackerFile(File file, SeekPoint seekPoint, long firstFrameOffset) {
            this.file = file;
            this.seekPoint = seekPoint;
            this.firstFrameOffset = firstFrameOffset;
        }
    }
}
