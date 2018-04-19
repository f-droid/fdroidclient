/*
 * Copyright (C) 2010 Ken Ellinwood
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
package kellinwood.zipio;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import kellinwood.logging.LoggerInterface;
import kellinwood.logging.LoggerManager;

/**
 *
 */
public class ZipListingHelper
{

    static DateFormat dateFormat = new SimpleDateFormat("MM-dd-yy HH:mm");
    
    public static void listHeader(LoggerInterface log)
    {
        log.debug(" Length   Method    Size  Ratio   Date   Time   CRC-32    Name");
        log.debug("--------  ------  ------- -----   ----   ----   ------    ----");
        
    }

    public static void listEntry(LoggerInterface log, ZioEntry entry)
    {
        int ratio = 0;
        if (entry.getSize() > 0) ratio = (100 * (entry.getSize() - entry.getCompressedSize())) / entry.getSize();
        log.debug(String.format("%8d  %6s %8d %4d%% %s  %08x  %s",
                                entry.getSize(),
                                entry.getCompression() == 0 ? "Stored" : "Defl:N",
                                entry.getCompressedSize(),
                                ratio,
                                dateFormat.format( new Date( entry.getTime())),
                                entry.getCrc32(),
                                entry.getName()));
    }
}


