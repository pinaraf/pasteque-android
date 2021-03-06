/*
    Pasteque Android client
    Copyright (C) Pasteque contributors, see the COPYRIGHT file

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package fr.pasteque.client.data;

import fr.pasteque.client.models.PaymentMode;
import android.content.Context;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PaymentModeData {

    private static final String FILENAME = "paymentmodes.data";

    private static List<PaymentMode> modes = new ArrayList<PaymentMode>();

    public static void setPaymentModes(List<PaymentMode> p) {
        modes = p;
    }

    public static List<PaymentMode> paymentModes(Context ctx) {
        return modes;
    }

    public static PaymentMode get(int id, Context ctx) {
        for (PaymentMode mode : modes) {
            if (mode.getId() == id) {
                return mode;
            }
        }
        return null;
    }

    public static boolean save(Context ctx)
        throws IOException {
        FileOutputStream fos = ctx.openFileOutput(FILENAME, Context.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(modes);
        oos.close();
        return true;
    }

    @SuppressWarnings("unchecked")
	public static boolean load(Context ctx)
        throws IOException {
        boolean ok = false;
        FileInputStream fis = ctx.openFileInput(FILENAME);
        ObjectInputStream ois = new ObjectInputStream(fis);
        try {
            modes = (List<PaymentMode>) ois.readObject();
            if (modes.size() > 0) {
                ok = true;
            }
        } catch (ClassNotFoundException cnfe) {
            // Should never happen
        }
        ois.close();
        return ok;
    }
    
}
