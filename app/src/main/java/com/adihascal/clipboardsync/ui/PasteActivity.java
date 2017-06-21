package com.adihascal.clipboardsync.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PasteActivity extends AppCompatActivity
{
    private DataInputStream data;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.data = new DataInputStream(new ByteArrayInputStream(getIntent().getByteArrayExtra("data")));
        Intent resultIntent = new Intent(this, DirectoryChooserActivity.class);
        DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("New Folder")
                .allowNewDirectoryNameModification(true)
                .allowReadOnlyDirectory(false)
                .build();
        resultIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);
        startActivityForResult(resultIntent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if (requestCode == 1 && resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED)
        {
            try
            {
                String folder = intent.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                int nFiles = data.read();
                byte[] buf;

                for (int i = 0; i < nFiles; i++)
                {
                    File f = new File(folder, data.readUTF());
                    if (f.createNewFile())
                    {
                        FileOutputStream out = new FileOutputStream(f);
                        buf = new byte[(int) data.readLong()];
                        data.read(buf);
                        out.write(buf);
                        out.close();
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
