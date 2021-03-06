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
package fr.pasteque.client.printing;

import java.io.IOException;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.mpowa.android.sdk.common.base.PowaEnums.ConnectionState;
import com.mpowa.android.sdk.powapos.core.PowaPOSEnums;
import com.mpowa.android.sdk.powapos.core.callbacks.PowaPOSCallback;

import fr.pasteque.client.models.Receipt;
import fr.pasteque.client.utils.PastequePowaPos;

public class PowaPrinter extends PrinterHelper {
    private static final String TAG = "PowaPrinter";
    private String receipt;
    private PowaCallback powaCallback;
    private boolean bManualDisconnect;

    public PowaPrinter(Context ctx, Handler callback) {
        super(ctx, null, callback);
        this.bManualDisconnect = false;
    }

    @Override
	public void connect() throws IOException {
        // Start Powa printer
        this.bManualDisconnect = false;
        this.receipt = "";
        if (PastequePowaPos.getSingleton().getMCU().getConnectionState()
                .equals(ConnectionState.DISCONNECTED)) {
            this.connected = false;
            throw new IOException("Can not print with Powa Printer, MCU Disconnected");
        }
        this.powaCallback = new PowaCallback();
        PastequePowaPos.getSingleton().addCallback(TAG, this.powaCallback);
        this.connected = true;
    }

    @Override
	public void disconnect() throws IOException {
        PastequePowaPos.getSingleton().removeCallback(this.powaCallback);
        this.powaCallback = null;
        this.receipt = "";
        this.connected = false;
        this.bManualDisconnect = true;
    }

    @Override
	public void printReceipt(Receipt r) {
        super.printReceipt(r);
    }

    @Override
	protected void printLine(String data) {
        String ascii = data.replace("é", "e");
        ascii = ascii.replace("è", "e");
        ascii = ascii.replace("ê", "e");
        ascii = ascii.replace("ë", "e");
        ascii = ascii.replace("à", "a");
        ascii = ascii.replace("ï", "i");
        ascii = ascii.replace("ô", "o");
        ascii = ascii.replace("ç", "c");
        ascii = ascii.replace("ù", "u");
        ascii = ascii.replace("É", "E");
        ascii = ascii.replace("È", "E");
        ascii = ascii.replace("Ê", "E");
        ascii = ascii.replace("Ë", "E");
        ascii = ascii.replace("À", "A");
        ascii = ascii.replace("Ï", "I");
        ascii = ascii.replace("Ô", "O");
        ascii = ascii.replace("Ç", "c");
        ascii = ascii.replace("Ù", "u");
        ascii = ascii.replace("€", "E");
        while (ascii.length() > 32) {
            String sub = ascii.substring(0, 32);
            this.receipt += "        " + sub + "        \n";
            ascii = ascii.substring(32);
        }
        this.receipt += "        " + ascii + "        \n";
    }

    @Override
    protected void printLine() {
        this.receipt += "\n";
    }

    @Override
    protected void cut() {
        PastequePowaPos.getSingleton().printText(this.receipt);
        this.receipt = "";
    }

    protected void printDone() {
        // Handled in PowaCallback
    }

    private class PowaCallback extends PowaPOSCallback {
        @Override
		public void onCashDrawerStatus(PowaPOSEnums.CashDrawerStatus status) {}
        @Override
		public void onScannerInitialized(final PowaPOSEnums.InitializedResult result) {}
        @Override
		public void onScannerRead(final String data) {}
        @Override
		public void onUSBDeviceAttached(final PowaPOSEnums.PowaUSBCOMPort port) {}
        @Override
		public void onUSBDeviceDetached(final PowaPOSEnums.PowaUSBCOMPort port) {}
        @Override
		public void onUSBReceivedData(PowaPOSEnums.PowaUSBCOMPort port,
                final byte[] data) {}
        @Override
        public void onPrintJobResult(PowaPOSEnums.PrintJobResult result) { 
            if (result.equals(PowaPOSEnums.PrintJobResult.SUCCESSFUL)) {
                PastequePowaPos.getSingleton().openCashDrawer();
                PowaPrinter.super.printDone();
            }
        }
        @Override
        public void onRotationSensorStatus(PowaPOSEnums.RotationSensorStatus status) {}
        @Override
        public void onMCUSystemConfiguration(Map<String, String> config) {}
        @Override
        public void onMCUBootloaderUpdateFailed(final PowaPOSEnums.BootloaderUpdateError error) {}
        @Override
        public void onMCUBootloaderUpdateStarted() {}
        @Override
        public void onMCUBootloaderUpdateProgress(final int progress) {}
        @Override
        public void onMCUBootloaderUpdateFinished() {}
        @Override
        public void onMCUInitialized(final PowaPOSEnums.InitializedResult result) {
            if (result.equals(PowaPOSEnums.InitializedResult.SUCCESSFUL)) {
                if (queued != null) {
                    printReceipt(queued);
                }
                if (zQueued != null) {
                    printZTicket(zQueued, crQueued);
                }
            }
        }
        @Override
        public void onMCUFirmwareUpdateStarted() {}
        @Override
        public void onMCUFirmwareUpdateProgress(final int progress) {}
        @Override
        public void onMCUFirmwareUpdateFinished() {}
		@Override
		public void onMCUConnectionStateChanged(ConnectionState state) {
            if (!PowaPrinter.this.bManualDisconnect) {
                PowaPrinter.this.connected = state.equals(ConnectionState.CONNECTED);
            }
        }
		@Override
		public void onPrinterOutOfPaper() {}
    }

}
