package com.example.moabergsmark.alfombraapplique;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;




import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;


import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;



public class MainActivity extends Activity {

    public static final String MIME_TEXT_PLAIN = "text/plain";

    public static final String TAG = "NfcDemo";


    public static final String BUG_TAG = "Bug insect hej";


    private TextView mTextView;

    private TextView bTextView;

    private TextView beTextView;

    private NfcAdapter mNfcAdapter;

    private ImageView mBugImage;
    private ImageView mBehaveImage;




    private String bugName;
    private String behaviour;



    //TextView tvIsConnected;
    //EditText etResponse;//for http request




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(getWindow().FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);



    /* adapt the image to the size of the display */
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Bitmap bmp = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.background),size.x,size.y,true);

    /* fill the background ImageView with the resized image */
        ImageView iv_background = (ImageView) findViewById(R.id.iv_background);
        iv_background.setImageBitmap(bmp);








        mTextView = (TextView) findViewById(R.id.hello_id);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        bTextView = (TextView) findViewById(R.id.bug_id);
        beTextView = (TextView) findViewById(R.id.behav_id);

        mBugImage = (ImageView) findViewById(R.id.bug_image_id);
        mBehaveImage = (ImageView) findViewById(R.id.behave_image);





        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;

        }

        if (!mNfcAdapter.isEnabled()) {
            mTextView.setText("NFC is disabled.");
        } else {
            mTextView.setText(R.string.hello_world);
        }

        handleIntent(getIntent());

    }







    @Override
    protected void onResume() {
        super.onResume();

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }



    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        }
    }


    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }


    private class NdefReaderTask extends AsyncTask<Tag, Void, String>  {
        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }


        //!!!!!!!!!!!!!!!!!!!!!!!!!!
        //this is where the action is. I compare result to colors and then request that adres to the server
        //super cool stuffs
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                mTextView.setText("Read content: " + result);



                //some bugs

                if(result.equals("greenBeetle")){

                    mBugImage.setImageResource(R.drawable.beetlegreen);

                    bugName = result;
                }


                if(result.equals("redBeetle")){
                    mBugImage.setImageResource(R.drawable.beetlered);

                    bugName = result;
                }


                if(result.equals("anotherAnt")){

                    mBugImage.setImageResource(R.drawable.anotherant);


                    bugName = result;
                }


                if(result.equals("ant")){


                    mBugImage.setImageResource(R.drawable.ant);


                    bugName = result;
                }


                //behaviours

                if(result.equals("upDown")){
                    mBehaveImage.setImageResource(R.drawable.updown);


                    behaviour = result;
                }




                //row1

                if(result.equals("11")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/11" + "?bug=" + bugName +"&behaviour=" + behaviour);

                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }

                if(result.equals("12")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/12" + "?bug=" + bugName +"&behaviour=" + behaviour);

                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }

                if(result.equals("13")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/13" + "?bug=" + bugName +"&behaviour=" + behaviour);

                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }

                if(result.equals("14")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/14" + "?bug=" + bugName +"&behaviour=" + behaviour);


                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }

                if(result.equals("15")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/15" + "?bug=" + bugName +"&behaviour=" + behaviour);


                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }


                //row2


                if(result.equals("21")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/21" + "?bug=" + bugName +"&behaviour=" + behaviour);


                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }

                if(result.equals("22")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/22" + "?bug=" + bugName +"&behaviour=" + behaviour);


                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }

                if(result.equals("23")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/23" + "?bug=" + bugName +"&behaviour=" + behaviour);

                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }

                if(result.equals("24")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/24" + "?bug=" + bugName +"&behaviour=" + behaviour);


                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }
                if(result.equals("25")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/25" + "?bug=" + bugName +"&behaviour=" + behaviour);


                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }




                //row3
                if(result.equals("31")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/31" + "?bug=" + bugName +"&behaviour=" + behaviour);

                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }

                if(result.equals("32")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/32" + "?bug=" + bugName +"&behaviour=" + behaviour);


                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }

                if(result.equals("33")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/33" + "?bug=" + bugName +"&behaviour=" + behaviour);

                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";


                }

                if(result.equals("34")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/34" + "?bug=" + bugName +"&behaviour=" + behaviour);


                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }

                if(result.equals("35")){
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/35" + "?bug=" + bugName +"&behaviour=" + behaviour);

                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    behaviour="";
                }



            }
        }
    }


    //+++++ +++++ ++++++++++++++++++++++++++
    public static String GET(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }


    // convert inputstream to String
    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }


    // check network connection
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }



    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();//shows a toast
            //etResponse.setText(result);//prints the recived stuff
        }
    }


    //+++++ +++++ ++++++++++++++++++++++++++



}
