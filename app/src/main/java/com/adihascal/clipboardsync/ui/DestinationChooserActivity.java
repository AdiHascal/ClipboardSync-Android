package com.adihascal.clipboardsync.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;

import com.adihascal.clipboardsync.reference.Reference;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.io.IOException;

import static com.adihascal.clipboardsync.network.SocketHolder.out;

public class DestinationChooserActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		startActivityForResult(getRequestIntent(), 1);
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
		if(requestCode == 1 && resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED)
		{
			try
			{
				final String folder = intent.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							Looper.prepare();
							out().writeUTF("accept");
							ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
							ClipData prevClip = manager.getPrimaryClip();
							ClipData replacement = null;
							if(prevClip != null)
							{
								String[] types = new String[prevClip.getDescription().getMimeTypeCount()];
								ClipData.Item[] items = new ClipData.Item[prevClip.getItemCount()];
								for(int i = 0; i < types.length; i++)
								{
									types[i] = prevClip.getDescription().getMimeType(i);
								}
								for(int i = 0; i < prevClip.getItemCount(); i++)
								{
									items[i] = prevClip.getItemAt(i);
								}
								replacement = new ClipData(Reference.ORIGIN, types, items[0]);
								if(items.length > 1)
								{
									for(int i = 1; i < items.length; i++)
									{
										replacement.addItem(items[i]);
									}
								}
							}
							Intent clip = new Intent().putExtra("folder", folder);
							clip.setClipData(replacement);
							manager.setPrimaryClip(ClipData.newIntent(Reference.ORIGIN, clip));
						}
						catch(IOException e)
						{
							e.printStackTrace();
						}
					}
				}).start();
			}
			finally
			{
				finish();
			}
		}
		else
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						out().writeUTF("refuse");
					}
					catch(IOException e)
					{
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
}
