package com.adihascal.clipboardsync.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.adihascal.clipboardsync.R;
import com.adihascal.clipboardsync.reference.Reference;
import com.adihascal.clipboardsync.service.NetworkThreadCreator;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

public class MainActivity extends AppCompatActivity
{
    static Reference reference = new Reference();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
        {
            reference.deviceName = savedInstanceState.getString("device_name", "N/A");
            reference.currentDeviceAddress = savedInstanceState.getString("device_address", "");
            setDeviceIPTextView(reference.deviceName);
        }
        setContentView(R.layout.mainactivity);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putString("device_name", reference.deviceName);
        outState.putString("device_address", reference.currentDeviceAddress);
        super.onSaveInstanceState(outState);
    }

    public void disconnect(View view)
    {
        setDeviceIPTextView(reference.defaultDeviceName);
        Intent intent = new Intent(this, NetworkThreadCreator.class);
        stopService(intent);
    }

    public void tryScanCode(View view)
    {
        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 0);
        }
        else
        {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null)
            {
                startActivityForResult(takePictureIntent, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null)
            {
                startActivityForResult(takePictureIntent, 1);
            }
        }
        else
        {
            Toast.makeText(this, "u r a scrub", Toast.LENGTH_SHORT).show();
        }
    }

    private void tryConnect(String address)
    {
        Intent intent = new Intent(null, null, this, NetworkThreadCreator.class);
        setDeviceIPTextView(reference.deviceName);
        intent.putExtra("device_address", address);
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 1 && resultCode == RESULT_OK)
        {
            try
            {
                Frame frame = new Frame.Builder().setBitmap((Bitmap) data.getExtras().get("data")).build();
                BarcodeDetector detector = new BarcodeDetector.Builder(this).build();
                SparseArray<Barcode> codes = detector.detect(frame);
                Barcode code = codes.get(codes.keyAt(0));
                String[] info = code.rawValue.split(",");
                reference.deviceName = info[0];
                reference.currentDeviceAddress = info[1];
                tryConnect(reference.currentDeviceAddress);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Toast.makeText(this, "u r a scrub", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setDeviceIPTextView(String toSet)
    {
        ((TextView) findViewById(R.id.connected_device_address)).setText(toSet);
    }
}
