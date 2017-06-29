package com.adihascal.clipboardsync.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.adihascal.clipboardsync.util.FilePaster;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.io.FileNotFoundException;

public class PasteActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Intent resultIntent = new Intent(this, DirectoryChooserActivity.class);
        DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("New Folder")
                .allowNewDirectoryNameModification(true)
                .allowReadOnlyDirectory(false)
                .build();
        resultIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        else
        {
            startActivityForResult(resultIntent, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            startActivityForResult(getRequestIntent(), 1);
        }
    }

    private Intent getRequestIntent()
    {
        Intent resultIntent = new Intent(this, DirectoryChooserActivity.class);
        DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("New Folder")
                .allowNewDirectoryNameModification(true)
                .allowReadOnlyDirectory(false)
                .build();
        resultIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);
        return resultIntent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (requestCode == 1 && resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED)
        {
            String folder = intent.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
            try
            {
                new Thread(new FilePaster(folder)).start();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            finally
            {
                finish();
            }
        }
    }
}
