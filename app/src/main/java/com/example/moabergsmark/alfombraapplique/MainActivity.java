package com.example.moabergsmark.alfombraapplique;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;





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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import static android.widget.Toast.*;


public class MainActivity extends Activity {

    public static final String MIME_TEXT_PLAIN = "text/plain";

    public static final String TAG = "NfcDemo";


    public static final String BUG_TAG = "Bug insect hej";


    private TextView mTextView;

    private TextView bTextView;

    private TextView beTextView;

    private NfcAdapter mNfcAdapter;

    private ImageButton mBugImage;
    //private ImageView mBehaveImage;
    private ImageView mExplosionImage;

    private LinearLayout mBehaveSlots;




    private String bugName ="";

    private String explode;

    private String bugState;


    private String pos;


    // private String behaviour = "";

    private StringBuilder behaviour = new StringBuilder("");


    //TextView tvIsConnected;
    //EditText etResponse;//for http request

    //context test
    private static Context context;



    @Override
    protected void onCreate(Bundle savedInstanceState) {





        super.onCreate(savedInstanceState);

        //context test
        MainActivity.context = getApplicationContext();



        requestWindowFeature(getWindow().FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);



    /* adapt the image to the size of the display */
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Bitmap bmp = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.background4),size.x,size.y,true);

    /* fill the background ImageView with the resized image */
        ImageView iv_background = (ImageView) findViewById(R.id.iv_background);
        iv_background.setImageBitmap(bmp);


        //views
/*
        mTextView = (TextView) findViewById(R.id.hello_id);
        bTextView = (TextView) findViewById(R.id.bug_id);
        beTextView = (TextView) findViewById(R.id.behav_id);*/


        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);



        mBugImage = (ImageButton) findViewById(R.id.bug_image_id);
        //mBehaveImage = (ImageView) findViewById(R.id.imageView2);

        mBehaveSlots = (LinearLayout) findViewById(R.id.behave_slot);





        mExplosionImage = (ImageView) findViewById(R.id.explosion);




        //nfc stuff

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            makeText(this, "This device doesn't support NFC.", LENGTH_LONG).show();
            finish();
            return;

        }
/*
        if (!mNfcAdapter.isEnabled()) {
            mTextView.setText("NFC is disabled.");
        } else {
            mTextView.setText(R.string.hello_world);
        }*/

        handleIntent(getIntent());

    }



    public static Context getAppContext() {
        return MainActivity.context;
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
                if(result.matches("(\\d+):(\\d+)")){
                    Log.d("string is int ", result );
                    bugState="preview";
                    pos = result;
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pre/" + result + "?bug=" + bugName  + "&state=" + bugState );
                    /*
                    new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/" + result + "?bug=" + bugName + "&exp=" + explode +"&behaviour=" + behaviour + "&state=" + bugName );

                    mBugImage.setImageDrawable(null);
                    mBehaveImage.setImageDrawable(null);
                    bugName = "";
                    explode = "";
                    behaviour.setLength(0);*/


                }
                else{
                    switch (result) {
                        case "greenBeetle":
                            mBugImage.setImageResource(R.drawable.beetlegreen);
                            addListenerOnButton();
                            bugName = result;
                            new HttpAsyncTask().execute("http://192.168.1.2:8080/pre/" + pos + "?bug=" + bugName  + "&state=" + bugState );
                            break;
                        case "redBeetle":
                            mBugImage.setImageResource(R.drawable.beetlered);
                            addListenerOnButton();
                            bugName = result;
                            new HttpAsyncTask().execute("http://192.168.1.2:8080/pre/" + pos + "?bug=" + bugName  + "&state=" + bugState );
                            break;
                        case "anotherAnt":
                            mBugImage.setImageResource(R.drawable.anotherant);
                            addListenerOnButton();
                            bugName = result;
                            new HttpAsyncTask().execute("http://192.168.1.2:8080/pre/" + pos + "?bug=" + bugName  + "&state=" + bugState );
                            break;
                        case "ant":
                            mBugImage.setImageResource(R.drawable.ant);
                            addListenerOnButton();
                            bugName = result;
                            new HttpAsyncTask().execute("http://192.168.1.2:8080/pre/" + pos + "?bug=" + bugName  + "&state=" + bugState );
                            break;
                        case "redBerry":
                            mBugImage.setImageResource(R.drawable.red);
                            addListenerOnButton();
                            bugName = result;
                            new HttpAsyncTask().execute("http://192.168.1.2:8080/pre/" + pos + "?bug=" + bugName  + "&state=" + bugState );
                            break;
                        case "grass1":
                            mBugImage.setImageResource(R.drawable.grass1);
                            addListenerOnButton();
                            bugName = result;
                            new HttpAsyncTask().execute("http://192.168.1.2:8080/pre/" + pos + "?bug=" + bugName  + "&state=" + bugState );
                            break;
                        case "grass2":
                            mBugImage.setImageResource(R.drawable.grass2);
                            addListenerOnButton();
                            bugName = result;
                            new HttpAsyncTask().execute("http://192.168.1.2:8080/pre/" + pos + "?bug=" + bugName  + "&state=" + bugState);
                            break;
                        case "grass3":
                            mBugImage.setImageResource(R.drawable.grass3);
                            addListenerOnButton();
                            bugName = result;
                            new HttpAsyncTask().execute("http://192.168.1.2:8080/pre/" + pos + "?bug=" + bugName + "&state=" + bugState);
                            break;
                        case "grass4":
                            mBugImage.setImageResource(R.drawable.grass4);
                            addListenerOnButton();
                            bugName = result;
                            new HttpAsyncTask().execute("http://192.168.1.2:8080/pre/" + pos + "?bug=" + bugName  + "&state=" + bugState);
                            break;
                        case "explode":
                            mExplosionImage.setImageResource(R.drawable.explosion);
                            explode = result;
                            break;
                        case "upDown":
                            ImageView move;
                            move = new ImageView(getAppContext());
                            move.setImageResource(R.drawable.updown2);
                            move.setAdjustViewBounds(true);
                            mBehaveSlots.addView(move);
                            behaviour.append(result).append(",");
                            break;
                        case "rightLeft":
                            ImageView rl = new ImageView(getAppContext());
                            rl.setImageResource(R.drawable.rightleft);
                            rl.setAdjustViewBounds(true);
                            mBehaveSlots.addView(rl);
                            behaviour.append(result).append(",");
                            break;
                        case "circle":
                            ImageView k = new ImageView(getAppContext());
                            k.setImageResource(R.drawable.circle);
                            k.setAdjustViewBounds(true);
                            mBehaveSlots.addView(k);
                            behaviour.append(result).append(",");
                            break;
                        case "up":
                            ImageView up = new ImageView(getAppContext());
                            up.setImageResource(R.drawable.up);
                            up.setAdjustViewBounds(true);
                            mBehaveSlots.addView(up);
                            behaviour.append(result).append(",");
                            break;
                        case "down":
                            ImageView d;
                            d = new ImageView(getAppContext());
                            d.setImageResource(R.drawable.down);
                            d.setAdjustViewBounds(true);
                            mBehaveSlots.addView(d);
                            behaviour.append(result).append(",");
                            break;
                        case "right":
                            ImageView right = new ImageView(getAppContext());
                            right.setImageResource(R.drawable.right);
                            right.setAdjustViewBounds(true);
                            mBehaveSlots.addView(right);
                            behaviour.append(result).append(",");
                            break;
                        case "left":
                            ImageView left = new ImageView(getAppContext());
                            left.setImageResource(R.drawable.left);
                            left.setAdjustViewBounds(true);
                            mBehaveSlots.addView(left);
                            behaviour.append(result).append(",");
                            break;
                        case "lu":
                            ImageView lu = new ImageView(getAppContext());
                            lu.setImageResource(R.drawable.lu);
                            lu.setAdjustViewBounds(true);
                            mBehaveSlots.addView(lu);
                            behaviour.append(result).append(",");
                            break;
                        case "rd":
                            ImageView rd = new ImageView(getAppContext());
                            rd.setImageResource(R.drawable.rd);
                            rd.setAdjustViewBounds(true);
                            mBehaveSlots.addView(rd);
                            behaviour.append(result).append(",");
                            break;
                        case "zigzag":
                            ImageView zz = new ImageView(getAppContext());
                            zz.setImageResource(R.drawable.zigzag);
                            zz.setAdjustViewBounds(true);
                            mBehaveSlots.addView(zz);
                            behaviour.append(result).append(",");
                            break;
                    }
                    //mTextView.setText("Beteendelista: " + behaviour);


                }
            }
        }
    }


    public void addListenerOnButton(){
        mBugImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                bugState="newbug";
                //makeText(MainActivity.this, "du har tryckt på bildknappen", LENGTH_SHORT).show();
                new HttpAsyncTask().execute("http://192.168.1.2:8080/pos/" + pos + "?bug=" + bugName + "&exp=" + explode + "&behaviour=" + behaviour + "&state=" + bugState);

                mBugImage.setImageDrawable(null);
                mBehaveSlots.removeAllViews();
                mExplosionImage.setImageDrawable(null);
                bugName = "";
                explode = "";
                behaviour.setLength(0);
                bugState="preview";
            }
        });
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
    /*public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }*/



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
