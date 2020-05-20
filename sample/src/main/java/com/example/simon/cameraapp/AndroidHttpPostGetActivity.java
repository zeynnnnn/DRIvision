package com.example.simon.cameraapp;
/*
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import 	org.json.JSONArray;
import com.example.app.R;

import org.json.JSONObject;
import 	android.media.MediaPlayer;
import java.util.Collections;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;




public class AndroidHttpPostGetActivity extends Activity {
    OkHttpClient client;
    MediaType JSON;
    URL url;
    String host;
    int port;
    String file;
    String protocol;
    String fileToRequest;
    String fileToRequest2;
    String fileName;
    int k;
    final double FILERESOLUTIONPERCENT= 1.5;
    ArrayList<ArrayList<Rectangle>> toRemember;
    MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        host = "192.168.1.22"; ///////////////////ip of digitalocean : 142.93.38.174
        port = 20;
        file = "/predict";
        protocol ="HTTP";
        fileToRequest = "file1";
        fileToRequest2 = "file2";
        fileName ="img.jpg";

        //initialize empty vehicleList and load alarm sound
        toRemember =new ArrayList();
        mp = MediaPlayer.create(this, R.raw.sample);
        k=1;
        try {
            url = new URL(protocol, host,port, file);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        client = new OkHttpClient().newBuilder().connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS).build();

        JSON = MediaType.parse("application/json; charset=utf-8");
    }

    public void makeGetRequest(View v) throws IOException,InterruptedException {
            GetTask task = new GetTask();
            task.execute();
    }


    //I changed the result to JSONObject
    public  class  GetTask extends AsyncTask<Void , Void, JSONObject> {
        private Exception exception;
        protected  JSONObject doInBackground(Void... urls) {
            try {
               // rastgee bi string verdim içine override için bir amaci yok
                return get(url.toString());
            } catch (Exception e) {
                this.exception = e;
                System.out.println(e);
                return null;
            }
        }

        protected void onPostExecute(JSONObject getResponse) {
            if (getResponse!=null) {
                System.out.println(getResponse.toString());
                try {
                    String vehiclestr =getResponse.get("vehicles").toString();
                    vehiclestr= vehiclestr.substring(1,vehiclestr.length()-1);
                    JSONArray jaFiles= new JSONArray(vehiclestr);

                    ArrayList<ArrayList<Rectangle>> fileVehicleList= new ArrayList();

                   for(int t=0;t<jaFiles.length();t++){

                       String oneFileResult =jaFiles.get(t).toString();
                       JSONArray picsVehic =new JSONArray(oneFileResult);
                       ArrayList<Rectangle> recs=new ArrayList();
                       for(int k=0;k<picsVehic.length();k++){
                           String or=picsVehic.get(k).toString();
                           String[] dividedRecInfo = or.substring(1,or.length()-1).split(",");
                          Rectangle r= new Rectangle(dividedRecInfo[0],Math.round(Float.parseFloat(dividedRecInfo[1])),Math.round(Float.parseFloat(dividedRecInfo[2])),Math.round(Float.parseFloat(dividedRecInfo[3])),Math.round(Float.parseFloat(dividedRecInfo[4])),Float.parseFloat(dividedRecInfo[5]));
                           recs.add(r);
                       }
                        Collections.sort(recs);
                       fileVehicleList.add(recs);
                   }

                    for(int y=0;y<fileVehicleList.size();y++)
                    {
                        toRemember.add(fileVehicleList.get(y));
                    }
                    //split("\[")[1]
                    if(evaluateCrashing())
                    {
                       mp.start();
                    }

                } catch (Exception e)
                {
                    Log.e("Error :(","--"+e);
                }
            }
        }
        private boolean evaluateCrashing(){

            int remSize=toRemember.size();
            //ring if it is too big car or medium person
            ArrayList<Rectangle> latest= toRemember.get(remSize-1);

            for(int k=0;k<latest.size();k++){
                Rectangle cur =latest.get(k);
                if((cur.equals("person")&&(cur.getArea()>200*200*FILERESOLUTIONPERCENT))||(cur.getArea()>FILERESOLUTIONPERCENT*250*400 ))
                {
                    Log.d("Here","It found person or realy big car");
                    return true;
                }
            }

            //noticing too much speed and car getting close compared to earlier images
            if(remSize>0)
            {
                for(int u=0;u<remSize-1;u++)
                {
                    ArrayList<Rectangle> earlier=toRemember.get(u);
                    ArrayList<Rectangle> later= toRemember.get(u+1);
                    //FILERESOLUTIONPERCENT
                    for(int iter=0;iter<later.size();iter++)
                    {
                        Rectangle cur =later.get(iter);
                        ArrayList<Integer> matches= possibleMatch(earlier,cur);
                              if(matches.isEmpty()&&((cur.getArea())>400*600*FILERESOLUTIONPERCENT)||(cur.getH()>400*FILERESOLUTIONPERCENT)||(cur.getW()>400*FILERESOLUTIONPERCENT)) {
                                  Log.d("Tagat","noticing too much speed and car getting close compared to earlier images ");
                                  return true;
                              }
                    }


                }
                if(remSize>3)
                    toRemember.remove(0);
            }
            return false;
        }
        private ArrayList<Integer> possibleMatch(ArrayList<Rectangle> list,Rectangle given){

            ArrayList<Integer> matches=new ArrayList();

            for(int c=0;c<list.size();c++)
            {
                Rectangle cur= list.get(c);
                if((Math.abs((given.getArea()/cur.getArea())-1)>0.3)&&((Math.abs(given.getX()/cur.getX()-1)<0.2))&&((Math.abs(given.getY()/cur.getY()-1)<0.2))) ///image yüzde 30dan fazla değişmediğyse
                    matches.add(c);
            }
            return matches;

        }
        public JSONObject get(String url) throws IOException {
            try {
                // take the image from the asset folder
                //String img = "img.jpg";
                System.out.println("k:"+k);
                String img =k+".jpg";
                k++;
               Response response= makeRequest(img);
                JSONObject json = new JSONObject(response.body().string());
                return json;
                //  return new JSONObject(response.body().string());

            } catch (UnknownHostException | UnsupportedEncodingException e) {
                System.out.println("Error: " + e.getLocalizedMessage());
            } catch (Exception e) {
                System.out.println("Other Error: " + e.getLocalizedMessage());
            }

           return null;
        }
        private Response makeRequest(String img){
            AssetManager assetManager = getAssets();
            // make request to send picture && aws account belongs to Musab
            MediaType mediaType = MediaType.parse("multipart/form-data; boundary=--------------------------205063402178265581033669");
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart(fileToRequest, fileName,
                            RequestBody.create(MediaType.parse("image/*jpg"), readImage(img, assetManager)))
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .method("POST", body)
                    .addHeader("Content-Type", "multipart/form-data; boundary=--------------------------205063402178265581033669")
                    .build();
            try {
                Response response = client.newCall(request).execute();
                return response;
            }catch (IOException e){}
            return null;
        }
        private byte[] readImage(String img, AssetManager assetManager){
            InputStream inputStream = null;
            try {
                inputStream = assetManager.open(img);

            } catch (IOException e) {
                Log.e("Test", "Cannot load image from assets");
            }
            Bitmap input = BitmapFactory.decodeStream(inputStream);


            // Compress the image because process jpeg is shorter than other types
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            input.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            return byteArray;
        }
    }
}

 */