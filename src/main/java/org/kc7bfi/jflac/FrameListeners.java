/*
 * Created on Jun 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

package org.kc7bfi.jflac;

import java.util.HashSet;
import java.util.Set;

import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.metadata.Metadata;


/**
 * Class to handle frame listeners.
 * @author kc7bfi
 */
class FrameListeners implements FrameListener {

    private final Set<FrameListener> frameListeners = new HashSet<>();
    
    /**
     * Add a frame listener.
     * @param listener  The frame listener to add
     */
    public void addFrameListener(FrameListener listener) {
        synchronized (frameListeners) {
            frameListeners.add(listener);
        }
    }
    
    /**
     * Remove a frame listener.
     * @param listener  The frame listener to remove
     */
    public void removeFrameListener(FrameListener listener) {
        synchronized (frameListeners) {
            frameListeners.remove(listener);
        }
    }
    
    /**
     * Process metadata records.
     * @param metadata the metadata block
     * @see org.kc7bfi.jflac.FrameListener#processMetadata(org.kc7bfi.jflac.metadata.Metadata)
     */
    public void processMetadata(Metadata metadata) {
        synchronized (frameListeners) {
            for (FrameListener listener : frameListeners) {
                listener.processMetadata(metadata);
            }
        }
    }
    
    /**
     * Process data frames.
     * @param frame the data frame
     * @see org.kc7bfi.jflac.FrameListener#processFrame(org.kc7bfi.jflac.frame.Frame)
     */
    public void processFrame(Frame frame) {
        synchronized (frameListeners) {
            for (FrameListener listener : frameListeners) {
                listener.processFrame(frame);
            }
        }
    }
   
    /**
     * Called for each frame error detected.
     * @param msg   The error message
     * @see org.kc7bfi.jflac.FrameListener#processError(java.lang.String)
     */
    public void processError(String msg) {
        synchronized (frameListeners) {
            for (FrameListener listener : frameListeners) {
                listener.processError(msg);
            }
        }
    }
}
