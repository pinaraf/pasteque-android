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
package fr.pasteque.client.sync;

import android.content.Context;
import android.os.Message;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.pasteque.client.Configure;
import fr.pasteque.client.R;
import fr.pasteque.client.data.ImagesData;
import fr.pasteque.client.data.StockData;
import fr.pasteque.client.models.Cash;
import fr.pasteque.client.models.Catalog;
import fr.pasteque.client.models.Category;
import fr.pasteque.client.models.Composition;
import fr.pasteque.client.models.Customer;
import fr.pasteque.client.models.Floor;
import fr.pasteque.client.models.User;
import fr.pasteque.client.models.Product;
import fr.pasteque.client.models.Stock;
import fr.pasteque.client.models.TariffArea;
import fr.pasteque.client.utils.Base64;
import fr.pasteque.client.utils.HostParser;
import fr.pasteque.client.utils.URLTextGetter;

public class SyncUpdate {

    private static final String LOG_TAG = "Pasteque/SyncUpdate";

    // Note: SyncUpdate uses positive values, SyncSend negative ones
    public static final int SYNC_DONE = 1;
    public static final int CONNECTION_FAILED = 2;
    public static final int CATALOG_SYNC_DONE = 3;
    public static final int USERS_SYNC_DONE = 4;
    public static final int CATEGORIES_SYNC_DONE = 5;
    public static final int CASH_SYNC_DONE = 6;
    public static final int PLACES_SYNC_DONE = 7;
    public static final int SYNC_ERROR = 8;
    public static final int CUSTOMERS_SYNC_DONE = 9;
    public static final int STOCK_SYNC_DONE = 10;
    public static final int COMPOSITIONS_SYNC_DONE = 11;
    public static final int TARIFF_AREAS_SYNC_DONE = 12;
    public static final int STOCK_SYNC_ERROR = 13;
    public static final int CATEGORIES_SYNC_ERROR = 14;
    public static final int CATALOG_SYNC_ERROR = 15;
    public static final int USERS_SYNC_ERROR = 16;
    public static final int CUSTOMERS_SYNC_ERROR = 17;
    public static final int CASH_SYNC_ERROR = 18;
    public static final int PLACES_SYNC_ERROR = 19;
    public static final int COMPOSITIONS_SYNC_ERROR = 20;
    public static final int TARIFF_AREA_SYNC_ERROR = 21;
    public static final int INCOMPATIBLE_VERSION = 22;
    public static final int ROLES_SYNC_DONE = 23;
    public static final int ROLES_SYNC_ERROR = 24;
    public static final int TAXES_SYNC_DONE = 25;
    public static final int TAXES_SYNC_ERROR = 26;
    public static final int LOCATIONS_SYNC_DONE = 27;
    public static final int LOCATIONS_SYNC_ERROR = 28;
    public static final int VERSION_DONE = 29;
    public static final int STOCKS_SKIPPED = 30;
    public static final int PLACES_SKIPPED = 31;

    private Context ctx;
    private Handler listener;
    private boolean versionDone;
    private boolean taxesDone;
    private boolean categoriesDone;
    private boolean productsDone;
    private boolean rolesDone;
    private boolean usersDone;
    private boolean customersDone;
    private boolean cashDone;
    private boolean placesDone;
    private boolean locationsDone;
    private boolean stocksDone;
    private boolean compositionsDone;
    private boolean tariffAreasDone;
    public static final int STEPS = 13;
    /** Stop parallel messages in case of error */
    private boolean stop;

    /** The catalog to build with multiple syncs */
    private Catalog catalog;
    /** Categories by id for quick products assignment */
    private Map<String, Category> categories;
    /** Permissions by role id */
    private Map<String, String> permissions;
    /** Tax rates by tax cat id */
    private Map<String, Double> taxRates;
    /** Tax ids by tax cat id */
    private Map<String, String> taxIds;
    /** Location ids by location name */
    private Map<String, String> locationIds;

    public SyncUpdate(Context ctx, Handler listener) {
        this.listener = listener;
        this.ctx = ctx;
        this.catalog = new Catalog();
        this.categories = new HashMap<String, Category>();
        this.permissions = new HashMap<String, String>();
        this.taxRates = new HashMap<String, Double>();
        this.taxIds = new HashMap<String, String>();
        this.locationIds = new HashMap<String, String>();
    }

    /** Launch synchronization */
    public void startSyncUpdate() {
        synchronize();
    }

    public void synchronize() {
        URLTextGetter.getText(SyncUtils.apiUrl(this.ctx),
                SyncUtils.initParams(this.ctx, "VersionAPI", "get"),
                new DataHandler(DataHandler.TYPE_VERSION));
    }

    private void parseVersion(JSONObject resp) {
        try {
            JSONObject o = resp.getJSONObject("content");
            String version = o.getString("version");
            String level = o.getString("level");
            if (!level.equals(this.ctx.getResources().getString(R.string.level))) {
                SyncUtils.notifyListener(this.listener, INCOMPATIBLE_VERSION);
            } else {
                SyncUtils.notifyListener(this.listener, VERSION_DONE);
                String baseUrl = SyncUtils.apiUrl(this.ctx);
                Map<String, String> cashParams = SyncUtils.initParams(this.ctx,
                        "CashesAPI", "get");
                cashParams.put("host", Configure.getMachineName(this.ctx));
                String stockLocation = Configure.getStockLocation(this.ctx);
                URLTextGetter.getText(baseUrl,
                        SyncUtils.initParams(this.ctx, "TaxesAPI", "getAll"),
                        new DataHandler(DataHandler.TYPE_TAX));
                URLTextGetter.getText(baseUrl,
                        SyncUtils.initParams(this.ctx, "RolesAPI", "getAll"),
                        new DataHandler(DataHandler.TYPE_ROLE));
                URLTextGetter.getText(baseUrl,
                        SyncUtils.initParams(this.ctx,
                                "CustomersAPI", "getAll"),
                        new DataHandler(DataHandler.TYPE_CUSTOMERS));
                URLTextGetter.getText(baseUrl, cashParams,
                        new DataHandler(DataHandler.TYPE_CASH));
                URLTextGetter.getText(baseUrl,
                        SyncUtils.initParams(this.ctx,
                                "TariffAreasAPI", "getAll"),
                        new DataHandler(DataHandler.TYPE_TARIFF));
                if (Configure.getTicketsMode(this.ctx) == Configure.RESTAURANT_MODE) {
                    // Restaurant mode: get places
                    URLTextGetter.getText(baseUrl,
                            SyncUtils.initParams(this.ctx,
                                    "PlacesAPI", "getAll"),
                            new DataHandler(DataHandler.TYPE_PLACES));
                } else {
                    // Other mode: skip places
                    placesDone = true;
                    SyncUtils.notifyListener(this.listener, PLACES_SKIPPED);
                }
                if (!stockLocation.equals("")) {
                    // Stock management: get stocks
                    URLTextGetter.getText(baseUrl,
                            SyncUtils.initParams(this.ctx,
                                    "LocationsAPI", "getAll"),
                            new DataHandler(DataHandler.TYPE_LOCATION));
                } else {
                    locationsDone = true;
                    stocksDone = true;
                    SyncUtils.notifyListener(this.listener, STOCKS_SKIPPED);
                }
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Unable to parse response: " + resp.toString(), e);
            SyncUtils.notifyListener(this.listener, SYNC_ERROR, e);
            return;
        }
    }

    private void parseTaxes(JSONObject resp) {
        try {
            JSONArray array = resp.getJSONArray("content");
            for (int i = 0; i < array.length(); i++) {
                JSONObject taxCat = array.getJSONObject(i);
                String taxCatId = taxCat.getString("id");
                JSONArray taxes = taxCat.getJSONArray("taxes");
                long now = System.currentTimeMillis() / 1000;
                int index = 0;
                long maxDate = 0;
                for (int j = 0; j < taxes.length(); j++) {
                    JSONObject tax = taxes.getJSONObject(j);
                    long date = tax.getLong("startDate");
                    if (date > maxDate && date < now) {
                        index = j;
                        maxDate = date;
                    }
                }
                JSONObject currentTax = taxes.getJSONObject(index);
                double rate = currentTax.getDouble("rate");
                String id = currentTax.getString("id");
                this.taxRates.put(taxCatId, rate);
                this.taxIds.put(taxCatId, id);
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Unable to parse response: " + resp.toString(), e);
            SyncUtils.notifyListener(this.listener, TAXES_SYNC_ERROR, e);
            this.taxesDone = true;
            this.productsDone = true;
            this.compositionsDone = true;
            return;
        }
        SyncUtils.notifyListener(this.listener, TAXES_SYNC_DONE, this.taxRates);
        // Start synchronizing catalog
        URLTextGetter.getText(SyncUtils.apiUrl(this.ctx),
                SyncUtils.initParams(this.ctx, "CategoriesAPI", "getAll"),
                new DataHandler(DataHandler.TYPE_CATEGORY));
    }

    /** Parse categories and start products sync to create catalog */
    private void parseCategories(JSONObject resp) {
        Map<String, List<Category>> children = new HashMap<String, List<Category>>();
        try {
            JSONArray array = resp.getJSONArray("content");
            // First pass: read all and register parents
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Category c = Category.fromJSON(o);
                String parent = null;
                if (!o.isNull("parent_id")) {
                    parent = o.getString("parent_id");
                }
                if (!children.containsKey(parent)) {
                    children.put(parent, new ArrayList<Category>());
                }
                children.get(parent).add(c);
                this.categories.put(c.getId(), c);
            }
            // Second pass: build subcategories
            for (Category root : children.get(null)) {
                // Build subcategories
                this.parseSubcats(root, children);
                // This branch is ready, add to catalog
                this.catalog.addRootCategory(root);
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Unable to parse response: " + resp.toString(), e);
            SyncUtils.notifyListener(this.listener, CATEGORIES_SYNC_ERROR, e);
            this.productsDone = true;
            this.compositionsDone = true;
            return;
        }
        SyncUtils.notifyListener(this.listener, CATEGORIES_SYNC_DONE,
                children.get(null));
        // Start synchronizing products
        URLTextGetter.getText(SyncUtils.apiUrl(this.ctx),
                SyncUtils.initParams(this.ctx, "ProductsAPI", "getAll"),
                new DataHandler(DataHandler.TYPE_PRODUCT));
    }

    // recursive subroutine of parseCategories
    private void parseSubcats(Category c,
                              Map<String, List<Category>> children) {
        if (children.containsKey(c.getId())) {
            for (Category sub : children.get(c.getId())) {
                c.addSubcategory(sub);
                this.parseSubcats(sub, children);
            }
        }
    }

    private void parseProducts(JSONObject resp) {
        try {
            JSONArray array = resp.getJSONArray("content");
            try {
                ImagesData.clearProducts(this.ctx);
            } catch (IOException e) {
                Log.w(LOG_TAG, "Unable to clear product images", e);
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                String taxCatId = o.getString("taxCatId");
                String taxId = this.taxIds.get(taxCatId);
                double taxRate = this.taxRates.get(taxCatId);
                Product p = Product.fromJSON(o, taxId, taxRate);
                if (o.getBoolean("hasImage") == true) {
                    // TODO: call for image
                    /*String image64 = o.getString("image");
                    try {
                        byte[] data = Base64.decode(image64);
                        ImagesData.storeProductImage(this.ctx, p.getId(), data);
                    } catch (IOException e) {
                        Log.w(LOG_TAG, "Unable to read product image for "
                                + p.getId(), e);
                                }*/
                }
                // Find its category and add it
                if (o.getBoolean("visible") == true) {
                    String catId = o.getString("categoryId");
                    for (Category c : this.catalog.getAllCategories()) {
                        if (c.getId().equals(catId)) {
                            this.catalog.addProduct(c, p);
                            break;
                        }
                    }
                } else {
                    this.catalog.addProduct(p);
                }
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Unable to parse response: " + resp.toString(), e);
            SyncUtils.notifyListener(this.listener, CATALOG_SYNC_ERROR, e);
            this.compositionsDone = true;
            return;
        }
        SyncUtils.notifyListener(this.listener, CATALOG_SYNC_DONE,
                this.catalog);
        // Start synchronizing compositions
        URLTextGetter.getText(SyncUtils.apiUrl(this.ctx),
                SyncUtils.initParams(this.ctx, "CompositionsAPI", "getAll"),
                new DataHandler(DataHandler.TYPE_COMPOSITION));
    }

    private void parseRoles(JSONObject resp) {
        try {
            JSONArray array = resp.getJSONArray("content");
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                String id = o.getString("id");
                String permissions = o.getString("permissions");
                this.permissions.put(id, permissions);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unable to parse response: " + resp.toString(), e);
            SyncUtils.notifyListener(this.listener, ROLES_SYNC_ERROR, e);
            return;
        }
        SyncUtils.notifyListener(this.listener, ROLES_SYNC_DONE,
                this.permissions);
        // Start synchronizing users
        URLTextGetter.getText(SyncUtils.apiUrl(this.ctx),
                SyncUtils.initParams(this.ctx, "UsersAPI", "getAll"),
                new DataHandler(DataHandler.TYPE_USER));
    }

    /** Parse users from JSONObject response. Roles must be parsed. */
    private void parseUsers(JSONObject resp) {
        List<User> users = new ArrayList<User>();
        try {
            JSONArray array = resp.getJSONArray("content");
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                String roleId = o.getString("roleId");
                String permissions = this.permissions.get(roleId);
                User u = User.fromJSON(o, permissions);
                users.add(u);
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Unable to parse response: " + resp.toString(), e);
            SyncUtils.notifyListener(this.listener, USERS_SYNC_ERROR, e);
            return;
        }
        SyncUtils.notifyListener(this.listener, USERS_SYNC_DONE, users);
    }

    private void parseCustomers(JSONObject resp) {
        List<Customer> customers = new ArrayList<Customer>();
        try {
            JSONArray array = resp.getJSONArray("content");
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Customer c = Customer.fromJSON(o);
                customers.add(c);
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Unable to parse response: " + resp.toString(), e);
            SyncUtils.notifyListener(this.listener, CUSTOMERS_SYNC_ERROR, e);
            return;
        }
        SyncUtils.notifyListener(this.listener, CUSTOMERS_SYNC_DONE, customers);
    }

    private void parseCash(JSONObject resp) {
        Cash cash = null;
        try {
            if (resp.isNull("content")) {
                cash = new Cash(Configure.getMachineName(this.ctx));
            } else {
                JSONObject o = resp.getJSONObject("content");                
                cash = Cash.fromJSON(o);
            }
        } catch(JSONException e) {
            Log.e(LOG_TAG, "Unable to parse response: " + resp.toString(),
                    e);
            SyncUtils.notifyListener(this.listener, CASH_SYNC_ERROR, e);
            return;
        }
        SyncUtils.notifyListener(this.listener, CASH_SYNC_DONE, cash);
    }

    private void parsePlaces(JSONObject resp) {
        List<Floor> floors = new ArrayList<Floor>();
        try {
            JSONArray a = resp.getJSONArray("content");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Floor f = Floor.fromJSON(o);
                floors.add(f);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SyncUtils.notifyListener(this.listener, PLACES_SYNC_ERROR, e);
            return;
        }
        SyncUtils.notifyListener(this.listener, PLACES_SYNC_DONE, floors);
    }

    private void parseLocations(JSONObject resp) {
        try {
            JSONArray a = resp.getJSONArray("content");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                String id = o.getString("id");
                String label = o.getString("label");
                this.locationIds.put(label.toLowerCase(), id);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SyncUtils.notifyListener(this.listener, LOCATIONS_SYNC_ERROR, e);
            this.stocksDone = true;
            return;
        }
        String stockLocation = Configure.getStockLocation(this.ctx);
        String locationId = this.locationIds.get(stockLocation.toLowerCase());
        if (locationId == null) {
            // Location not found
            SyncUtils.notifyListener(this.listener, LOCATIONS_SYNC_ERROR);
            this.stocksDone = true;
            return;
        } else {
            // Save id
            try {
                StockData.saveLocation(this.ctx, stockLocation, locationId);
            } catch (IOException e) {
                // Should not happen but it will screw up stocks. Make it fail
                SyncUtils.notifyListener(this.listener,
                        LOCATIONS_SYNC_ERROR, e);
                this.stocksDone = true;
                return;
            }
            // Notify success
            SyncUtils.notifyListener(this.listener, LOCATIONS_SYNC_DONE,
                    locationId);
        }
        // Start synchronizing stocks
        Map<String, String> stockParams = SyncUtils.initParams(this.ctx,
                "StocksAPI", "getAll");
        stockParams.put("locationId", locationId);
        URLTextGetter.getText(SyncUtils.apiUrl(this.ctx), stockParams,
                new DataHandler(DataHandler.TYPE_STOCK));
    }

    private void parseStocks(JSONObject resp) {
        Map<String, Stock> stocks = new HashMap<String, Stock>();
        try {
            JSONArray a = resp.getJSONArray("content");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Stock s = Stock.fromJSON(o);
                stocks.put(s.getProductId(), s);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SyncUtils.notifyListener(this.listener, STOCK_SYNC_ERROR, e);
        }
        SyncUtils.notifyListener(this.listener, STOCK_SYNC_DONE, stocks);
    }

    private void parseCompositions(JSONObject resp) {
        Map<String, Composition> compos = new HashMap<String, Composition>();
        try {
            JSONArray a = resp.getJSONArray("content");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Composition c = Composition.fromJSON(o);
                compos.put(c.getProductId(), c);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SyncUtils.notifyListener(this.listener, COMPOSITIONS_SYNC_ERROR, e);
            return;
        }
        SyncUtils.notifyListener(this.listener, COMPOSITIONS_SYNC_DONE, compos);
    }

    private void parseTariffAreas(JSONObject resp) {
        List<TariffArea> areas = new ArrayList<TariffArea>();
        try {
            JSONArray a = resp.getJSONArray("content");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                TariffArea area = TariffArea.fromJSON(o);
                areas.add(area);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SyncUtils.notifyListener(this.listener, TARIFF_AREA_SYNC_ERROR, e);
            return;
        }
        SyncUtils.notifyListener(this.listener, TARIFF_AREAS_SYNC_DONE, areas);
    }

    private void finish() {
        SyncUtils.notifyListener(this.listener, SYNC_DONE);
    }

    private void checkFinished() {
        if (this.categoriesDone && this.productsDone && this.rolesDone
                && this.usersDone && this.cashDone && this.placesDone
                && this.stocksDone && this.compositionsDone && this.taxesDone
                && this.tariffAreasDone && this.versionDone
                && this.customersDone && this.locationsDone) {
            this.finish();
        }
    }

    private class DataHandler extends Handler {
        
        private static final int TYPE_USER = 1;
        private static final int TYPE_PRODUCT = 2;
        private static final int TYPE_CATEGORY = 3;
        private static final int TYPE_CASH = 4;
        private static final int TYPE_PLACES = 5;
        private static final int TYPE_CUSTOMERS = 6;
        private static final int TYPE_STOCK = 7;
        private static final int TYPE_COMPOSITION = 8;
        private static final int TYPE_TARIFF = 9;
        private static final int TYPE_VERSION = 10;
        private static final int TYPE_ROLE = 11;
        private static final int TYPE_TAX = 12;
        private static final int TYPE_LOCATION = 13;

        private int type;
        
        public DataHandler(int type) {
            this.type = type;
        }

        private String getError(String response) {
            try {
                JSONObject o = new JSONObject(response);
                if (o.has("error")) {
                    return o.getString("error");
                }
            } catch (JSONException e) {
            }
            return null;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (this.type) {
            case TYPE_VERSION:
                SyncUpdate.this.versionDone = true;
                break;
            case TYPE_USER:
                SyncUpdate.this.usersDone = true;
                break;
            case TYPE_TAX:
                SyncUpdate.this.taxesDone = true;
                break;
            case TYPE_PRODUCT:
                SyncUpdate.this.productsDone = true;
                break;
            case TYPE_CATEGORY:
                SyncUpdate.this.categoriesDone = true;
                break;
            case TYPE_CASH:
                SyncUpdate.this.cashDone = true;
                break;
            case TYPE_PLACES:
                SyncUpdate.this.placesDone = true;
                break;
            case TYPE_ROLE:
                SyncUpdate.this.rolesDone = true;
                break;
            case TYPE_CUSTOMERS:
                SyncUpdate.this.customersDone = true;
                break;
            case TYPE_LOCATION:
                SyncUpdate.this.locationsDone = true;
                break;
            case TYPE_STOCK:
                SyncUpdate.this.stocksDone = true;
                break;
            case TYPE_COMPOSITION:
                SyncUpdate.this.compositionsDone = true;
                break;
            case TYPE_TARIFF:
                SyncUpdate.this.tariffAreasDone = true;
                break;
            }
            switch (msg.what) {
            case URLTextGetter.SUCCESS:
                // Parse content
                String content = (String) msg.obj;
                try {
                    JSONObject result = new JSONObject(content);
                    String status = result.getString("status");
                    if (!status.equals("ok")) {
                        JSONObject err = result.getJSONObject("content");
                        String error = err.getString("code");
                        if (listener != null && !stop) {
                            Log.e(LOG_TAG, "Unable to parse response "
                                    + content);
                            SyncUtils.notifyListener(listener, SYNC_ERROR,
                                    error);
                        }
                        stop = true;
                        finish();
                    } else if (!stop) {
                        switch (type) {
                        case TYPE_VERSION:
                            parseVersion(result);
                            break;
                        case TYPE_ROLE:
                            parseRoles(result);
                            break;
                        case TYPE_USER:
                            parseUsers(result);
                            break;
                        case TYPE_TAX:
                            parseTaxes(result);
                            break;
                        case TYPE_PRODUCT:
                            parseProducts(result);
                            break;
                        case TYPE_CATEGORY:
                            parseCategories(result);
                            break;
                        case TYPE_CASH:
                            parseCash(result);
                            break;
                        case TYPE_PLACES:
                            parsePlaces(result);
                            break;
                        case TYPE_CUSTOMERS:
                            parseCustomers(result);
                            break;
                        case TYPE_LOCATION:
                            parseLocations(result);
                            break;
                        case TYPE_STOCK:
                            parseStocks(result);
                            break;
                        case TYPE_COMPOSITION:
                            parseCompositions(result);
                            break;
                        case TYPE_TARIFF:
                            parseTariffAreas(result);
                            break;
                        }
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Unable to parse response "
                            + content);
                    if (!stop) {
                        SyncUtils.notifyListener(listener, SYNC_ERROR, e);
                    }
                    stop = true;
                    finish();
                }
                break;
            case URLTextGetter.ERROR:
                ((Exception)msg.obj).printStackTrace();
            case URLTextGetter.STATUS_NOK:
                if (!stop) {
                    SyncUtils.notifyListener(listener, CONNECTION_FAILED,
                            msg.obj);
                }
                stop = true;
                finish();
                return;
            }
            checkFinished();
        }
    }

}