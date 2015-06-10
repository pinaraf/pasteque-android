package fr.pasteque.client.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import fr.pasteque.client.R;
import fr.pasteque.client.data.CustomerData;
import fr.pasteque.client.models.Customer;
import fr.pasteque.client.widgets.CustomersAdapter;

public class CustomerSelectDialog extends DialogFragment implements AdapterView.OnItemClickListener {

    public static final String TAG = "CustomerSelectDFRAG";

    private static final String NONE_FIELD_ARG = "none_arg";

    // Data
    private Context mContext;
    private Listener mListener;
    private boolean mbNoneField;
    // Views
    private ListView mList;

    public interface Listener {
        void onCustomerPicked(Customer customer);
    }

    public static CustomerSelectDialog newInstance(boolean bNoneField) {
        Bundle args = new Bundle();
        args.putBoolean(NONE_FIELD_ARG, bNoneField);
        CustomerSelectDialog dial = new CustomerSelectDialog();
        dial.setArguments(args);
        return dial;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mbNoneField = getArguments().getBoolean(NONE_FIELD_ARG);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.customer_select, null);

        layout.findViewById(R.id.btn_negative).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        mList = (ListView) layout.findViewById(R.id.customers_list);
        List<Customer> data;
        if (mbNoneField) {
            data = new ArrayList<>();
            data.add(null);
            data.addAll(CustomerData.customers);
        } else {
            data = CustomerData.customers;
        }
        mList.setAdapter(new CustomersAdapter(data, mContext));
        mList.setOnItemClickListener(this);
        return layout;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dial = super.onCreateDialog(savedInstanceState);
        dial.setCanceledOnTouchOutside(true);
        dial.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dial;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Customer c = (Customer) mList.getAdapter().getItem(position);
        if (mListener != null) mListener.onCustomerPicked(c);
        getDialog().dismiss();
    }

    public void setDialogListener(Listener listener) {
        mListener = listener;
    }
}
