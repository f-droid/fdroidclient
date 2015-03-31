/*
 * Copyright (C) 2010 Ken Ellinwood.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kellinwood.security.zipsigner;

import java.util.ArrayList;

public class ProgressHelper {

    private int progressTotalItems = 0;
    private int progressCurrentItem = 0;
    private ProgressEvent progressEvent = new ProgressEvent();
    
    public void initProgress()
    {
        progressTotalItems = 10000;
        progressCurrentItem = 0;        
    }
    
    public int getProgressTotalItems() {
        return progressTotalItems;
    }

    public void setProgressTotalItems(int progressTotalItems) {
        this.progressTotalItems = progressTotalItems;
    }

    public int getProgressCurrentItem() {
        return progressCurrentItem;
    }

    public void setProgressCurrentItem(int progressCurrentItem) {
        this.progressCurrentItem = progressCurrentItem;
    }

    public void progress( int priority, String message) {

        progressCurrentItem += 1;

        int percentDone;
        if (progressTotalItems == 0) percentDone = 0;
        else percentDone = (100 * progressCurrentItem) / progressTotalItems;

        // Notify listeners here
        for (ProgressListener listener : listeners) {
            progressEvent.setMessage(message);
            progressEvent.setPercentDone(percentDone);
            progressEvent.setPriority(priority);
            listener.onProgress( progressEvent);
        }
    }

    private ArrayList<ProgressListener> listeners = new ArrayList<ProgressListener>();

    @SuppressWarnings("unchecked")
    public synchronized void addProgressListener( ProgressListener l)
    {
        ArrayList<ProgressListener> list = (ArrayList<ProgressListener>)listeners.clone();
        list.add(l);
        listeners = list;
    }

    @SuppressWarnings("unchecked")
    public synchronized void removeProgressListener( ProgressListener l)
    {
        ArrayList<ProgressListener> list = (ArrayList<ProgressListener>)listeners.clone();
        list.remove(l);
        listeners = list;
    }      
}
