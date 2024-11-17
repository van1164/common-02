package com.van1164.photoviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.van1164.photoviewer.detail.DetailActivity;
import com.van1164.photoviewer.dto.PhotoResponseDto;
import com.van1164.photoviewer.imageview.ImageAdapter;
import com.van1164.photoviewer.upload.UploadActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    ImageView imgView;
    TextView textView;
    String site_url = "https://van133.pythonanywhere.com";
    JSONObject post_json;
    String imageUrl = null;
    Bitmap bmImg = null;
    CloadImage taskDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        //imgView = (ImageView) findViewById(R.id.imgView);
        textView = (TextView) findViewById(R.id.textView);

    }

    public void onClickDownload(View v) {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
    }

    public void onClickUpload(View v) {
        Intent intent = new Intent(this, UploadActivity.class);
        startActivity(intent);
        Toast.makeText(getApplicationContext(), "Upload", Toast.LENGTH_LONG).show();
    }

    private class CloadImage extends AsyncTask<String, Integer, List<PhotoResponseDto>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);  // Show progress bar
        }

        @Override
        protected List<PhotoResponseDto> doInBackground(String... urls) {
            List<PhotoResponseDto> photoResponseDtoList = new ArrayList<>();
            try {
                String apiUrl = urls[0];
                String token = "b37773d5ad2d82b029b1a2b21d4b92d70ce1eb6b";
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int responseCode = conn.getResponseCode();
                System.out.println("SSSSSSSSSSSSSSS"+responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);
// 배열 내 모든 이미지 다운로드
                    for (int i = 0; i < aryJson.length(); i++) {
                        post_json = (JSONObject) aryJson.get(i);
                        Log.d("WWWWWWWWWWWWWWWWWWWWW",post_json.toString());
                        imageUrl = post_json.getString("image");
                        String title = post_json.getString("title");
                        String text = post_json.getString("text");
                        String author = post_json.getString("author");
                        String createdDate = post_json.getString("created_date");
                        if (!imageUrl.equals("")) {
                            URL myImageUrl = new URL(imageUrl);
                            conn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = conn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                            photoResponseDtoList.add(new PhotoResponseDto(imageBitmap,title,text,author,createdDate)); // 이미지 리스트에 추가
                            imgStream.close();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return photoResponseDtoList;
        }

        @Override
        protected void onPostExecute(List<PhotoResponseDto> photoResponseDtoList) {
            ProgressBar progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);
            if (photoResponseDtoList.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText("이미지 로드 성공!");
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                ImageAdapter adapter = new ImageAdapter(photoResponseDtoList, photo -> {
                    try {
                        File file = new File(recyclerView.getContext().getCacheDir(), "image_" + photo.getTitle() + ".png");
                        FileOutputStream fos = new FileOutputStream(file);
                        photo.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();

                        // Create an intent to navigate to DetailActivity
                        Intent intent = new Intent(recyclerView.getContext(), DetailActivity.class);

                        // Pass the URI of the saved image file instead of the Bitmap
                        intent.putExtra("imageUri", Uri.fromFile(file).toString());
                        intent.putExtra("title", photo.getTitle());
                        intent.putExtra("text", photo.getText());
                        intent.putExtra("author", photo.getAuthor());
                        intent.putExtra("createdDate", photo.getCreatedDate());
                        // Start DetailActivity
                        recyclerView.getContext().startActivity(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(recyclerView.getContext(), "Clicked on: " + photo.getTitle(), Toast.LENGTH_SHORT).show();
                });
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }
}
