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

import fr.pasteque.client.models.Customer;

import android.content.Context;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CustomerData {

    private static final String FILENAME = "customers.data";

    public static List<Customer> customers = new ArrayList<Customer>();
    public static List<Customer> createdCustomers = new ArrayList<>();
    // Map containing which local id to replace with server id
    public static HashMap<String, String> resolvedIds = new HashMap<>();

    public static void setCustomers(List<Customer> c) {
        customers = c;
    }

    public static boolean save(Context ctx)
            throws IOException {
        FileOutputStream fos = ctx.openFileOutput(FILENAME, Context.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(customers);
        oos.writeObject(createdCustomers);
        oos.writeObject(resolvedIds);
        oos.close();
        return true;
    }

    public static boolean load(Context ctx)
        throws IOException {
        boolean ok = false;
        FileInputStream fis = ctx.openFileInput(FILENAME);
        ObjectInputStream ois = new ObjectInputStream(fis);
        try {
            customers = (List) ois.readObject();
            createdCustomers = (List<Customer>) ois.readObject();
            resolvedIds = (HashMap<String, String>) ois.readObject();
            ok = true;
        } catch (ClassNotFoundException cnfe) {
            // Should never happen
        }
        ois.close();
        return ok;
    }

    public static void addCreatedCustomer(Customer c) {
        CustomerData.customers.add(c);
        CustomerData.createdCustomers.add(c);
    }
}
