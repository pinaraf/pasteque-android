/*
    POS-Tech Android
    Copyright (C) 2012 SARL SCOP Scil (contact@scil.coop)

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
package fr.postech.client.models;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

public class Cash implements Serializable {

    private String id;
    private String machineName;
    private long openDate;
    private long closeDate;

    /** Create and open a cash */
    public Cash(String machineName) {
        this.machineName = machineName;
        this.openDate = System.currentTimeMillis() / 1000;
        this.closeDate = -1;
    }

    /** Create an already opened cash */
    public Cash(String id, String machineName, long openDate) {
        this.id = id;
        this.machineName = machineName;
        this.openDate = openDate;
        this.closeDate = -1;
    }
    
    public Cash(String id, String machineName, long openDate, long closeDate) {
        this.id = id;
        this.machineName = machineName;
        this.openDate = openDate;
        this.closeDate = closeDate;
    }
   
    public String getId() {
        return this.id;
    }

    public String getMachineName() {
        return this.machineName;
    }
    
    public long getOpenDate() {
        return this.openDate;
    }

    public boolean isClosed() {
        return this.closeDate != -1;
    }

    public long getCloseDate() {
        return this.closeDate;
    }

    public void closeNow() {
        this.closeDate = System.currentTimeMillis() / 1000;
    }

    public static Cash fromJSON(JSONObject o) throws JSONException {
        String id = o.getString("id");
        String name = o.getString("host");
        long openDate = o.getLong("openDate");
        long closeDate = -1;
        if (o.has("closeDate") && !o.isNull("closeDate")) {
            closeDate = o.getLong("closeDate");
        }
        return new Cash(id, name, openDate, closeDate);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", this.getId());
        o.put("host", this.getMachineName());
        o.put("openDate", this.getOpenDate());
        if (this.isClosed()) {
            o.put("closeDate", this.getCloseDate());
        }
        return o;
    }

}