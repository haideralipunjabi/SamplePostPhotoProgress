package org.hackesta.samplepostprogress;

import android.app.Activity;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.imagepicker.features.ImagePicker;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    final String TAG = "org.hackesta";
    final String CAPTION = "";
    final int PHOTO_PICKER_REQUESTCODE = 3000;
    AccessTokenTracker accessTokenTracker;
    Context mContext;
    LoginButton loginButton;
    Button btnPost, btnOpen;
    ImageView imageView;
    ProgressBar progressBar;
    CallbackManager callbackManager;
    Bitmap photoBitmap;
    TextView tvProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FacebookSdk.sdkInitialize(this);
        AppEventsLogger.activateApp(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {

            }
        };
        accessTokenTracker.startTracking();
        callbackManager = CallbackManager.Factory.create();
        imageView = (ImageView)findViewById(R.id.ivPhoto);
        tvProgress = (TextView)findViewById(R.id.tvProgress);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        loginButton = (LoginButton)findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList("email","user_photos","public_profile"));
        // Other app specific specialization

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });
        btnPost = (Button)findViewById(R.id.btnPost);
        btnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostButtonClick(v);
            }
        });
        btnOpen = (Button)findViewById(R.id.btnOpenPhoto);
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                photoPicker(PHOTO_PICKER_REQUESTCODE);
            }
        });
    }
    public void PostButtonClick(View v) {

        if (!hasPublishPermission()) {
            getPublishPermission((MainActivity)mContext, new LoginDelegate() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    if (photoBitmap != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        photoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] byteArray = stream.toByteArray();
                        PostToGroup(byteArray, CAPTION, new PostDelegate() {
                            @Override
                            public void onSuccess(String id) {

                            }

                            @Override
                            public void onError() {

                            }

                            @Override
                            public void onProgress(long current, long max) {
                                progress(current,max);
                            }
                        });
                    } else {
                        PostToGroup( null, CAPTION, new PostDelegate() {
                            @Override
                            public void onSuccess(String id) {
                            }

                            @Override
                            public void onError() {
                            }
                            @Override
                            public void onProgress(long current, long max){
                                progress(current, max);
                            }
                        });
                    }
                }

                @Override
                public void onCancel() {
                }

                @Override
                public void onError(FacebookException error) {
                }
            });

        } else if (hasPublishPermission()) {
            if (photoBitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                photoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                //PostPhotoService.bitmapArray = byteArray;
                PostToGroup(byteArray, CAPTION, new PostDelegate() {
                    @Override
                    public void onSuccess(String id) {

                    }

                    @Override
                    public void onError() {

                    }

                    @Override
                    public void onProgress(long current, long max){
                        progress(current, max);
                    }
                });
               // PostPhotoService.caption = CAPTION;
               // Intent intent = new Intent(getBaseContext(), PostPhotoService.class);
               // startService(intent);
            } else {
                PostToGroup(null, CAPTION, new PostDelegate() {
                    @Override
                    public void onSuccess(String id) {
                    }

                    @Override
                    public void onError() {
                    }

                    @Override
                    public void onProgress(long current, long max){
                        progress(current, max);
                    }
                });
            }
        }
    }
    public void progress(long current, long max)
    {
        progressBar.setProgress(Math.round((current * 100) / max));
        tvProgress.setText("Progress: " +  String.valueOf(current) + "/" + String.valueOf(max));
    }
    public boolean hasPublishPermission()
    {
        final boolean[] result = new boolean[1];


        final GraphRequest.Callback gCallBack =  new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                result[0] = PermissionStatus(response, "publish_actions");
            }
        };
        final GraphRequest graphRequest = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/permissions",
                null,
                HttpMethod.GET,
                gCallBack);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                GraphResponse graphResponse = graphRequest.executeAndWait();
            }
        });
        try {
            t.start();
            t.join();

        }
        catch (InterruptedException e)
        {

        }
        return result[0];
    }
    public boolean PermissionStatus(GraphResponse response, String permission_key) {
        try {
            JSONObject json = response.getJSONObject();
            if(json != null)
            {
                JSONArray jArr = json.getJSONArray("data");
                for (int i = 0; i < jArr.length(); i++) {

                    JSONObject obj = jArr.getJSONObject(i);
                    if (obj.getString("permission").matches(permission_key)) {

                        if (obj.getString("status").matches("granted")) return true;
                        else return false;
                    }
                }

            }
            else{
                return false;
            }

        } catch (JSONException e)

        {
        }
        return false;
    }
    public void loadPhoto(String path){
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize=1;
        o2.inJustDecodeBounds =false;
        o2.inPreferredConfig = Bitmap.Config.RGB_565;
        photoBitmap = BitmapFactory.decodeFile(path, o2);
        Picasso.with(mContext)
                .load(new File(path)).into(imageView);
    }
    public void photoPicker(int requestCode)
    {
        ImagePicker.create(this)
                .returnAfterFirst(true) // set whether pick or camera action should return immediate result or not. For pick image only work on single mode
                .folderMode(false) // folder mode (false by default)
                .imageTitle("Tap to select") // image selection title
                .single() // single mode
                .showCamera(false) // show camera or not (true by default)
                .start(requestCode); // start image picker activity with request code
    }

    public static void PostToGroup(final byte[] image, final String caption, final PostDelegate postDelegate)              //Working code, present here since it needs a activity class
    {
        String post_request = "/" + "1631546380475418";
        if(image != null) post_request +=  "/photos";
        else post_request += "/feed";
        Bundle params = new Bundle();
        params.putString("message", caption);
        if(image != null) params.putByteArray("source", image);
        FacebookSdk.setOnProgressThreshold(1l);
        GraphRequest.OnProgressCallback gCallBack = new GraphRequest.OnProgressCallback() {
            @Override
            public void onProgress(long current, long max) {
                Log.d("ONProgress", "onProgress() called with: current = [" + current + "], max = [" + max + "]");
                int progress = Math.round((current * 100) / max);
                Log.d("Progress: " ,String.valueOf(progress));
                postDelegate.onProgress(current, max);
            }

            @Override
            public void onCompleted(GraphResponse response) {
                JSONObject jsonObject = response.getJSONObject();
                if(jsonObject != null && jsonObject.has("id")) {
                    postDelegate.onSuccess(jsonObject.optString("id"));
                }
                else
                {
                    postDelegate.onError();
                }
            }
        };



        final GraphRequest graphRequest = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                post_request,
                params,
                HttpMethod.POST,
                gCallBack);

        graphRequest.executeAsync();
    }
    public void getPublishPermission(Activity activity, final LoginDelegate loginDelegate)
    {
        final Activity _activity = activity;


        LoginManager.getInstance().logInWithPublishPermissions(activity, Arrays.asList("publish_actions"));

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        loginDelegate.onSuccess(loginResult);

                    }

                    @Override
                    public void onCancel() {
                        loginDelegate.onCancel();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        loginDelegate.onError(error);

                    }
                });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode,
                resultCode, data);
        if (requestCode == PHOTO_PICKER_REQUESTCODE && resultCode == RESULT_OK && data != null) {
            List<com.esafirm.imagepicker.model.Image> images = ImagePicker.getImages(data);
            if (images == null) return;

            loadPhoto(images.get(0).getPath().toString());

            return;
        }
    }

public interface PostDelegate{
    void onSuccess(String id);
    void onError();
    void onProgress(long current, long max);
}
    public interface LoginDelegate{
        void onSuccess(LoginResult loginResult);
        void onCancel();
        void onError(FacebookException error);

    }
}