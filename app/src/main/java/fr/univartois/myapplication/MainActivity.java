package fr.univartois.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.util.Xml;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton load_btn = findViewById(R.id.load_btn);
        load_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Loading news", Snackbar.LENGTH_LONG).show();

                String url = "https://www.lemonde.fr/rss/une.xml";
                Downloader downloader = new Downloader(url);
                downloader.start();

                Snackbar.make(view, "Loading news .. done", Snackbar.LENGTH_LONG).show();

            }
        });

        ListView listView = findViewById(R.id.load_list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                RssItem item = (RssItem) adapterView.getItemAtPosition(position);
                Uri uri = Uri.parse(item.link);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class Downloader extends Thread {
        private String url;

        Downloader(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            try {
                final List<RssItem> news = new ArrayList<>();
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INTERNET)
                        == PackageManager.PERMISSION_GRANTED) {


                    InputStream stream = new URL(this.url).openConnection().getInputStream();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(stream, null);
                    int eventType = parser.getEventType();
                    boolean done = false;
                    RssItem item = null;
                    while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                        String name = null;
                        switch (eventType) {
                            case XmlPullParser.START_DOCUMENT:
                                break;
                            case XmlPullParser.START_TAG:
                                name = parser.getName();
                                if (name.equalsIgnoreCase("item")) {
                                    item = new RssItem();
                                } else if (item != null) {
                                    if (name.equalsIgnoreCase("link")) {
                                        item.link = parser.nextText();
                                    } else if (name.equalsIgnoreCase("description")) {
                                        item.description = parser.nextText().trim();
                                    } else if (name.equalsIgnoreCase("pubDate")) {
                                        item.pubDate = parser.nextText();
                                    } else if (name.equalsIgnoreCase("title")) {
                                        item.title = parser.nextText().trim();
                                    }
                                }
                                break;
                            case XmlPullParser.END_TAG:
                                name = parser.getName();
                                if (name.equalsIgnoreCase("item") && item != null) {
                                    news.add(item);
                                } else if (name.equalsIgnoreCase("channel")) {
                                    done = true;
                                }
                                break;
                        }
                        eventType = parser.next();
                    }
                }


                final ListView listView = findViewById(R.id.load_list);
                listView.post(new Runnable() {
                    @Override
                    public void run() {
                        RssItemAdapter adapter = new RssItemAdapter(
                                getApplicationContext(),
                                news);
                        listView.setAdapter(adapter);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
}
