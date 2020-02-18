/*
 * Copyright 2020 Conor Gregg Escalante
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.crossrank.backend.serialization;

import com.crossrank.backend.datatypes.MeetResults;

import java.io.*;

public class MeetResultsSerializer {
    public static void SaveMeetResults(MeetResults meetResults) {
        try {
            FileOutputStream file = new FileOutputStream("src\\main\\resources\\meetResults.txt");
            ObjectOutputStream out = new ObjectOutputStream(file);

            out.writeObject(meetResults);
            out.close();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static MeetResults LoadMeetResults() {
        try {
            FileInputStream file = new FileInputStream("src\\main\\resources\\meetResults.txt");
            ObjectInputStream in = new ObjectInputStream(file);

            MeetResults meetResults = (MeetResults) in.readObject();

            in.close();
            file.close();

            return meetResults;

        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
