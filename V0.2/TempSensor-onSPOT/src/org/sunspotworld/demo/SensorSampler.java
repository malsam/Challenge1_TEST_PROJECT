/*
 * SensorSampler.java
 *
 * Copyright (c) 2008-2010 Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.sunspotworld.demo;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.resources.transducers.IMeasurementInfo;
import com.sun.spot.resources.transducers.ITemperatureInput;
import com.sun.spot.util.Utils;
import java.io.IOException;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * This application is the 'on SPOT' portion of the SendDataDemo. It
 * periodically samples a sensor value on the SPOT and transmits it to a desktop
 * application (the 'on Desktop' portion of the SendDataDemo) where the values
 * are displayed.
 *
 * @author: Vipul Gupta modified: Ron Goldman
 */
public class SensorSampler extends MIDlet {

    private static final int HOST_PORT = 67;
    private static final int SAMPLE_PERIOD = 1000;  // in milliseconds
    private ITemperatureInput tempSensor = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);
    RadiogramConnection rCon = null;
    Datagram dg = null;
    String ourAddress = System.getProperty("IEEE_ADDRESS");
    ITriColorLED led = (ITriColorLED) Resources.lookup(ITriColorLED.class, "LED7");

    private void run() throws IOException {

        // If you run the project from NetBeans on the host or with "ant run" on a command line,
        // and if the USB is connected, you will see System.out text output.

        System.out.println("Temperature device an instance of " + tempSensor.getClass());
        System.out.println(" located: " + tempSensor.getTagValue("location"));
        System.out.println("Starting sensor sampler application on " + ourAddress + " ...");

        // Listen for downloads/commands over USB connection
        new com.sun.spot.service.BootloaderListenerService().getInstance().start();

        try {
            // Open up a broadcast connection to the host port
            // where the 'on Desktop' portion of this demo is listening
            rCon = (RadiogramConnection) Connector.open("radiogram://broadcast:" + HOST_PORT);
            dg = rCon.newDatagram(50);  // only sending 12 bytes of data
        } catch (Exception e) {
            System.err.println("Caught " + e + " in connection initialization.");
            notifyDestroyed();
        }

        if (tempSensor instanceof IMeasurementInfo) {
            System.out.println(" with range: " + ((IMeasurementInfo) tempSensor).getMinValue() + " C"
                    + " to " + ((IMeasurementInfo) tempSensor).getMaxValue() + " C");
        }

        while (true) {
            try {
                // Get the current time and sensor reading
                long now = System.currentTimeMillis();
                double calibrate = 0.0;
                // Flash an LED to indicate a sampling event
                led.setRGB(255, 255, 255);
                led.setOn();
                Utils.sleep(50);
                led.setOff();
                double tempC = 0;
                double tempF = 0;

                for (int i = 0; i < 10; i++) {
                    tempC = tempSensor.getCelsius();      // Temperature in Celcius.
                    tempF = tempSensor.getFahrenheit();   // Temperature in Farenheight
                    calibrate += tempC;
                    Utils.sleep(SAMPLE_PERIOD/2);
                }
                tempC = calibrate / 10;
                System.out.println("Temperature = " + tempC);
                // Package the time and sensor reading into a radio datagram and send it.
                dg.reset();
                dg.writeLong(now);
                dg.writeDouble(tempC);
                rCon.send(dg);

                System.out.println("temperature: " + tempC + " C = " + tempF + " F");

                Utils.sleep(10 * SAMPLE_PERIOD - (System.currentTimeMillis() - now));
            } catch (Exception e) {
                System.err.println("Caught " + e + " while collecting/sending sensor sample.");
            }// Like Thread.sleep() without the exception.
        }
    }

    protected void startApp() throws MIDletStateChangeException {
        try {
            run();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        // Only called if startApp throws any exception other than MIDletStateChangeException
    }
}
