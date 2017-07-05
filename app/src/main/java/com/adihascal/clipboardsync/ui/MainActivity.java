package com.adihascal.clipboardsync.ui;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.reference.Reference;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import static com.adihascal.clipboardsync.reference.Reference.savedData;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
        {
            Reference.deviceName = savedInstanceState.getString("device_name", "N/A");
            Reference.currentDeviceAddress = savedInstanceState.getString("device_address", "");
            setDeviceIPTextView(Reference.deviceName);
        }
        savedData = new File(this.getFilesDir().getPath(), "data.txt");
        readFromSave();
        setContentView(R.layout.mainactivity);
        setSupportActionBar((Toolbar) findViewById(R.id.actionbar));
        requestPermissions();
    }

    private void readFromSave()
    {
        try
        {
            if (!savedData.exists())
            {
                savedData.createNewFile();
            }
            if (savedData.length() > 0)
            {
                FileInputStream fileIn = new FileInputStream(savedData);
                DataInputStream in = new DataInputStream(fileIn);
                String[] s = in.readUTF().split(",");
                Reference.currentDeviceAddress = s[0];
                Reference.deviceName = s[1];
                in.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void writeToSave()
    {
        try
        {
            FileWriter fout = new FileWriter(savedData, false);
            String s = Reference.currentDeviceAddress + "," + Reference.deviceName;
            fout.write(s);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause()
    {
        writeToSave();
        super.onPause();
    }

    private void requestPermissions()
    {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_DOCUMENTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_DOCUMENTS,
                    Manifest.permission.CAMERA}, 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString("device_name", Reference.deviceName);
        outState.putString("device_address", Reference.currentDeviceAddress);
        super.onSaveInstanceState(outState);
    }

    public void disconnect(View view)
    {
        setDeviceIPTextView(Reference.defaultDeviceName);
        Intent intent = new Intent(this, NetworkThreadCreator.class);
        stopService(intent);
    }

    public void tryScanCode(View view)
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null)
        {
            startActivityForResult(takePictureIntent, 1);
        }
    }

    public void tryReconnect(View v)
    {
        readFromSave();
        Intent intent = new Intent(NetworkThreadCreator.ACTION_RECONNECT, null, this, NetworkThreadCreator.class);
        setDeviceIPTextView(Reference.deviceName);
        intent.putExtra("device_address", Reference.currentDeviceAddress);
        startService(intent);
    }

    private void tryConnect(String address)
    {
        Intent intent = new Intent(NetworkThreadCreator.ACTION_CONNECT, null, this, NetworkThreadCreator.class);
        setDeviceIPTextView(Reference.deviceName);
        intent.putExtra("device_address", address);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        chooseFolder();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 1 && resultCode == RESULT_OK)
        {
            try
            {
                Bitmap bmap = ((Bitmap) data.getExtras().get("data")).copy(Bitmap.Config.ARGB_8888, true);
                Frame frame = new Frame.Builder().setBitmap(bmap).build();
                BarcodeDetector detector = new BarcodeDetector.Builder(this).build();
                SparseArray<Barcode> codes = detector.detect(frame);
                Barcode code = codes.get(codes.keyAt(0));
                String[] info = code.rawValue.split(",");
                Reference.deviceName = info[0];
                Reference.currentDeviceAddress = info[1];
                tryConnect(Reference.currentDeviceAddress);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Toast.makeText(this, "u r a scrub", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == 2 && resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED)
        {
            String folder = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
            Uri uri = new Uri.Builder().path(folder).scheme("file").build();
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newIntent("", sendIntent));
        }
    }

    private void setDeviceIPTextView(String toSet)
    {
        ((TextView) findViewById(R.id.connected_device_address)).setText(toSet);
    }

    public void chooseFolder()
    {
        Intent resultIntent = new Intent(this, DirectoryChooserActivity.class);
        DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("New Folder")
                .allowNewDirectoryNameModification(true)
                .allowReadOnlyDirectory(false)
                .build();
        resultIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);
        startActivityForResult(resultIntent, 2);
    }
}
