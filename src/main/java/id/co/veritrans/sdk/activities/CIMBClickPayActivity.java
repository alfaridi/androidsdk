package id.co.veritrans.sdk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.gson.Gson;

import id.co.veritrans.sdk.R;
import id.co.veritrans.sdk.callbacks.TransactionCallback;
import id.co.veritrans.sdk.core.Constants;
import id.co.veritrans.sdk.core.Logger;
import id.co.veritrans.sdk.core.SdkUtil;
import id.co.veritrans.sdk.core.VeritransSDK;
import id.co.veritrans.sdk.fragments.InstructionCIMBFragment;
import id.co.veritrans.sdk.fragments.PaymentTransactionStatusFragment;
import id.co.veritrans.sdk.models.DescriptionModel;
import id.co.veritrans.sdk.models.TransactionResponse;

/**
 * Created by Ankit on 11/26/15.
 */
public class CIMBClickPayActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PAYMENT_WEB_INTENT = 151;
    private InstructionCIMBFragment cimbClickPayFragment = null;
    private Button buttonConfirmPayment = null;
    private Toolbar mToolbar = null;
    private VeritransSDK mVeritransSDK = null;
    private TransactionResponse transactionResponse = null;
    private String errorMessage = null;
    private int RESULT_CODE = RESULT_CANCELED;
    private int position = Constants.PAYMENT_METHOD_CIMB_CLICKS;

    private FragmentManager fragmentManager;
    private String currentFragmentName = "";
    private TransactionResponse transactionResponseFromMerchant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getSupportFragmentManager();
        setContentView(R.layout.activity_cimb_clickpay);
        mVeritransSDK = VeritransSDK.getVeritransSDK();

        if (mVeritransSDK == null) {
            SdkUtil.showSnackbar(CIMBClickPayActivity.this, Constants
                    .ERROR_SDK_IS_NOT_INITIALIZED);
            finish();
        }
        initializeViews();
        setUpFragment();
    }

    private void initializeViews() {
        buttonConfirmPayment = (Button) findViewById(R.id.btn_confirm_payment);
        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        buttonConfirmPayment.setOnClickListener(this);
    }

    private void setUpFragment() {

        // setup  fragment
        cimbClickPayFragment = new InstructionCIMBFragment();
        replaceFragment(cimbClickPayFragment, true, false);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_confirm_payment) {
            makeTransaction();
        }
    }

    private void makeTransaction() {
        SdkUtil.showProgressDialog(this, getString(R.string.processing_payment), false);
        DescriptionModel cimbDescription = new DescriptionModel("Any Description");

        mVeritransSDK.paymentUsingCIMBClickPay(CIMBClickPayActivity.this, cimbDescription, new TransactionCallback() {
            @Override
            public void onSuccess(TransactionResponse cimbClickPayTransferResponse) {
                SdkUtil.hideProgressDialog();

                if (cimbClickPayTransferResponse != null &&
                        !TextUtils.isEmpty(cimbClickPayTransferResponse.getRedirectUrl())) {
                    CIMBClickPayActivity.this.transactionResponse = cimbClickPayTransferResponse;
                    Intent intentPaymentWeb = new Intent(CIMBClickPayActivity.this, PaymentWebActivity.class);
                    intentPaymentWeb.putExtra(Constants.WEBURL, cimbClickPayTransferResponse.getRedirectUrl());
                    startActivityForResult(intentPaymentWeb, PAYMENT_WEB_INTENT);
                } else {
                    SdkUtil.showApiFailedMessage(CIMBClickPayActivity.this, getString(R.string
                            .empty_transaction_response));
                }

            }

            @Override
            public void onFailure(String errorMessage, TransactionResponse transactionResponse) {

                try {
                    CIMBClickPayActivity.this.errorMessage = errorMessage;
                    CIMBClickPayActivity.this.transactionResponse = transactionResponse;
                    SdkUtil.hideProgressDialog();
                    SdkUtil.showSnackbar(CIMBClickPayActivity.this, "" + errorMessage);
                } catch (NullPointerException ex) {
                    SdkUtil.showApiFailedMessage(CIMBClickPayActivity.this, getString(R.string.empty_transaction_response));
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.i("reqCode:" + requestCode + ",res:" + resultCode);

        if (resultCode == RESULT_OK && data != null) {
            String responseStr = data.getStringExtra(Constants.PAYMENT_RESPONSE);
            if (TextUtils.isEmpty(responseStr)) {
                return;
            }
            Gson gson = new Gson();
            transactionResponseFromMerchant = gson.fromJson(responseStr, TransactionResponse.class);
            PaymentTransactionStatusFragment paymentTransactionStatusFragment =
                    PaymentTransactionStatusFragment.newInstance(transactionResponseFromMerchant);
            replaceFragment(paymentTransactionStatusFragment, true, false);
            buttonConfirmPayment.setVisibility(View.GONE);
        }
    }

    public void replaceFragment(Fragment fragment, boolean addToBackStack, boolean clearBackStack) {
        if (fragment != null) {
            Logger.i("replace freagment");
            boolean fragmentPopped = false;
            String backStateName = fragment.getClass().getName();

            if (clearBackStack) {
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else {
                fragmentPopped = fragmentManager.popBackStackImmediate(backStateName, 0);
            }

            if (!fragmentPopped) { //fragment not in back stack, create it.
                Logger.i("fragment not in back stack, create it");
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.replace(R.id.cimb_clickpay_container, fragment, backStateName);
                if (addToBackStack) {
                    ft.addToBackStack(backStateName);
                }
                ft.commit();
                currentFragmentName = backStateName;
            }
        }
    }

    public void setResultAndFinish() {

        Intent data = new Intent();
        if (transactionResponseFromMerchant != null) {
            data.putExtra(Constants.TRANSACTION_RESPONSE, transactionResponseFromMerchant);
        }
        data.putExtra(Constants.TRANSACTION_ERROR_MESSAGE, errorMessage);
        setResult(RESULT_CODE, data);
        finish();
    }

    public void setResultCode(int resultCode) {
        this.RESULT_CODE = resultCode;
    }
}

